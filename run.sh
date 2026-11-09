#!/usr/bin/env bash
# Dagger Ridge build/run helper.
#
#   ./run.sh                 desktop client (same as ./run.sh client)
#   ./run.sh build           core + desktop + debug APK
#   ./run.sh test            headless JUnit tests
#   ./run.sh android         debug APK
#   ./run.sh install         install the debug APK on a connected device
#   ./run.sh package         self-contained desktop app image zip
#   ./run.sh fatjar          standalone executable jar
#   ./run.sh smoke           headless desktop smoke test (needs xvfb)
#   ./run.sh clean
#
# Override detection with JAVA_HOME and/or ANDROID_HOME if your tools live
# somewhere unusual.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

usage() {
    sed -n '2,15p' "$0" | sed 's/^# \{0,1\}//'
}

java_major() {
    "$1/bin/java" -version 2>&1 | awk -F'"' '/version/ {print $2}' \
        | awk -F. '{ if ($1 == "1") print $2; else print $1 }'
}

find_jdk() {
    local candidates=()
    [ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
    [ -x /usr/libexec/java_home ] && candidates+=("$(/usr/libexec/java_home -v 17 2>/dev/null || true)")
    candidates+=(
        /usr/lib/jvm/temurin-21
        /usr/lib/jvm/temurin-17
        /usr/lib/jvm/java-21-openjdk-amd64
        /usr/lib/jvm/java-17-openjdk-amd64
        /opt/java/temurin-21
        /opt/java/openjdk
        "$HOME/.jdks/temurin-21"
        "$HOME/.jdks/temurin-17"
    )
    local candidate
    for candidate in "${candidates[@]}"; do
        [ -n "$candidate" ] || continue
        if [ -x "$candidate/bin/java" ] && [ "$(java_major "$candidate")" -ge 17 ]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

if JDK="$(find_jdk)"; then
    export JAVA_HOME="$JDK"
    export PATH="$JAVA_HOME/bin:$PATH"
else
    echo "warning: no JDK 17+ found. Android builds and jpackage need JDK 17+." >&2
    echo "         Set JAVA_HOME to a JDK 17 or 21 installation." >&2
fi

find_android_sdk() {
    local candidates=()
    [ -n "${ANDROID_HOME:-}" ] && candidates+=("$ANDROID_HOME")
    [ -n "${ANDROID_SDK_ROOT:-}" ] && candidates+=("$ANDROID_SDK_ROOT")
    if [ -f "$ROOT/local.properties" ]; then
        local from_props
        from_props="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | head -1)"
        [ -n "$from_props" ] && candidates+=("$from_props")
    fi
    candidates+=(
        "$HOME/Android/Sdk"
        "$HOME/Library/Android/sdk"
        /opt/android-sdk
        /usr/local/android-sdk
        /tmp/android-sdk
    )
    local candidate
    for candidate in "${candidates[@]}"; do
        if [ -n "$candidate" ] && [ -d "$candidate/platforms" ]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

if SDK="$(find_android_sdk)"; then
    export ANDROID_HOME="$SDK"
    export ANDROID_SDK_ROOT="$SDK"
    export PATH="$SDK/platform-tools:$SDK/cmdline-tools/latest/bin:$PATH"
    if [ ! -f "$ROOT/local.properties" ] || ! grep -qxF "sdk.dir=$SDK" "$ROOT/local.properties"; then
        printf 'sdk.dir=%s\n' "$SDK" > "$ROOT/local.properties"
    fi
    echo "JDK:  $JAVA_HOME"
    echo "SDK:  $ANDROID_HOME"
else
    echo "warning: Android SDK not found. Set ANDROID_HOME or install the command-line tools." >&2
fi

COMMAND="${1:-client}"
shift || true
GRADLE_ARGS=("$@")

case "$COMMAND" in
    client|run|desktop)
        exec ./gradlew :lwjgl3:run "${GRADLE_ARGS[@]}"
        ;;
    build)
        exec ./gradlew :core:build :lwjgl3:build :android:assembleDebug "${GRADLE_ARGS[@]}"
        ;;
    test)
        exec ./gradlew :core:test "${GRADLE_ARGS[@]}"
        ;;
    android|apk)
        exec ./gradlew :android:assembleDebug "${GRADLE_ARGS[@]}"
        ;;
    install)
        exec ./gradlew :android:installDebug "${GRADLE_ARGS[@]}"
        ;;
    package|desktop-package)
        exec ./gradlew :lwjgl3:desktopZip "${GRADLE_ARGS[@]}"
        ;;
    fatjar|jar)
        exec ./gradlew :lwjgl3:shadowJar "${GRADLE_ARGS[@]}"
        ;;
    smoke)
        exec xvfb-run -a -s "-screen 0 1440x810x24" \
            ./gradlew :lwjgl3:run --args="--smoke-test" "${GRADLE_ARGS[@]}"
        ;;
    clean)
        exec ./gradlew clean "${GRADLE_ARGS[@]}"
        ;;
    help|-h|--help)
        usage
        ;;
    *)
        echo "unknown command: $COMMAND" >&2
        usage >&2
        exit 2
        ;;
esac
