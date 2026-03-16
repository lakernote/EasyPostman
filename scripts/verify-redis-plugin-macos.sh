#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_DIR="${ROOT_DIR}/temp/redis-plugin-verify"
ARTIFACT_DIR="${VERIFY_DIR}/artifacts"
DATA_DIR="${VERIFY_DIR}/data"
PLUGIN_DIR="${DATA_DIR}/plugins/installed"
APP_JAR="${ARTIFACT_DIR}/easy-postman.jar"
PLUGIN_JAR="${ARTIFACT_DIR}/easy-postman-plugin-redis.jar"
CATALOG_FILE="${VERIFY_DIR}/catalog.json"
SHA256_FILE="${VERIFY_DIR}/easy-postman-plugin-redis.sha256.txt"
JAVA_BIN="${JAVA_BIN:-java}"
MAVEN_BIN="${MAVEN_BIN:-mvn}"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/verify-redis-plugin-macos.sh prepare
  ./scripts/verify-redis-plugin-macos.sh run-clean
  ./scripts/verify-redis-plugin-macos.sh install-direct
  ./scripts/verify-redis-plugin-macos.sh run-with-plugin
  ./scripts/verify-redis-plugin-macos.sh reset
  ./scripts/verify-redis-plugin-macos.sh info

Commands:
  prepare          Build app + Redis plugin, verify jar boundaries, generate local verify sandbox.
  run-clean        Launch EasyPostman in isolated mode with an empty plugin dir and a prefilled local catalog URL.
  install-direct   Copy the Redis plugin jar into the isolated managed plugin dir.
  run-with-plugin  Install the Redis plugin into the isolated plugin dir, then launch the app.
  reset            Remove the isolated verify data directory.
  info             Print the generated local verify paths and commands.
EOF
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

project_version() {
  awk '
    /<parent>/ { in_parent=1 }
    /<\/parent>/ { in_parent=0 }
    !in_parent && /<version>/ {
      match($0, /<version>[^<]+<\/version>/)
      if (RSTART > 0) {
        value=substr($0, RSTART + 9, RLENGTH - 19)
        print value
        exit
      }
    }
  ' "${ROOT_DIR}/pom.xml"
}

abs_path() {
  local target="$1"
  printf '%s/%s\n' "$(cd "$(dirname "${target}")" && pwd)" "$(basename "${target}")"
}

file_uri() {
  local path
  path="$(abs_path "$1")"
  printf 'file://%s\n' "${path// /%20}"
}

build_artifacts() {
  require_cmd "${MAVEN_BIN}"
  require_cmd "${JAVA_BIN}"
  require_cmd jar
  require_cmd shasum
  require_cmd unzip

  local version
  version="$(project_version)"

  echo "[redis-verify] Building project version ${version}"
  "${MAVEN_BIN}" -DskipTests clean package

  local app_source="${ROOT_DIR}/easy-postman-app/target/easy-postman-${version}.jar"
  local plugin_source="${ROOT_DIR}/plugins/plugin-redis/target/easy-postman-${version}-plugin-redis.jar"

  [[ -f "${app_source}" ]] || {
    echo "App jar not found: ${app_source}" >&2
    exit 1
  }
  [[ -f "${plugin_source}" ]] || {
    echo "Redis plugin jar not found: ${plugin_source}" >&2
    exit 1
  }

  mkdir -p "${ARTIFACT_DIR}" "${PLUGIN_DIR}"
  cp -f "${app_source}" "${APP_JAR}"
  cp -f "${plugin_source}" "${PLUGIN_JAR}"
}

verify_boundaries() {
  echo "[redis-verify] Verifying app/plugin jar boundaries"

  if jar tf "${APP_JAR}" | grep -Eq 'com/laker/postman/plugin/redis|icons/redis\.svg|redis-messages'; then
    echo "App jar still contains Redis plugin content: ${APP_JAR}" >&2
    exit 1
  fi

  jar tf "${PLUGIN_JAR}" | grep -Eq 'com/laker/postman/plugin/redis/RedisPlugin.class' || {
    echo "Redis plugin jar is missing RedisPlugin.class: ${PLUGIN_JAR}" >&2
    exit 1
  }
  jar tf "${PLUGIN_JAR}" | grep -Eq 'META-INF/easy-postman/plugin-redis.properties' || {
    echo "Redis plugin jar is missing plugin descriptor: ${PLUGIN_JAR}" >&2
    exit 1
  }
  jar tf "${PLUGIN_JAR}" | grep -Eq 'icons/redis\.svg' || {
    echo "Redis plugin jar is missing its icon resource: ${PLUGIN_JAR}" >&2
    exit 1
  }
}

write_catalog() {
  local plugin_uri
  local homepage
  local sha256

  plugin_uri="$(file_uri "${PLUGIN_JAR}")"
  homepage="https://github.com/lakernote/easy-postman"
  sha256="$(shasum -a 256 "${PLUGIN_JAR}" | awk '{print $1}')"

  cat > "${SHA256_FILE}" <<EOF
${sha256}  $(basename "${PLUGIN_JAR}")
EOF

  cat > "${CATALOG_FILE}" <<EOF
{
  "name": "EasyPostman Local Redis Plugin Catalog",
  "generatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "plugins": [
    {
      "id": "plugin-redis",
      "name": "Redis Plugin",
      "version": "$(unzip -p "${PLUGIN_JAR}" META-INF/easy-postman/plugin-redis.properties | awk -F= '/^plugin.version=/{print $2}')",
      "description": "Redis toolbox panel, pm.redis script API, completions and snippets.",
      "downloadUrl": "${plugin_uri}",
      "homepage": "${homepage}",
      "sha256": "${sha256}"
    }
  ]
}
EOF
}

