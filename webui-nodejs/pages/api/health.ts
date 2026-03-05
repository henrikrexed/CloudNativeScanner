import type { NextApiRequest, NextApiResponse } from 'next'

type HealthResponse = {
  status: 'ok' | 'error'
  timestamp: string
  uptime?: number
  version?: string
}

export default function handler(
  req: NextApiRequest,
  res: NextApiResponse<HealthResponse>
) {
  if (req.method !== 'GET') {
    return res.status(405).json({ 
      status: 'error', 
      timestamp: new Date().toISOString() 
    })
  }

  res.status(200).json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    version: process.env.npm_package_version || '1.0.0'
  })
}

