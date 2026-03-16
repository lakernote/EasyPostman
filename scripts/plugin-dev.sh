#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMP_ROOT="${ROOT_DIR}/temp/plugin-dev"
JAVA_BIN="${JAVA_BIN:-java}"
MAVEN_BIN="${MAVEN_BIN:-mvn}"
MAVEN_ARGS=(${MAVEN_ARGS:-})

usage() {
  cat <<'EOF'
Usage:
  ./scripts/plugin-dev.sh list
  ./scripts/plugin-dev.sh build <plugin|all>
  ./scripts/plugin-dev.sh prepare <plugin>
  ./scripts/plugin-dev.sh run-clean <plugin>
  ./scripts/plugin-dev.sh install-direct <plugin>
  ./scripts/plugin-dev.sh run-with-plugin <plugin>
  ./scripts/plugin-dev.sh info <plugin>
  ./scripts/plugin-dev.sh reset <plugin>

Plugins:
  redis | kafka | git | decompiler | client-cert

Commands:
  list             Print supported plugin ids.
  build            Build the app and the selected plugin, or all plugins.
  prepare          Build app + plugin, verify jar boundaries, generate isolated local catalog.
  run-clean        Launch EasyPostman with an empty isolated plugin dir and prefilled local catalog URL.
  install-direct   Copy the selected plugin jar into the isolated managed plugin dir.
  run-with-plugin  Install the selected plugin, then launch the app with the local catalog URL.
  info             Print local paths, packaged offline bundle path, and recommended verification flow.
  reset            Remove the isolated verify data for the selected plugin.

Examples:
  ./scripts/plugin-dev.sh prepare redis
  ./scripts/plugin-dev.sh run-clean git
  ./scripts/plugin-dev.sh build all
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
    /<revision>/ {
      match($0, /<revision>[^<]+<\/revision>/)
      if (RSTART > 0) {
        value=substr($0, RSTART + 10, RLENGTH - 21)
        print value
        exit
      }
    }
  ' "${ROOT_DIR}/pom.xml"
}

plugin_exists() {
  case "$1" in
    redis|kafka|git|decompiler|client-cert) return 0 ;;
    *) return 1 ;;
  esac
}

plugin_label() {
  case "$1" in
    redis) echo "Redis" ;;
    kafka) echo "Kafka" ;;
    git) echo "Git" ;;
    decompiler) echo "Decompiler" ;;
    client-cert) echo "Client Certificate" ;;
  esac
}

plugin_package_path() {
  case "$1" in
    redis) echo "com/laker/postman/plugin/redis" ;;
    kafka) echo "com/laker/postman/plugin/kafka" ;;
    git) echo "com/laker/postman/plugin/git" ;;
    decompiler) echo "com/laker/postman/plugin/decompiler" ;;
    client-cert) echo "com/laker/postman/plugin/clientcert" ;;
  esac
}

plugin_entry_class() {
  case "$1" in
    redis) echo "RedisPlugin.class" ;;
    kafka) echo "KafkaPlugin.class" ;;
    git) echo "GitPlugin.class" ;;
    decompiler) echo "DecompilerPlugin.class" ;;
    client-cert) echo "ClientCertificatePlugin.class" ;;
  esac
}

plugin_description() {
  case "$1" in
    redis) echo "Redis toolbox panel, pm.redis script API, completions and snippets." ;;
    kafka) echo "Kafka toolbox panel, pm.kafka script API, completions and snippets." ;;
    git) echo "Git workspace actions, repository status, sync and auth operations." ;;
    decompiler) echo "Java decompiler toolbox panel powered by CFR." ;;
    client-cert) echo "Client certificate management and mTLS key material loading." ;;
  esac
}

plugin_homepage() {
  echo "https://github.com/lakernote/easy-postman"
}

plugin_module_dir() {
  echo "${ROOT_DIR}/easy-postman-plugins/plugin-$1"
}

