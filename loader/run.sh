#!/bin/sh
# ============================================================
#  Biodex - launcher (macOS / Linux)
#  Requirements: JDK 17 or newer only.
#  Maven is NOT required: the bundled Maven Wrapper
#  (loader/mvnw) downloads it automatically on first run.
#  Usage: from anywhere -  loader/run.sh
# ============================================================
set -u
LOADER_DIR="$(cd "$(dirname "$0")" && pwd)" || exit 1

# --- Run Maven from the project root so the app database (biodex.db) stays there ---
cd "$LOADER_DIR/.." || exit 1

# --- Check Java, the only real prerequisite ---
if ! command -v java >/dev/null 2>&1 && [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
    echo "[ERROR] Java was not found on this computer."
    echo "        Install JDK 17 or newer, for example:"
    echo "          macOS:  brew install --cask temurin@17"
    echo "          Linux:  sudo apt install openjdk-17-jdk"
    echo "        then open a new terminal and run this script again."
    exit 1
fi

# --- Locate Maven: prefer the bundled wrapper, then PATH ---
if [ -x "$LOADER_DIR/mvnw" ]; then
    MVN_CMD="$LOADER_DIR/mvnw"
elif command -v mvn >/dev/null 2>&1; then
    MVN_CMD=mvn
else
    echo "[ERROR] Maven not found and loader/mvnw wrapper is missing."
    echo "        Please re-clone the repository or install Maven 3.8+."
    exit 1
fi

echo "Starting Biodex..."
exec "$MVN_CMD" javafx:run "$@"
