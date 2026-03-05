# Multi-stage build for x86_64 compatible webui
# This Dockerfile is optimized for building linux/amd64 images
FROM --platform=linux/amd64 node:18-alpine AS base

# Install dependencies only when needed
FROM base AS deps
# Install required packages for x86_64 compatibility
RUN apk add --no-cache \
    libc6-compat \
    python3 \
    make \
    g++ \
    && rm -rf /var/cache/apk/*

WORKDIR /app

# Copy package files first for better layer caching
COPY package.json package-lock.json* ./

# Install dependencies with explicit platform compatibility
# Note: This step can take 15-30 minutes on ARM Mac due to emulation
# Try npm ci first (faster, requires lock file), fall back to npm install if lock file is out of sync
RUN echo "===== Installing npm dependencies for x86_64 =====" && \
    (npm ci --prefer-offline --no-audit --loglevel=warn || \
     (echo "===== package-lock.json out of sync, running npm install =====" && \
      npm install --prefer-offline --no-audit --loglevel=warn)) && \
    echo "===== Dependencies installed successfully ====="

# Rebuild the source code only when needed
FROM base AS builder
WORKDIR /app

# Copy node_modules from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy source code
COPY . .

# Ensure required directories exist
RUN mkdir -p ./public ./.next

# Build the application with optimizations for x86_64
ENV NODE_OPTIONS="--max-old-space-size=4096"
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
ENV SKIP_ENV_VALIDATION=1
# Ensure NEXT_PUBLIC_API_URL is not set during build - it should be empty at runtime
# This allows the API client to use relative URLs which are proxied by Next.js API routes
ENV NEXT_PUBLIC_API_URL=""

# Build Next.js application
# This step can take 20-40 minutes on ARM Mac due to QEMU emulation
# The build is NOT stuck - it's just very slow due to architecture emulation
RUN echo "===== Starting Next.js build for x86_64 =====" && \
    echo "===== This may take 20-40 minutes on ARM Mac - please be patient =====" && \
    npm run build && \
    echo "===== Build completed successfully ====="

# Production image, copy all the files and run next
FROM base AS runner
WORKDIR /app

ENV NODE_ENV production

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

# Copy public directory (will be empty if no public assets exist)
COPY --from=builder /app/public ./public

# Set the correct permission for prerender cache
RUN mkdir .next
RUN chown nextjs:nodejs .next

# Automatically leverage output traces to reduce image size
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]