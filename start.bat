@REM @echo off
@REM chcp 65001 >nul
@REM echo ========================================
@REM echo    Chem+ Platform Startup Script
@REM echo ========================================
@REM echo.

@REM REM Check if force rebuild is requested
@REM if "%1"=="-b" goto BUILD
@REM if "%1"=="--build" goto BUILD

@REM cd /d "%~dp0backend"

@REM REM Check if jar file exists
@REM if not exist "target\auth-system-0.0.1-SNAPSHOT.jar" (
@REM     echo [!] Jar file not found, starting build...
@REM     goto DO_BUILD
@REM )

@REM goto START

@REM :BUILD
@REM echo [!] Force rebuild mode...
@REM cd /d "%~dp0backend"

@REM :DO_BUILD
@REM echo.
@REM call ./mvnw clean package -DskipTests
@REM if %errorlevel% neq 0 (
@REM     echo [X] Build failed!
@REM     pause
@REM     exit /b 1
@REM )
@REM echo [OK] Build completed!
@REM echo.

@REM :START
@REM echo [1/2] Starting backend service...
@REM start "Backend Server" cmd /k "java -jar target/auth-system-0.0.1-SNAPSHOT.jar"

@REM echo Waiting for backend to start (15 seconds)...
@REM timeout /t 15 /nobreak >nul

@REM echo.
@REM echo [2/2] Starting frontend service...
@REM cd /d "%~dp0frontend"
@REM start "Frontend Server" cmd /k "npm run dev"

@REM echo.
@REM echo ========================================
@REM echo    Services started successfully!
@REM echo ========================================
@REM echo.
@REM echo Backend: http://localhost:8080
@REM echo Frontend: http://localhost:5173
@REM echo.
@REM echo This window will close automatically...
@REM timeout /t 3 /nobreak >nul
@REM exit

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
echo [1/3] Starting backend service...
start "Backend Server" cmd /k "java -jar target/auth-system-0.0.1-SNAPSHOT.jar"

echo Waiting for backend to start (15 seconds)...
timeout /t 15 /nobreak >nul

echo.
echo [2/3] Starting knowledge graph service...
cd /d "%~dp0frontend\my-notes-site"
start "Knowledge Graph Server" cmd /k "python start.py"

echo Waiting for knowledge graph service to start (5 seconds)...
timeout /t 5 /nobreak >nul

echo.
echo [3/3] Starting frontend service...
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