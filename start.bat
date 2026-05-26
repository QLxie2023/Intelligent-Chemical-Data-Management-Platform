@echo off
chcp 65001 >nul
echo ========================================
echo    Chem+ Platform Startup Script
echo ========================================
echo.

REM ========== 1. Build Backend ==========
echo [1/6] Building backend (Maven)...
cd /d "%~dp0backend"
call ./mvnw clean package -DskipTests
if %errorlevel% neq 0 (
    echo [X] Backend build failed!
    pause
    exit /b 1
)
echo [OK] Backend build completed!
echo.

REM ========== 2. Install Frontend Dependencies ==========
echo [2/6] Installing frontend dependencies...
cd /d "%~dp0frontend"
if not exist "node_modules" (
    call npm install
    if %errorlevel% neq 0 (
        echo [X] Frontend npm install failed!
        pause
        exit /b 1
    )
) else (
    echo [OK] node_modules already exists, skip install.
)
echo.

REM ========== 3. Install Knowledge Graph Dependencies ==========
echo [3/6] Installing knowledge graph dependencies...
cd /d "%~dp0frontend\my-notes-site"
if not exist "node_modules" (
    call npm install
    if %errorlevel% neq 0 (
        echo [X] Knowledge graph npm install failed!
        pause
        exit /b 1
    )
) else (
    echo [OK] node_modules already exists, skip install.
)
echo.

REM ========== 4. Start Backend ==========
echo [4/6] Starting backend service...
cd /d "%~dp0backend"
start "Backend Server" cmd /k "java -jar target/auth-system-0.0.1-SNAPSHOT.jar"

echo Waiting for backend to start (15 seconds)...
timeout /t 15 /nobreak >nul

echo.
echo [5/6] Starting knowledge graph service...
cd /d "%~dp0frontend\my-notes-site"
start "Knowledge Graph Server" cmd /k "python start.py"

echo Waiting for knowledge graph service to start (5 seconds)...
timeout /t 5 /nobreak >nul

echo.
echo [6/6] Starting frontend service...
cd /d "%~dp0frontend"
start "Frontend Server" cmd /k "npm run dev"

echo.
echo ========================================
echo    Services started successfully!
echo ========================================
echo.
echo Backend: http://localhost:8080
echo Knowledge Graph: http://localhost:8000
echo Frontend: http://localhost:5173
echo.
echo This window will close automatically...
timeout /t 3 /nobreak >nul
exit