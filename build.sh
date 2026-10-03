#!/bin/sh
# ============================================================
#  Biodex - build script (macOS / Linux)
#  Runs the full Maven build from a clean state: compiles the
#  sources, runs the whole test suite and packages the app jar.
#  Requirements: JDK 17 or newer only. Maven is not required;
#  the bundled wrapper (loader/mvnw) downloads it on first use.
#  Usage: from anywhere -  ./build.sh
#  Output: target/biodex-1.0-SNAPSHOT.jar and a test summary.
# ============================================================
set -u
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)" || exit 1
cd "$ROOT_DIR" || exit 1

# --- Check Java, the only real prerequisite ---
if ! command -v java >/dev/null 2>&1 && [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
    echo "[ERROR] Java was not found on this computer."
    echo "        Install JDK 17 or newer (https://adoptium.net),"
    echo "        then open a new terminal and run this script again."
    exit 1
fi

# --- Locate Maven: prefer the bundled wrapper, then PATH ---
if [ -x "$ROOT_DIR/loader/mvnw" ]; then
    MVN_CMD="$ROOT_DIR/loader/mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD=mvn
else
    echo "[ERROR] Maven not found and loader/mvnw wrapper is missing."
    echo "        Please re-clone the repository or install Maven 3.8+."
    exit 1
fi

echo "Building Biodex (clean compile + test + package)..."
if ! "$MVN_CMD" -B clean verify; then
    echo ""
    echo "[ERROR] Build FAILED - see the Maven output above."
    exit 1
fi

echo ""
echo "Build SUCCESSFUL."
echo "  Application jar : target/biodex-1.0-SNAPSHOT.jar"
echo "  Run the app     : loader/run.sh"
