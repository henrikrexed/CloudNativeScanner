# WebUI Docker Build Troubleshooting

## Issue: Build Taking Hours / Appearing Stuck

### Root Cause
When building for `linux/amd64` (x86_64) on an ARM Mac (Apple Silicon), Docker/Podman must use emulation (QEMU). This makes builds **10-20x slower** than native builds.

### Expected Build Times
- **Native ARM64 build**: 5-10 minutes
- **Emulated x86_64 build on ARM Mac**: 30-60 minutes (or more)
- **npm install**: 10-20 minutes (emulated)
- **Next.js build**: 20-40 minutes (emulated)

### How to Verify Build is Still Running

Check if the build process is active:
```bash
ps aux | grep podman | grep webui
```

Check Podman machine resources:
```bash
podman machine inspect
```

### Solutions

#### Option 1: Use BuildKit Cache (Faster Rebuilds)
```bash
export DOCKER_BUILDKIT=1
make docker-build-webui PLATFORM=linux/amd64 VERSION=0.11 DOCKER_CACHE=true
```

#### Option 2: Build on Native Architecture First
If you have access to an x86_64 machine or CI/CD pipeline, build there instead.

#### Option 3: Use Docker Buildx with Remote Builder
```bash
docker buildx create --name x86builder --platform linux/amd64 --use
docker buildx build --platform linux/amd64 -t webui:0.11 ./webui-nodejs/
```

#### Option 4: Build Locally Then Copy (Fastest for Development)
Build the Next.js app locally first (native speed):
```bash
cd webui-nodejs
npm install
npm run build
```

Then create a minimal Dockerfile that just copies the built artifacts.

### Monitoring Build Progress

The build will show progress during:
1. **npm ci** - Installing dependencies (10-20 min on emulation)
2. **npm run build** - Next.js compilation (20-40 min on emulation)

Look for these messages in the output:
- `Installing npm dependencies` - Still working
- `Starting Next.js build` - Still working  
- `Generating static pages` - Still working
- `Build completed successfully` - Done!

### If Build Actually Hangs

1. **Check system resources:**
   ```bash
   podman machine inspect
   docker stats  # if using Docker
   ```

2. **Increase Podman machine memory:**
   ```bash
   podman machine stop
   podman machine set --memory 8192
   podman machine start
   ```

3. **Use the optimized Dockerfile:**
   ```bash
   cd webui-nodejs
   cp Dockerfile.optimized Dockerfile
   ```

4. **Kill stuck build and retry:**
   ```bash
   pkill -f "podman build.*webui"
   make docker-build-webui PLATFORM=linux/amd64 VERSION=0.11
   ```

### Performance Tips

1. **Use cache mounts** (requires BuildKit):
   ```dockerfile
   RUN --mount=type=cache,target=/root/.npm npm ci
   ```

2. **Disable telemetry:**
   ```dockerfile
   ENV NEXT_TELEMETRY_DISABLED=1
   ```

3. **Limit memory:**
   ```dockerfile
   ENV NODE_OPTIONS="--max-old-space-size=4096"
   ```

4. **Build during off-hours** - Let it run overnight if needed

### Common Errors

**Error: "Build failed or timed out"**
- The 30-40 minute timeout was exceeded
- Solution: Increase timeout in Dockerfile or build on faster hardware

**Error: "Out of memory"**
- Emulation requires more RAM
- Solution: Increase Podman machine memory to 8GB+

**Error: "Cannot find module"**
- Build cache issue
- Solution: Clear cache and rebuild: `podman system prune -a`











