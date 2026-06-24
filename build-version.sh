#!/bin/bash
# Build ImmersiveThunder for a specific Minecraft version
# Usage: ./build-version.sh <mc-version>
#   <mc-version> : 1.20.1 | 1.19.2 | 1.18.2
#
# Examples:
#   ./build-version.sh 1.20.1    # Build for Minecraft 1.20.1
#   ./build-version.sh 1.19.2    # Build for Minecraft 1.19.2
#   ./build-version.sh 1.18.2    # Build for Minecraft 1.18.2
#   ./build-version.sh all       # Build for all supported versions

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

SUPPORTED_VERSIONS=("1.20.1" "1.19.2" "1.18.2")

# Track if we need to restore gradle.properties
PROPERTIES_BACKED_UP=false

cleanup() {
    if [ "$PROPERTIES_BACKED_UP" = true ] && [ -f gradle.properties.bak ]; then
        mv gradle.properties.bak gradle.properties
        echo "Restored original gradle.properties"
    fi
}
trap cleanup EXIT

build_version() {
    local mc_version="$1"
    local prop_file="gradle-mc${mc_version}.properties"
    
    if [ ! -f "$prop_file" ]; then
        echo "ERROR: Properties file not found: $prop_file"
        echo "Supported versions: ${SUPPORTED_VERSIONS[*]}"
        exit 1
    fi
    
    echo ""
    echo "=========================================="
    echo " Building ImmersiveThunder for Minecraft $mc_version"
    echo "=========================================="
    
    # Save current gradle.properties and swap in version-specific one
    PROPERTIES_BACKED_UP=false
    if [ -f gradle.properties ]; then
        cp gradle.properties gradle.properties.bak
        PROPERTIES_BACKED_UP=true
    fi
    cp "$prop_file" gradle.properties
    
    # Ensure gradlew is executable
    chmod +x gradlew
    
    # Run the build
    ./gradlew clean build --no-daemon --stacktrace
    local build_result=$?
    
    # Restore original gradle.properties (also handled by cleanup trap)
    cleanup
    
    if [ $build_result -ne 0 ]; then
        echo "ERROR: Build failed for Minecraft $mc_version"
        exit $build_result
    fi
    
    echo "=========================================="
    echo " Build SUCCESS for Minecraft $mc_version"
    echo "=========================================="
}

# Parse version argument
if [ $# -eq 0 ]; then
    echo "Usage: $0 <mc-version>"
    echo "  <mc-version> : ${SUPPORTED_VERSIONS[*]} or 'all'"
    exit 1
fi

TARGET="$1"

if [ "$TARGET" = "all" ]; then
    for ver in "${SUPPORTED_VERSIONS[@]}"; do
        build_version "$ver"
    done
    echo ""
    echo "=========================================="
    echo " All builds completed successfully!"
    echo "=========================================="
    echo ""
    echo "Build artifacts:"
    ls -lh build/libs/*.jar 2>/dev/null || echo "(run a build first)"
else
    build_version "$TARGET"
fi
