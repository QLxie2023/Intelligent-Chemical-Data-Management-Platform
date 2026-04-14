@echo off
chcp 65001 >nul
echo ========================================
echo    Chem+ Platform Startup Script
echo ========================================
echo.

REM Check if force rebuild is requested
if "%1"=="-b" goto BUILD
if "%1"=="--build" goto BUILD

cd /d "%~dp0backend"

REM Check if jar file exists
if not exist "target\auth-system-0.0.1-SNAPSHOT.jar" (
    echo [!] Jar file not found, starting build...
    goto DO_BUILD
)

goto START

:BUILD
echo [!] Force rebuild mode...
cd /d "%~dp0backend"

:DO_BUILD
echo.
call ./mvnw clean package -DskipTests
if %errorlevel% neq 0 (
    echo [X] Build failed!
    pause
    exit /b 1
)
echo [OK] Build completed!
echo.

:START
echo [1/2] Starting backend service...
start "Backend Server" cmd /k "java -jar target/auth-system-0.0.1-SNAPSHOT.jar"

echo Waiting for backend to start (15 seconds)...
timeout /t 15 /nobreak >nul

echo.
echo [2/2] Starting frontend service...
cd /d "%~dp0frontend"
start "Frontend Server" cmd /k "npm run dev"

echo.
echo ========================================
echo    Services started successfully!
echo ========================================
echo.
echo Backend: http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo This window will close automatically...
timeout /t 3 /nobreak >nul
exit
