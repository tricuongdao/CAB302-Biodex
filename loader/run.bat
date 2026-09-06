@echo off
rem ============================================================
rem  Biodex - launcher (Windows)
rem  Requirements: JDK 17 or newer only.
rem  Maven is NOT required: the bundled Maven Wrapper
rem  (loader\mvnw.cmd) downloads it automatically on first run.
rem  Usage: from anywhere -  loader\run.bat
rem ============================================================
setlocal
set "LOADER_DIR=%~dp0"

rem --- Run Maven from the project root so the app database (biodex.db) stays there ---
cd /d "%LOADER_DIR%.." || exit /b 1

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
if exist "%LOADER_DIR%mvnw.cmd" set "MVN_CMD=%LOADER_DIR%mvnw.cmd"
if not defined MVN_CMD where mvn >nul 2>nul && set "MVN_CMD=mvn"
if not defined MVN_CMD (
    echo [ERROR] Maven not found and loader\mvnw.cmd wrapper is missing.
    echo         Please re-clone the repository or install Maven 3.8+.
    pause
    exit /b 1
)

echo Starting Biodex...
call "%MVN_CMD%" javafx:run
if errorlevel 1 (
    echo.
    echo [ERROR] Biodex failed to start - see the messages above.
    pause
)
endlocal