verify_root() {
  echo "${TEMP_ROOT}/$1"
}

artifact_dir() {
  echo "$(verify_root "$1")/artifacts"
}

data_dir() {
  echo "$(verify_root "$1")/data"
}

installed_plugin_dir() {
  echo "$(data_dir "$1")/plugins/installed"
}

app_jar_path() {
  echo "$(artifact_dir "$1")/easy-postman.jar"
}

plugin_jar_path() {
  echo "$(artifact_dir "$1")/easy-postman-plugin-$1.jar"
}

catalog_file_path() {
  echo "$(verify_root "$1")/catalog.json"
}

sha256_file_path() {
  echo "$(verify_root "$1")/easy-postman-plugin-$1.sha256.txt"
}

offline_bundle_dir() {
  echo "$(plugin_module_dir "$1")/target/plugin-market/offline/easy-postman-plugin-$1"
}

abs_path() {
  local target="$1"
  printf '%s/%s\n' "$(cd "$(dirname "$target")" && pwd)" "$(basename "$target")"
}

file_uri() {
  local path
  path="$(abs_path "$1")"
  printf 'file://%s\n' "${path// /%20}"
}

ensure_plugin() {
  local plugin="${1:-}"
  if [[ -z "${plugin}" ]] || ! plugin_exists "${plugin}"; then
    echo "Unknown or missing plugin id: ${plugin:-<empty>}" >&2
    usage
    exit 1
  fi
}

list_plugins() {
  printf '%s\n' redis kafka git decompiler client-cert
}

