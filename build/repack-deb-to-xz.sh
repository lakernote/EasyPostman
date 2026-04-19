#!/bin/bash

set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <package.deb>" >&2
    exit 1
fi

DEB_PATH="$1"

if [ ! -f "$DEB_PATH" ]; then
    echo "DEB file not found: $DEB_PATH" >&2
    exit 1
fi

DEB_PATH=$(cd "$(dirname "$DEB_PATH")" && pwd)/$(basename "$DEB_PATH")

if ! command -v ar >/dev/null 2>&1; then
    echo "Missing required command: ar" >&2
    exit 1
fi

if ! command -v tar >/dev/null 2>&1; then
    echo "Missing required command: tar" >&2
    exit 1
fi

if ! command -v xz >/dev/null 2>&1; then
    echo "Missing required command: xz" >&2
    exit 1
fi

if ! command -v gzip >/dev/null 2>&1; then
    echo "Missing required command: gzip" >&2
    exit 1
fi

if ! command -v zstd >/dev/null 2>&1; then
    echo "Missing required command: zstd" >&2
    exit 1
fi

TMP_DIR=$(mktemp -d)
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

cp "$DEB_PATH" "$TMP_DIR/input.deb"
cd "$TMP_DIR"
ar x input.deb

CONTROL_MEMBER=$(find . -maxdepth 1 -type f -name 'control.tar.*' | head -n 1)
DATA_MEMBER=$(find . -maxdepth 1 -type f -name 'data.tar.*' | head -n 1)

if [ ! -f "./debian-binary" ] || [ -z "$CONTROL_MEMBER" ] || [ -z "$DATA_MEMBER" ]; then
    echo "Unexpected DEB structure in $DEB_PATH" >&2
    exit 1
fi

normalize_to_xz() {
    local input_path="$1"
    local base_name
    base_name=$(basename "$input_path")
    local output_path="${base_name%%.tar.*}.tar.xz"

    case "$base_name" in
        *.tar.xz)
            :
            ;;
        *.tar.gz)
            gzip -d -c "$input_path" | xz -T0 -c > "$output_path"
            ;;
        *.tar.zst)
            zstd -d -q -c "$input_path" | xz -T0 -c > "$output_path"
            ;;
        *.tar)
            xz -T0 -c "$input_path" > "$output_path"
            ;;
        *)
            echo "Unsupported tar member format: $base_name" >&2
            exit 1
            ;;
    esac
}

normalize_to_xz "$CONTROL_MEMBER"
normalize_to_xz "$DATA_MEMBER"

if [ "$(basename "$CONTROL_MEMBER")" != "control.tar.xz" ]; then
    rm -f "$(basename "$CONTROL_MEMBER")"
fi

if [ "$(basename "$DATA_MEMBER")" != "data.tar.xz" ]; then
    rm -f "$(basename "$DATA_MEMBER")"
fi

rm -f output.deb
ar cr output.deb debian-binary control.tar.xz data.tar.xz
mv output.deb "$DEB_PATH"
