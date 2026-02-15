@echo off
setlocal
echo Stopping dPolaris...

REM Kill backend port owner no matter what
for /f "tokens=5" %%p in ('netstat -ano ^| findstr /r /c:":8420 .*LISTENING"') do (
  echo Killing PID on 8420: %%p
  taskkill /PID %%p /F >nul 2>&1
)

REM Also attempt ops down (safe even if already down)
cd /d C:\my-git\dPolaris_ops
.\.venv\Scripts\python.exe -m ops.main down

echo Done.
endlocal
