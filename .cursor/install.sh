#!/usr/bin/env bash
# Idempotent Cursor Cloud Agent bootstrap for Bow Tune.
# Installs JDK 17 + Android SDK (API 35) and syncs Gradle. No daemons.
set -euo pipefail

log() { printf '[bowtune-install] %s\n' "$*"; }

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

export DEBIAN_FRONTEND=noninteractive
export ANDROID_HOME="${ANDROID_HOME:-${HOME}/Android/Sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"

# Official command-line tools package (Linux). See
# https://developer.android.com/studio#command-line-tools-only
CMDLINE_TOOLS_URL="${CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip}"
SDK_PACKAGES=(
  "platform-tools"
  "platforms;android-35"
  "build-tools;35.0.0"
)
# Same source CI uses when gradle-wrapper.jar is omitted from the tree.
WRAPPER_JAR_URL="${WRAPPER_JAR_URL:-https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar}"

need_apt=()
if ! dpkg -s openjdk-17-jdk-headless >/dev/null 2>&1; then
  need_apt+=(openjdk-17-jdk-headless)
fi
if ! command -v unzip >/dev/null 2>&1; then
  need_apt+=(unzip)
fi
if ((${#need_apt[@]})); then
  log "Installing apt packages: ${need_apt[*]}"
  sudo apt-get update -y
  sudo apt-get install -y --no-install-recommends "${need_apt[@]}"
else
  log "JDK 17 and unzip already installed"
fi

find_java17_home() {
  local candidate
  for candidate in \
    ${JAVA_17_HOME:-} \
    /usr/lib/jvm/java-17-openjdk-amd64 \
    /usr/lib/jvm/java-17-openjdk-arm64 \
    /usr/lib/jvm/java-17-openjdk-*; do
    if [ -n "${candidate}" ] && [ -x "${candidate}/bin/java" ] \
      && "${candidate}/bin/java" -version 2>&1 | grep -q 'version "17'; then
      printf '%s\n' "${candidate}"
      return 0
    fi
  done
  return 1
}

JAVA_HOME="$(find_java17_home)" || {
  log "ERROR: JDK 17 is required (compile/target JVM 17) but was not found"
  exit 1
}
export JAVA_HOME
export PATH="${JAVA_HOME}/bin:${PATH}"
log "Using JAVA_HOME=${JAVA_HOME} ($("${JAVA_HOME}/bin/java" -version 2>&1 | head -n 1))"

mkdir -p "${ANDROID_HOME}"
SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "${SDKMANAGER}" ]; then
  log "Installing Android command-line tools"
  tmp="$(mktemp -d)"
  cleanup_tmp() { rm -rf "${tmp}"; }
  trap cleanup_tmp EXIT
  curl -fsSL "${CMDLINE_TOOLS_URL}" -o "${tmp}/cmdline-tools.zip"
  unzip -q "${tmp}/cmdline-tools.zip" -d "${tmp}"
  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  rm -rf "${ANDROID_HOME}/cmdline-tools/latest"
  # Zip root is "cmdline-tools/"; sdkmanager expects cmdline-tools/<version>/bin.
  mv "${tmp}/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  cleanup_tmp
  trap - EXIT
  SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
else
  log "Android command-line tools already present"
fi

if [ ! -x "${SDKMANAGER}" ]; then
  log "ERROR: sdkmanager missing at ${SDKMANAGER}"
  exit 1
fi

log "Accepting Android SDK licenses"
# `yes` gets SIGPIPE when sdkmanager closes stdin; do not fail the script.
set +o pipefail
yes | "${SDKMANAGER}" --sdk_root="${ANDROID_HOME}" --licenses >/dev/null
set -o pipefail

log "Installing SDK packages: ${SDK_PACKAGES[*]}"
"${SDKMANAGER}" --sdk_root="${ANDROID_HOME}" --install "${SDK_PACKAGES[@]}"

# Machine-local only (gitignored). Lets Gradle find the SDK without secrets.
printf 'sdk.dir=%s\n' "${ANDROID_HOME}" > "${ROOT}/local.properties"
log "Wrote ${ROOT}/local.properties (sdk.dir=${ANDROID_HOME})"

# Persist JDK 17 for later Gradle invocations without mutating shell profiles.
mkdir -p "${HOME}/.gradle"
gradle_props="${HOME}/.gradle/gradle.properties"
if [ -f "${gradle_props}" ]; then
  grep -v '^org.gradle.java.home=' "${gradle_props}" > "${gradle_props}.tmp" || true
  mv "${gradle_props}.tmp" "${gradle_props}"
fi
printf 'org.gradle.java.home=%s\n' "${JAVA_HOME}" >> "${gradle_props}"

if [ ! -f "${ROOT}/gradle/wrapper/gradle-wrapper.jar" ]; then
  log "Downloading gradle-wrapper.jar for Gradle 8.9"
  mkdir -p "${ROOT}/gradle/wrapper"
  curl -fsSL -o "${ROOT}/gradle/wrapper/gradle-wrapper.jar" "${WRAPPER_JAR_URL}"
else
  log "gradle-wrapper.jar already present"
fi
chmod +x "${ROOT}/gradlew"

log "Syncing Gradle (configure project; no assemble)"
if ! "${ROOT}/gradlew" --no-daemon --stacktrace :app:tasks > /tmp/bowtune-gradle-tasks.log 2>&1; then
  log "Gradle configure failed; last 80 lines:"
  tail -n 80 /tmp/bowtune-gradle-tasks.log || true
  exit 1
fi
tail -n 15 /tmp/bowtune-gradle-tasks.log
log "Install complete. Build with: ./gradlew assembleDebug"
