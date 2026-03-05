#!/bin/bash
# Script to verify x86_64 build compatibility

set -e

IMAGE_NAME="${1:-hrexed/cloudnatviescaner-webui:0.20}"
PLATFORM="linux/amd64"

echo "Verifying x86_64 build compatibility for: $IMAGE_NAME"
echo ""

# Check if image exists
if ! podman images | grep -q "$(echo $IMAGE_NAME | cut -d: -f1)"; then
    echo "❌ Image not found. Please build it first:"
    echo "   make docker-build-webui PLATFORM=linux/amd64 VERSION=0.20"
    exit 1
fi

echo "✅ Image found: $IMAGE_NAME"
echo ""

# Inspect image architecture
echo "Checking image architecture..."
ARCH=$(podman inspect $IMAGE_NAME | jq -r '.[0].Architecture' 2>/dev/null || podman inspect $IMAGE_NAME | grep -i architecture | head -1)
echo "Architecture: $ARCH"

if [ "$ARCH" = "amd64" ] || [ "$ARCH" = "x86_64" ]; then
    echo "✅ Image is x86_64/amd64 compatible"
else
    echo "⚠️  Warning: Image architecture is $ARCH (expected amd64/x86_64)"
fi

echo ""
echo "Image details:"
podman inspect $IMAGE_NAME | jq -r '.[0] | "Image ID: \(.Id[:12])\nCreated: \(.Created)\nSize: \(.Size / 1024 / 1024) MB\nPlatform: \(.Architecture)/\(.Os)"' 2>/dev/null || podman inspect $IMAGE_NAME | grep -E "(Id|Created|Size|Architecture|Os)" | head -5

echo ""
echo "✅ Verification complete"




