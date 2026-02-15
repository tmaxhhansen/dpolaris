@echo off
setlocal
echo Starting dPolaris (backend + java)...

REM 0) Kill anything using port 8420 (aggressive takeover)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8420 .*LISTENING"') do (
  echo Killing PID on 8420: %%p
  taskkill /PID %%p /F >nul 2>&1
)

REM 1) Start backend via dPolaris_ops (recommended)
start "dPolaris_ops up" cmd /k ^
  "cd /d C:\my-git\dPolaris_ops && .\.venv\Scripts\python.exe -m ops.main up"

REM 2) Wait a bit for backend to come up
timeout /t 5 >nul

REM 3) Start Java control center
start "dpolaris java" cmd /k ^
  "cd /d C:\my-git\dpolaris && .\gradlew.bat --no-daemon run"

echo Done. Two windows opened.
endlocal
