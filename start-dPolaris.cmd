@echo off
setlocal
echo Starting dPolaris (backend + java)...

REM 1) Start backend in a new window
start "dpolaris_ai server" cmd /k ^
  "cd /d C:\my-git\dpolaris_ai && set LLM_PROVIDER=none && .\.venv\Scripts\python.exe -m cli.main server --host 127.0.0.1 --port 8420"

REM 2) (Optional) wait a bit for backend to come up
timeout /t 3 >nul

REM 3) Start Java control center in a new window
start "dpolaris java" cmd /k ^
  "cd /d C:\my-git\dpolaris && gradlew.bat --no-daemon run"

echo Done. Two windows opened.
endlocal
