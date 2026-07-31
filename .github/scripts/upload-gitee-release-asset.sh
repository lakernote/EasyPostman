#!/usr/bin/env bash

set -euo pipefail

RELEASE_ID="${1:-}"
FILE="${2:-}"
GITEE_REPOSITORY="${GITEE_REPOSITORY:-lakernote/easy-postman}"
MAX_ATTEMPTS="${GITEE_UPLOAD_MAX_ATTEMPTS:-4}"
RETRY_DELAY_SECONDS="${GITEE_UPLOAD_RETRY_DELAY_SECONDS:-10}"
API_BASE_URL="https://gitee.com/api/v5/repos/${GITEE_REPOSITORY}/releases"

if [ -z "${GITEE_TOKEN:-}" ]; then
  echo "❌ Missing GITEE_TOKEN secret." >&2
  exit 1
fi

if [ -z "$RELEASE_ID" ]; then
  echo "❌ Missing Gitee release ID." >&2
  exit 1
fi

if [ -z "$FILE" ] || [ ! -f "$FILE" ]; then
  echo "❌ Release asset not found: ${FILE:-<empty>}" >&2
  exit 1
fi

FILE_NAME="$(basename "$FILE")"
FILE_SIZE="$(stat -c%s "$FILE" 2>/dev/null || stat -f%z "$FILE")"
FILE_SIZE_MB="$(( (FILE_SIZE + 1048575) / 1048576 ))"
RELEASE_RESPONSE_FILE="$(mktemp)"
UPLOAD_RESPONSE_FILE="$(mktemp)"
trap 'rm -f "$RELEASE_RESPONSE_FILE" "$UPLOAD_RESPONSE_FILE"' EXIT

if [ "$FILE_SIZE" -gt 104857600 ]; then
  echo "❌ ${FILE_NAME} is ${FILE_SIZE_MB} MB and exceeds the Gitee 100 MB attachment limit." >&2
  exit 1
fi

asset_exists() {
  local curl_exit_code
  local http_code

  if http_code="$(curl -sS -o "$RELEASE_RESPONSE_FILE" -w "%{http_code}" \
      --retry 3 \
      --retry-delay 2 \
      --retry-all-errors \
      --connect-timeout 30 \
      --max-time 60 \
      --get \
      --data-urlencode "access_token=${GITEE_TOKEN}" \
      "${API_BASE_URL}/${RELEASE_ID}")"; then
    curl_exit_code=0
  else
    curl_exit_code=$?
  fi

  if [ "$curl_exit_code" -ne 0 ] || [ "$http_code" -lt 200 ] || [ "$http_code" -ge 300 ]; then
    echo "❌ Failed to inspect Gitee release ${RELEASE_ID}: curl_exit=${curl_exit_code}, http_code=${http_code}" >&2
    cat "$RELEASE_RESPONSE_FILE" >&2 || true
    return 2
  fi

  if jq -e --arg file_name "$FILE_NAME" \
      'any(.assets[]?; .name == $file_name)' \
      "$RELEASE_RESPONSE_FILE" >/dev/null; then
    return 0
  fi

  return 1
}

for ((attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)); do
  if asset_exists; then
    echo "✅ ${FILE_NAME} already exists in Gitee release ${RELEASE_ID}; skipping upload."
    exit 0
  else
    ASSET_LOOKUP_EXIT_CODE=$?
  fi

  if [ "$ASSET_LOOKUP_EXIT_CODE" -ne 1 ]; then
    exit "$ASSET_LOOKUP_EXIT_CODE"
  fi

  echo "📦 Uploading ${FILE_NAME} (${FILE_SIZE_MB} MB) to Gitee, attempt ${attempt}/${MAX_ATTEMPTS}..."

  if UPLOAD_HTTP_CODE="$(curl --fail-with-body \
      -o "$UPLOAD_RESPONSE_FILE" \
      -w "%{http_code}" \
      --connect-timeout 30 \
      --speed-limit 1024 \
      --speed-time 300 \
      -X POST "${API_BASE_URL}/${RELEASE_ID}/attach_files" \
      -F "access_token=${GITEE_TOKEN}" \
      -F "file=@${FILE}")"; then
    UPLOAD_EXIT_CODE=0
  else
    UPLOAD_EXIT_CODE=$?
  fi

  if [ "$UPLOAD_EXIT_CODE" -eq 0 ] && [ "$UPLOAD_HTTP_CODE" -ge 200 ] && [ "$UPLOAD_HTTP_CODE" -lt 300 ]; then
    echo "✅ ${FILE_NAME} uploaded successfully."
    exit 0
  fi

  echo "⚠️ Upload failed: curl_exit=${UPLOAD_EXIT_CODE}, http_code=${UPLOAD_HTTP_CODE}" >&2
  cat "$UPLOAD_RESPONSE_FILE" >&2 || true

  if [ "$attempt" -lt "$MAX_ATTEMPTS" ]; then
    echo "⏳ Retrying in ${RETRY_DELAY_SECONDS} seconds; the next attempt will first check whether Gitee saved the asset."
    sleep "$RETRY_DELAY_SECONDS"
  fi
done

echo "❌ Failed to upload ${FILE_NAME} after ${MAX_ATTEMPTS} attempts." >&2
exit 1
