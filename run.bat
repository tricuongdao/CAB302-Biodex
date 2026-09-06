@echo off
rem ============================================================
rem  Biodex - launcher (Windows)
rem  Requirements: JDK 17 or newer only.
rem  Maven is NOT required: the bundled Maven Wrapper (mvnw.cmd)
rem  automatically downloads the right Maven version on first run.
rem ============================================================
setlocal
cd /d "%~dp0"

rem --- Locate Maven: prefer the project wrapper, then PATH ---
set "MVN_CMD="
if exist "%~dp0mvnw.cmd" set "MVN_CMD=%~dp0mvnw.cmd"
if not defined MVN_CMD where mvn >nul 2>nul && set "MVN_CMD=mvn"
if not defined MVN_CMD (
    echo [ERROR] Maven not found and mvnw.cmd wrapper is missing.
    echo         Please re-clone the repository or install Maven 3.8+.
    pause
    exit /b 1
)

rem --- Make sure JAVA_HOME points at a JDK 17+ (required by mvnw) ---
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run
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

:run
echo Starting Biodex...
call "%MVN_CMD%" javafx:run
if errorlevel 1 (
    echo.
    echo [ERROR] Biodex failed to start - see the messages above.
    pause
)
endlocal

