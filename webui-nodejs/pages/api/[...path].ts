import type { NextApiRequest, NextApiResponse } from 'next'
import http from 'http'
import https from 'https'
import { URL } from 'url'

const BACKEND_URL = process.env.BACKEND_URL || 'http://localhost:8082'

// Create a logger function that writes to stderr (visible in Kubernetes logs)
const logError = (message: string, error?: any) => {
  const timestamp = new Date().toISOString()
  const errorDetails = error ? ` - ${JSON.stringify(error)}` : ''
  console.error(`[${timestamp}] [API Proxy Error] ${message}${errorDetails}`)
}

const logInfo = (message: string, details?: any) => {
  const timestamp = new Date().toISOString()
  const detailsStr = details ? ` - ${JSON.stringify(details)}` : ''
  console.log(`[${timestamp}] [API Proxy] ${message}${detailsStr}`)
}

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse
) {
  const { path } = req.query
  const pathArray = Array.isArray(path) ? path : [path]
  const apiPath = pathArray.join('/')
  
  // Construct the backend URL
  const backendUrl = new URL(`${BACKEND_URL}/api/${apiPath}`)
  
  // Add query parameters
  if (req.url) {
    const url = new URL(req.url, `http://${req.headers.host}`)
    url.searchParams.forEach((value, key) => {
      backendUrl.searchParams.append(key, value)
    })
  }

  logInfo(`Proxying ${req.method} request`, {
    path: `/api/${apiPath}`,
    backendUrl: backendUrl.toString(),
    query: req.query
  })

  try {
    // Detect streaming and content generation endpoints BEFORE making the request
    // This must be defined before the request callback so it's available in the callback scope
    const isContentGeneration = apiPath.startsWith('content/generate')
    const isStreaming = apiPath === 'content/generate/stream'
    const timeoutMs = isStreaming ? 0 : (isContentGeneration ? 120000 : 30000) // No timeout for streaming, 120s for non-streaming content generation, 30s for others
    
    // Log endpoint detection for debugging
    if (isContentGeneration) {
      logInfo(`Content generation endpoint detected`, {
        apiPath,
        isStreaming,
        timeoutMs: timeoutMs === 0 ? 'disabled (streaming)' : `${timeoutMs}ms`
      })
    }

    // Prepare request options
    const requestOptions: any = {
      method: req.method,
      headers: {
        ...req.headers,
        host: backendUrl.host,
        'Content-Type': req.headers['content-type'] || 'application/json',
      },
    }

    // Remove headers that shouldn't be forwarded
    delete requestOptions.headers['connection']
    delete requestOptions.headers['host']
    delete requestOptions.headers['content-length']

    // For streaming endpoints, set appropriate headers BEFORE making the request
    if (isStreaming) {
      res.setHeader('Content-Type', 'text/event-stream')
      res.setHeader('Cache-Control', 'no-cache')
      res.setHeader('Connection', 'keep-alive')
      res.setHeader('X-Accel-Buffering', 'no') // Disable nginx buffering
    }

    // Choose http or https module
    const httpModule = backendUrl.protocol === 'https:' ? https : http

    // Build request options with hostname and path
    const requestOpts = {
      ...requestOptions,
      hostname: backendUrl.hostname,
      port: backendUrl.port || (backendUrl.protocol === 'https:' ? 443 : 80),
      path: backendUrl.pathname + backendUrl.search,
      protocol: backendUrl.protocol,
    }

    logInfo(`Making backend request`, {
      apiPath: `/api/${apiPath}`,
      hostname: requestOpts.hostname,
      port: requestOpts.port,
      requestPath: requestOpts.path,
      method: requestOpts.method,
      isStreaming
    })

    // Make the request to the backend
    const backendReq = httpModule.request(requestOpts, (backendRes) => {
      logInfo(`Backend response received`, {
        status: backendRes.statusCode,
        path: `/api/${apiPath}`,
        method: req.method,
        isStreaming,
        headers: Object.keys(backendRes.headers)
      })
      
      // Set response status
      res.status(backendRes.statusCode || 500)
      
      // For streaming endpoints, pipe directly without buffering
      // Don't forward headers from backend for streaming - we've already set SSE headers
      if (isStreaming) {
        logInfo(`Streaming response setup`, {
          status: backendRes.statusCode,
          path: `/api/${apiPath}`,
          method: req.method,
          contentType: backendRes.headers['content-type']
        })
        
        // If backend returns error status, collect body and send as error
        if (backendRes.statusCode && backendRes.statusCode >= 400) {
          const errorChunks: Buffer[] = []
          backendRes.on('data', (chunk: Buffer) => {
            errorChunks.push(chunk)
          })
          backendRes.on('end', () => {
            const errorBody = Buffer.concat(errorChunks).toString('utf-8')
            logError(`Backend returned error for streaming request`, {
              status: backendRes.statusCode,
              path: `/api/${apiPath}`,
              body: errorBody.substring(0, 500)
            })
            try {
              const errorData = JSON.parse(errorBody)
              res.write(`event: error\ndata: ${JSON.stringify(errorData)}\n\n`)
            } catch {
              res.write(`event: error\ndata: ${JSON.stringify({error: 'Backend error', message: errorBody.substring(0, 200)})}\n\n`)
            }
            res.end()
          })
          return
        }
        
        let chunkCount = 0
        let totalBytes = 0
        
        // Pipe backend response directly to client for streaming
        backendRes.on('data', (chunk: Buffer) => {
          chunkCount++
          totalBytes += chunk.length
          if (!res.writableEnded) {
            try {
              res.write(chunk)
              if (chunkCount <= 3) {
                // Log first few chunks for debugging
                logInfo(`Streaming chunk ${chunkCount}`, {
                  size: chunk.length,
                  preview: chunk.toString('utf-8').substring(0, 100)
                })
              }
            } catch (writeError: any) {
              logError(`Error writing chunk to client`, {
                error: writeError.message,
                chunkCount
              })
            }
          }
        })
        
        backendRes.on('end', () => {
          logInfo(`Streaming response ended`, {
            chunkCount,
            totalBytes,
            path: `/api/${apiPath}`
          })
          if (!res.writableEnded) {
            res.end()
          }
        })
        
        backendRes.on('error', (error: any) => {
          logError(`Error in streaming response`, {
            error: error.message,
            path: `/api/${apiPath}`,
            chunkCount,
            totalBytes
          })
          if (!res.writableEnded) {
            res.end()
          }
        })
        
        // Handle client disconnect
        req.on('close', () => {
          logInfo(`Client disconnected from stream`, {
            path: `/api/${apiPath}`,
            chunkCount,
            totalBytes
          })
          backendReq.destroy()
        })
        
        return // Don't process as regular response
      }

      // For non-streaming endpoints, forward headers (excluding some that shouldn't be forwarded)
      Object.keys(backendRes.headers).forEach((key) => {
        const value = backendRes.headers[key]
        if (value && !['connection', 'transfer-encoding', 'content-length'].includes(key.toLowerCase())) {
          res.setHeader(key, value)
        }
      })

      // For non-streaming endpoints, collect response body
      const chunks: Buffer[] = []
      backendRes.on('data', (chunk: Buffer) => {
        chunks.push(chunk)
      })

      backendRes.on('end', () => {
        const responseBody = Buffer.concat(chunks).toString('utf-8')
        
        // Log response status and body for errors
        if (backendRes.statusCode && backendRes.statusCode >= 400) {
          logError(`Backend returned error status`, {
            status: backendRes.statusCode,
            path: `/api/${apiPath}`,
            method: req.method,
            backendUrl: backendUrl.toString(),
            responseBody: responseBody.substring(0, 1000), // Limit to first 1000 chars
            headers: backendRes.headers
          })
        } else {
          logInfo(`Backend response`, {
            status: backendRes.statusCode,
            path: `/api/${apiPath}`,
            method: req.method
          })
        }

        // Send the response body
        // Try to parse as JSON for better logging, but send as-is
        try {
          const jsonBody = JSON.parse(responseBody)
          res.json(jsonBody)
        } catch {
          // Not JSON, send as text
          res.send(responseBody)
        }
      })
    })

    // Handle request errors
    backendReq.on('error', (error: any) => {
      logError(`Request to backend failed`, {
        error: error.message,
        stack: error.stack,
        path: `/api/${apiPath}`,
        backendUrl: backendUrl.toString(),
        isStreaming
      })
      
      if (!isStreaming) {
        res.status(502).json({
          error: 'Bad Gateway',
          message: `Failed to connect to backend: ${error.message}`,
          path: `/api/${apiPath}`
        })
      } else {
        res.write(`event: error\ndata: ${JSON.stringify({error: 'Bad Gateway', message: `Failed to connect to backend: ${error.message}`})}\n\n`)
        res.end()
      }
    })

    // Handle timeout for non-streaming endpoints
    // Streaming endpoints have timeout disabled (timeoutMs = 0)
    if (timeoutMs > 0) {
      backendReq.setTimeout(timeoutMs, () => {
        logError(`Request to backend timed out`, {
          path: `/api/${apiPath}`,
          backendUrl: backendUrl.toString(),
          timeout: timeoutMs,
          isStreaming
        })
        backendReq.destroy()
        res.status(504).json({
          error: 'Gateway Timeout',
          message: `Backend request timed out after ${timeoutMs / 1000} seconds`,
          path: `/api/${apiPath}`
        })
      })
    }

    // Stream request body if present
    if (req.method !== 'GET' && req.method !== 'HEAD' && req.method !== 'DELETE') {
      let bodyReceived = false
      let bodySize = 0
      
      req.on('data', (chunk: Buffer) => {
        bodyReceived = true
        bodySize += chunk.length
        backendReq.write(chunk)
      })
      
      req.on('end', () => {
        logInfo(`Request body streamed to backend`, {
          path: `/api/${apiPath}`,
          bodyReceived,
          bodySize,
          isStreaming
        })
        backendReq.end()
      })
      
      req.on('error', (error: any) => {
        logError(`Error reading request body`, {
          error: error.message,
          path: `/api/${apiPath}`,
          bodyReceived,
          bodySize
        })
        backendReq.destroy()
        if (!isStreaming) {
          res.status(400).json({
            error: 'Bad Request',
            message: 'Error reading request body'
          })
        } else {
          res.write(`event: error\ndata: ${JSON.stringify({error: 'Bad Request', message: 'Error reading request body'})}\n\n`)
          res.end()
        }
      })
      
      // If no data events fire, the body might already be consumed
      // Set a small timeout to detect this
      setTimeout(() => {
        if (!bodyReceived && !backendReq.destroyed) {
          logInfo(`No body data received, ending request`, {
            path: `/api/${apiPath}`,
            isStreaming
          })
          backendReq.end()
        }
      }, 100)
    } else {
      backendReq.end()
    }
  } catch (error: any) {
    logError(`Unexpected error in API proxy`, {
      error: error.message,
      stack: error.stack,
      path: `/api/${apiPath}`
    })
    
    res.status(500).json({
      error: 'Internal Server Error',
      message: error.message || 'An unexpected error occurred',
      path: `/api/${apiPath}`
    })
  }
}

// Disable body parsing for Next.js - we'll handle it manually
export const config = {
  api: {
    bodyParser: false,
    // Note: Next.js Pages Router doesn't support maxDuration export
    // Timeout is controlled by the deployment platform (Kubernetes, etc.)
    // For streaming, we disable the backendReq timeout (timeoutMs = 0)
    // and rely on the connection staying open for SSE
  },
}
