#!/bin/bash
set -e

# Define targets
TARGETS=("aarch64-linux-android" "armv7-linux-androideabi" "i686-linux-android" "x86_64-linux-android")

# Navigate to Rust project root
cd "$(dirname "$0")/rust"

echo "Building Rust targets..."

for target in "${TARGETS[@]}"; do
    echo "Building for target: $target"
    cargo build --target "$target" --release
done

echo "Rust build completed successfully."