write_helper_scripts() {
  local catalog_uri
  catalog_uri="$(file_uri "${CATALOG_FILE}")"

  cat > "${VERIFY_DIR}/launch-clean.command" <<EOF
#!/bin/bash
set -euo pipefail
rm -rf "${DATA_DIR}"
mkdir -p "${PLUGIN_DIR}"
exec "${JAVA_BIN}" -DeasyPostman.data.dir="${DATA_DIR}" -DeasyPostman.plugins.catalogUrl="${catalog_uri}" -jar "${APP_JAR}"
EOF

  cat > "${VERIFY_DIR}/install-redis.command" <<EOF
#!/bin/bash
set -euo pipefail
mkdir -p "${PLUGIN_DIR}"
cp -f "${PLUGIN_JAR}" "${PLUGIN_DIR}/easy-postman-plugin-redis.jar"
echo "Installed Redis plugin into: ${PLUGIN_DIR}"
echo "Restart EasyPostman after direct installation."
EOF

  cat > "${VERIFY_DIR}/launch-with-redis.command" <<EOF
#!/bin/bash
set -euo pipefail
mkdir -p "${PLUGIN_DIR}"
cp -f "${PLUGIN_JAR}" "${PLUGIN_DIR}/easy-postman-plugin-redis.jar"
exec "${JAVA_BIN}" -DeasyPostman.data.dir="${DATA_DIR}" -DeasyPostman.plugins.catalogUrl="${catalog_uri}" -jar "${APP_JAR}"
EOF

  cat > "${VERIFY_DIR}/reset-verify.command" <<EOF
#!/bin/bash
set -euo pipefail
rm -rf "${DATA_DIR}"
mkdir -p "${PLUGIN_DIR}"
echo "Reset verify data dir: ${DATA_DIR}"
EOF

  chmod +x \
    "${VERIFY_DIR}/launch-clean.command" \
    "${VERIFY_DIR}/install-redis.command" \
    "${VERIFY_DIR}/launch-with-redis.command" \
    "${VERIFY_DIR}/reset-verify.command"
}

prepare() {
  rm -rf "${VERIFY_DIR}"
  mkdir -p "${VERIFY_DIR}" "${ARTIFACT_DIR}" "${PLUGIN_DIR}"
  build_artifacts
  verify_boundaries
  write_catalog
  write_helper_scripts
  print_info
}

launch_clean() {
  [[ -f "${APP_JAR}" && -f "${CATALOG_FILE}" ]] || prepare
  rm -rf "${DATA_DIR}"
  mkdir -p "${PLUGIN_DIR}"
  exec "${JAVA_BIN}" -DeasyPostman.data.dir="${DATA_DIR}" -DeasyPostman.plugins.catalogUrl="$(file_uri "${CATALOG_FILE}")" -jar "${APP_JAR}"
}

install_direct() {
  [[ -f "${PLUGIN_JAR}" ]] || prepare
  mkdir -p "${PLUGIN_DIR}"
  cp -f "${PLUGIN_JAR}" "${PLUGIN_DIR}/easy-postman-plugin-redis.jar"
  echo "Installed Redis plugin into: ${PLUGIN_DIR}"
}

launch_with_plugin() {
  [[ -f "${APP_JAR}" && -f "${PLUGIN_JAR}" && -f "${CATALOG_FILE}" ]] || prepare
  install_direct
  exec "${JAVA_BIN}" -DeasyPostman.data.dir="${DATA_DIR}" -DeasyPostman.plugins.catalogUrl="$(file_uri "${CATALOG_FILE}")" -jar "${APP_JAR}"
}

reset_verify() {
  rm -rf "${DATA_DIR}"
  mkdir -p "${PLUGIN_DIR}"
  echo "Reset verify data dir: ${DATA_DIR}"
}

print_info() {
  cat <<EOF
[redis-verify] Ready

Verify root:
  ${VERIFY_DIR}

Artifacts:
  App jar:    ${APP_JAR}
  Plugin jar: ${PLUGIN_JAR}
  Catalog:    ${CATALOG_FILE}
  SHA256:     ${SHA256_FILE}

Recommended local validation flow:
  1. Clean launch (market install path):
     ./scripts/verify-redis-plugin-macos.sh run-clean
  2. In EasyPostman, open Plugin Manager -> Marketplace.
     Catalog URL is already prefilled with:
     $(file_uri "${CATALOG_FILE}")
  3. Install Redis plugin from the local catalog, then restart the app.
  4. Relaunch with the plugin already installed:
     ./scripts/verify-redis-plugin-macos.sh run-with-plugin

Helper files for macOS Finder double-click:
  ${VERIFY_DIR}/launch-clean.command
  ${VERIFY_DIR}/install-redis.command
  ${VERIFY_DIR}/launch-with-redis.command
  ${VERIFY_DIR}/reset-verify.command
EOF
}

main() {
  local command="${1:-prepare}"
  case "${command}" in
    prepare)
      prepare
      ;;
    run-clean)
      launch_clean
      ;;
    install-direct)
      install_direct
      ;;
    run-with-plugin)
      launch_with_plugin
      ;;
    reset)
      reset_verify
      ;;
    info)
      print_info
      ;;
    -h|--help|help)
      usage
      ;;
    *)
      echo "Unknown command: ${command}" >&2
      usage
      exit 1
      ;;
  esac
}

main "$@"
