@echo off
rem ============================================================
rem  Biodex - build script (Windows)
rem  Runs the full Maven build from a clean state: compiles the
rem  sources, runs the whole test suite and packages the app jar.
rem  Requirements: JDK 17 or newer only. Maven is not required;
rem  the bundled wrapper (loader\mvnw.cmd) downloads it on first use.
rem  Usage: from anywhere -  build.bat   (or double-click it)
rem  Output: target\biodex-1.0-SNAPSHOT.jar and a test summary.
rem ============================================================
setlocal
set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%" || exit /b 1

rem --- Make sure JAVA_HOME points at a JDK 17+ (required by Maven) ---
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto findmvn
for %%i in (java.exe) do set "JAVA_BIN=%%~$PATH:i"
if not defined JAVA_BIN (
    echo [ERROR] Java was not found on this computer.
    echo         Install JDK 17 or newer from https://adoptium.net
    echo         then open a new terminal and run this script again.
    pause
    exit /b 1
)
for %%i in ("%JAVA_BIN%\..\..") do set "JAVA_HOME=%%~fi"
echo Using JAVA_HOME=%JAVA_HOME%

:findmvn
rem --- Locate Maven: prefer the bundled wrapper, then PATH ---
set "MVN_CMD="
if exist "%ROOT_DIR%loader\mvnw.cmd" set "MVN_CMD=%ROOT_DIR%loader\mvnw.cmd"
if not defined MVN_CMD where mvn >nul 2>nul && set "MVN_CMD=mvn"
if not defined MVN_CMD (
    echo [ERROR] Maven not found and loader\mvnw.cmd wrapper is missing.
    echo         Please re-clone the repository or install Maven 3.8+.
    pause
    exit /b 1
)

echo Building Biodex (clean compile + test + package)...
call "%MVN_CMD%" -B clean verify
if errorlevel 1 (
    echo.
    echo [ERROR] Build FAILED - see the Maven output above.
    pause
    exit /b 1
)

echo.
echo Build SUCCESSFUL.
echo   Application jar : target\biodex-1.0-SNAPSHOT.jar
echo   Run the app     : loader\run.bat
endlocal