run_maven() {
  local -a cmd=("${MAVEN_BIN}")
  if [[ ${#MAVEN_ARGS[@]} -gt 0 ]]; then
    cmd+=("${MAVEN_ARGS[@]}")
  fi
  cmd+=("$@")
  "${cmd[@]}"
}

build_target() {
  local target="${1:-}"
  require_cmd "${MAVEN_BIN}"
  require_cmd "${JAVA_BIN}"
  require_cmd jar
  require_cmd shasum
  require_cmd unzip

  if [[ "${target}" == "all" ]]; then
    echo "[plugin-dev] Building app and all plugin modules"
    run_maven -DskipTests clean package
    return
  fi

  ensure_plugin "${target}"
  echo "[plugin-dev] Building app and plugin-${target}"
  run_maven -DskipTests -pl easy-postman-app,easy-postman-plugins/plugin-"${target}" -am clean package
}

copy_artifacts() {
  local plugin="$1"
  local version app_source plugin_source

  version="$(project_version)"
  app_source="${ROOT_DIR}/easy-postman-app/target/easy-postman-${version}.jar"
  plugin_source="$(plugin_module_dir "${plugin}")/target/easy-postman-${version}-plugin-${plugin}.jar"

  [[ -f "${app_source}" ]] || {
    echo "App jar not found: ${app_source}" >&2
    exit 1
  }
  [[ -f "${plugin_source}" ]] || {
    echo "Plugin jar not found: ${plugin_source}" >&2
    exit 1
  }

  mkdir -p "$(artifact_dir "${plugin}")" "$(installed_plugin_dir "${plugin}")"
  cp -f "${app_source}" "$(app_jar_path "${plugin}")"
  cp -f "${plugin_source}" "$(plugin_jar_path "${plugin}")"
}

verify_boundaries() {
  local plugin="$1"
  local package_path entry_class

  package_path="$(plugin_package_path "${plugin}")"
  entry_class="$(plugin_entry_class "${plugin}")"

  echo "[plugin-dev] Verifying app/plugin jar boundaries for ${plugin}"

  if jar tf "$(app_jar_path "${plugin}")" | grep -Eq "${package_path}|META-INF/easy-postman/plugin-${plugin}\.properties"; then
    echo "App jar still contains plugin-${plugin} content: $(app_jar_path "${plugin}")" >&2
    exit 1
  fi

  jar tf "$(plugin_jar_path "${plugin}")" | grep -Eq "${package_path}/${entry_class}" || {
    echo "Plugin jar is missing ${entry_class}: $(plugin_jar_path "${plugin}")" >&2
    exit 1
  }
  jar tf "$(plugin_jar_path "${plugin}")" | grep -Eq "META-INF/easy-postman/plugin-${plugin}\.properties" || {
    echo "Plugin jar is missing descriptor: $(plugin_jar_path "${plugin}")" >&2
    exit 1
  }
}

write_catalog() {
  local plugin="$1"
  local plugin_uri version sha256

  plugin_uri="$(file_uri "$(plugin_jar_path "${plugin}")")"
  version="$(unzip -p "$(plugin_jar_path "${plugin}")" META-INF/easy-postman/plugin-${plugin}.properties | awk -F= '/^plugin.version=/{print $2}')"
  sha256="$(shasum -a 256 "$(plugin_jar_path "${plugin}")" | awk '{print $1}')"

  cat > "$(sha256_file_path "${plugin}")" <<EOF
${sha256}  $(basename "$(plugin_jar_path "${plugin}")")
EOF

  cat > "$(catalog_file_path "${plugin}")" <<EOF
{
  "name": "EasyPostman Local $(plugin_label "${plugin}") Plugin Catalog",
  "generatedAt": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "plugins": [
    {
      "id": "plugin-${plugin}",
      "name": "$(plugin_label "${plugin}") Plugin",
      "version": "${version}",
      "description": "$(plugin_description "${plugin}")",
      "downloadUrl": "${plugin_uri}",
      "homepage": "$(plugin_homepage "${plugin}")",
      "sha256": "${sha256}"
    }
  ]
}
EOF
}

write_helper_scripts() {
  local plugin="$1"
  local verify_dir script_path

  verify_dir="$(verify_root "${plugin}")"
  script_path="${ROOT_DIR}/scripts/plugin-dev.sh"

  cat > "${verify_dir}/launch-clean.command" <<EOF
#!/bin/bash
set -euo pipefail
exec "${script_path}" run-clean "${plugin}"
EOF

  cat > "${verify_dir}/install-plugin.command" <<EOF
#!/bin/bash
set -euo pipefail
exec "${script_path}" install-direct "${plugin}"
EOF

  cat > "${verify_dir}/launch-with-plugin.command" <<EOF
#!/bin/bash
set -euo pipefail
exec "${script_path}" run-with-plugin "${plugin}"
EOF

  cat > "${verify_dir}/reset-verify.command" <<EOF
#!/bin/bash
set -euo pipefail
exec "${script_path}" reset "${plugin}"
EOF

  chmod +x \
    "${verify_dir}/launch-clean.command" \
    "${verify_dir}/install-plugin.command" \
    "${verify_dir}/launch-with-plugin.command" \
    "${verify_dir}/reset-verify.command"
}

prepare_plugin() {
  local plugin="$1"

  rm -rf "$(verify_root "${plugin}")"
  mkdir -p "$(verify_root "${plugin}")" "$(artifact_dir "${plugin}")" "$(installed_plugin_dir "${plugin}")"
  build_target "${plugin}"
  copy_artifacts "${plugin}"
  verify_boundaries "${plugin}"
  write_catalog "${plugin}"
  write_helper_scripts "${plugin}"
  print_info "${plugin}"
}

ensure_prepared() {
  local plugin="$1"
  [[ -f "$(app_jar_path "${plugin}")" && -f "$(plugin_jar_path "${plugin}")" && -f "$(catalog_file_path "${plugin}")" ]] || prepare_plugin "${plugin}"
}

launch_clean() {
  local plugin="$1"

  ensure_prepared "${plugin}"
  rm -rf "$(data_dir "${plugin}")"
  mkdir -p "$(installed_plugin_dir "${plugin}")"
  exec "${JAVA_BIN}" \
    -DeasyPostman.data.dir="$(data_dir "${plugin}")" \
    -DeasyPostman.plugins.catalogUrl="$(file_uri "$(catalog_file_path "${plugin}")")" \
    -jar "$(app_jar_path "${plugin}")"
}

install_direct() {
  local plugin="$1"

  ensure_prepared "${plugin}"
  mkdir -p "$(installed_plugin_dir "${plugin}")"
  cp -f "$(plugin_jar_path "${plugin}")" "$(installed_plugin_dir "${plugin}")/easy-postman-plugin-${plugin}.jar"
  echo "Installed plugin-${plugin} into: $(installed_plugin_dir "${plugin}")"
}

launch_with_plugin() {
  local plugin="$1"

  ensure_prepared "${plugin}"
  install_direct "${plugin}"
  exec "${JAVA_BIN}" \
    -DeasyPostman.data.dir="$(data_dir "${plugin}")" \
    -DeasyPostman.plugins.catalogUrl="$(file_uri "$(catalog_file_path "${plugin}")")" \
    -jar "$(app_jar_path "${plugin}")"
}

reset_verify() {
  local plugin="$1"

  rm -rf "$(data_dir "${plugin}")"
  mkdir -p "$(installed_plugin_dir "${plugin}")"
  echo "Reset verify data dir: $(data_dir "${plugin}")"
}

print_info() {
  local plugin="$1"

  cat <<EOF
[plugin-dev] Ready for plugin-${plugin}

Verify root:
  $(verify_root "${plugin}")

Artifacts:
  App jar:         $(app_jar_path "${plugin}")
  Plugin jar:      $(plugin_jar_path "${plugin}")
  Local catalog:   $(catalog_file_path "${plugin}")
  SHA256:          $(sha256_file_path "${plugin}")
  Offline bundle:  $(offline_bundle_dir "${plugin}")
  Installed dir:   $(installed_plugin_dir "${plugin}")

Recommended local validation flow:
  1. Clean launch for marketplace install:
     ./scripts/plugin-dev.sh run-clean ${plugin}
  2. In EasyPostman -> Plugin Manager:
     - Marketplace: the catalog URL is prefilled with
       $(file_uri "$(catalog_file_path "${plugin}")")
     - Direct install: choose
       $(plugin_jar_path "${plugin}")
     - Offline bundle: choose directory or catalog
       $(offline_bundle_dir "${plugin}")
  3. Restart the app after installation.
  4. Relaunch with the plugin already installed:
     ./scripts/plugin-dev.sh run-with-plugin ${plugin}

Helper files for macOS Finder double-click:
  $(verify_root "${plugin}")/launch-clean.command
  $(verify_root "${plugin}")/install-plugin.command
  $(verify_root "${plugin}")/launch-with-plugin.command
  $(verify_root "${plugin}")/reset-verify.command
EOF
}

main() {
  local command="${1:-help}"
  local plugin="${2:-}"

  case "${command}" in
    list)
      list_plugins
      ;;
    build)
      local target="${plugin:-all}"
      if [[ "${target}" != "all" ]]; then
        ensure_plugin "${target}"
      fi
      build_target "${target}"
      ;;
    prepare)
      ensure_plugin "${plugin}"
      prepare_plugin "${plugin}"
      ;;
    run-clean)
      ensure_plugin "${plugin}"
      launch_clean "${plugin}"
      ;;
    install-direct)
      ensure_plugin "${plugin}"
      install_direct "${plugin}"
      ;;
    run-with-plugin)
      ensure_plugin "${plugin}"
      launch_with_plugin "${plugin}"
      ;;
    info)
      ensure_plugin "${plugin}"
      print_info "${plugin}"
      ;;
    reset)
      ensure_plugin "${plugin}"
      reset_verify "${plugin}"
      ;;
    -h|--help|help|"")
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
