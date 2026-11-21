@echo off
title Habit Tracker Launcher
echo ===================================================
echo           Starting Habit Tracker App
echo ===================================================

set "PROJECT_DIR=%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
set "MAVEN_BIN=%PROJECT_DIR%tools\maven\bin\mvn.cmd"

echo [1/2] Launching Backend (Spring Boot on port 8080)...
start "Habit Tracker - Backend (Port 8080)" cmd /k "cd /d %PROJECT_DIR%backend && "%MAVEN_BIN%" spring-boot:run"

echo [2/2] Launching Frontend (Vite on port 5173)...
start "Habit Tracker - Frontend (Port 5173)" cmd /k "cd /d %PROJECT_DIR%frontend && npm run dev"

echo.
echo Waiting 5 seconds for dev servers to initialize...
timeout /t 5 /nobreak >nul

echo Opening browser at http://localhost:5173 ...
start http://localhost:5173

echo.
echo Both servers are running! You can now register or log in.
pause
