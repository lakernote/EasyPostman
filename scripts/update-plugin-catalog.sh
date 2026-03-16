#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PUBLIC_CATALOG_DIR="${ROOT_DIR}/plugin-catalog"
EMBEDDED_CATALOG_DIR="${ROOT_DIR}/easy-postman-plugins/plugin-manager/src/main/resources/plugin-catalog"

usage() {
  cat <<'EOF'
Usage:
  ./scripts/update-plugin-catalog.sh
  ./scripts/update-plugin-catalog.sh --generated-at 2026-03-16T00:00:00Z
  ./scripts/update-plugin-catalog.sh --with-local-sha256

Options:
  --generated-at <value>  Override generatedAt timestamp. Default: current UTC time
  --with-local-sha256     Fill sha256 from local built plugin jars when available
  --help                  Show this help

Behavior:
  - Generate public official catalogs under plugin-catalog/
  - Sync the same catalog files into plugin-manager bundled resources
  - By default sha256 is left empty
  - With --with-local-sha256, sha256 is read from local plugin jars under
    easy-postman-plugins/plugin-*/target/
EOF
}

plugin_version() {
  local plugin="$1"
  python3 <<PY
import xml.etree.ElementTree as ET
ns = {"m": "http://maven.apache.org/POM/4.0.0"}
root = ET.parse("${ROOT_DIR}/easy-postman-plugins/plugin-${plugin}/pom.xml").getroot()
version = root.findtext("m:version", default="", namespaces=ns).strip()
if not version:
    raise SystemExit("missing <version> for plugin-${plugin}")
print(version)
PY
}

json_escape() {
  local value="${1:-}"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "${value}"
}

plugin_name() {
  case "$1" in
    redis) echo "Redis Plugin" ;;
    kafka) echo "Kafka Plugin" ;;
    git) echo "Git Plugin" ;;
    decompiler) echo "Decompiler Plugin" ;;
    client-cert) echo "Client Certificate Plugin" ;;
    *) echo "Unknown Plugin" ;;
  esac
}

plugin_description() {
  case "$1" in
    redis) echo "Redis toolbox panel, pm.redis script API, completions and snippets." ;;
    kafka) echo "Kafka toolbox panel, pm.kafka script API, completions and snippets." ;;
    git) echo "Git workspace operations, history and conflict checks powered by JGit." ;;
    decompiler) echo "Java decompiler toolbox panel powered by CFR." ;;
    client-cert) echo "Client certificate management and mTLS key material loading." ;;
    *) echo "" ;;
  esac
}

plugin_sha256() {
  local plugin="$1"
  local version="$2"
  local include_sha256="$3"
  local jar_path="${ROOT_DIR}/easy-postman-plugins/plugin-${plugin}/target/easy-postman-${version}-plugin-${plugin}.jar"
  if [[ "${include_sha256}" == true ]] && [[ -f "${jar_path}" ]] && command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "${jar_path}" | awk '{print $1}'
    return
  fi
  echo ""
}

plugin_download_url() {
  local source="$1"
  local plugin="$2"
  local version="$3"
  case "${source}" in
    github)
      echo "https://github.com/lakernote/easy-postman/releases/download/plugin-${plugin}-v${version}/easy-postman-${version}-plugin-${plugin}.jar"
      ;;
    gitee)
      echo "https://gitee.com/lakernote/easy-postman/releases/download/plugin-${plugin}-v${version}/easy-postman-${version}-plugin-${plugin}.jar"
      ;;
    *)
      echo ""
      ;;
  esac
}

plugin_homepage() {
  case "$1" in
    github) echo "https://github.com/lakernote/easy-postman" ;;
    gitee) echo "https://gitee.com/lakernote/easy-postman" ;;
    *) echo "" ;;
  esac
}

write_catalog() {
  local source="$1"
  local generated_at="$2"
  local include_sha256="$3"
  local output_path="$4"
  local source_title source_homepage
  local plugins=(redis kafka git decompiler client-cert)
  local plugin first=true

  mkdir -p "$(dirname "${output_path}")"

  case "${source}" in
    github) source_title="GitHub" ;;
    gitee) source_title="Gitee" ;;
    *) echo "Unsupported source: ${source}" >&2; exit 1 ;;
  esac

  source_homepage="$(plugin_homepage "${source}")"

  {
    printf '{\n'
    printf '  "name": "EasyPostman Official Plugin Catalog (%s)",\n' "${source_title}"
    printf '  "generatedAt": "%s",\n' "$(json_escape "${generated_at}")"
    printf '  "plugins": [\n'
    for plugin in "${plugins[@]}"; do
      local plugin_id="plugin-${plugin}"
      local version name description download_url sha256
      version="$(plugin_version "${plugin}")"
      name="$(plugin_name "${plugin}")"
      description="$(plugin_description "${plugin}")"
      download_url="$(plugin_download_url "${source}" "${plugin}" "${version}")"
      sha256="$(plugin_sha256 "${plugin}" "${version}" "${include_sha256}")"
      if [[ "${first}" == true ]]; then
        first=false
      else
        printf ',\n'
      fi
      printf '    {\n'
      printf '      "id": "%s",\n' "$(json_escape "${plugin_id}")"
      printf '      "name": "%s",\n' "$(json_escape "${name}")"
      printf '      "version": "%s",\n' "$(json_escape "${version}")"
      printf '      "description": "%s",\n' "$(json_escape "${description}")"
      printf '      "downloadUrl": "%s",\n' "$(json_escape "${download_url}")"
      printf '      "homepage": "%s",\n' "$(json_escape "${source_homepage}")"
      printf '      "sha256": "%s"\n' "$(json_escape "${sha256}")"
      printf '    }'
    done
    printf '\n'
    printf '  ]\n'
    printf '}\n'
  } > "${output_path}"
}

sync_catalog() {
  local filename="$1"
  cp -f "${PUBLIC_CATALOG_DIR}/${filename}" "${EMBEDDED_CATALOG_DIR}/${filename}"
}

main() {
  local generated_at=""
  local include_sha256=false

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --generated-at)
        [[ $# -ge 2 ]] || { echo "Missing value for --generated-at" >&2; exit 1; }
        generated_at="$2"
        shift 2
        ;;
      --with-local-sha256)
        include_sha256=true
        shift
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      *)
        echo "Unknown option: $1" >&2
        usage
        exit 1
        ;;
    esac
  done

  if [[ -z "${generated_at}" ]]; then
    generated_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  fi

  mkdir -p "${PUBLIC_CATALOG_DIR}" "${EMBEDDED_CATALOG_DIR}"
  write_catalog "github" "${generated_at}" "${include_sha256}" "${PUBLIC_CATALOG_DIR}/catalog-github.json"
  write_catalog "gitee" "${generated_at}" "${include_sha256}" "${PUBLIC_CATALOG_DIR}/catalog-gitee.json"
  sync_catalog "catalog-github.json"
  sync_catalog "catalog-gitee.json"

  echo "[plugin-catalog] Generated catalogs from per-plugin pom versions"
  echo "[plugin-catalog] Public catalogs:"
  echo "  - ${PUBLIC_CATALOG_DIR}/catalog-github.json"
  echo "  - ${PUBLIC_CATALOG_DIR}/catalog-gitee.json"
  echo "[plugin-catalog] Bundled catalogs:"
  echo "  - ${EMBEDDED_CATALOG_DIR}/catalog-github.json"
  echo "  - ${EMBEDDED_CATALOG_DIR}/catalog-gitee.json"
}

main "$@"
