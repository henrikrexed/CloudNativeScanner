/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  experimental: {
    serverComponentsExternalPackages: ['axios']
  },
  env: {
    CUSTOM_KEY: process.env.CUSTOM_KEY,
  },
  // API routes handle proxying at runtime (see pages/api/[...path].ts)
  // Rewrites are evaluated at build time and can't use runtime env vars
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          {
            key: 'Referrer-Policy',
            value: 'origin-when-cross-origin',
          },
        ],
      },
    ]
  },
}

export default nextConfig
