@echo off
setlocal

set ROOT_DIR=%~dp0
set SRC_DIR=%ROOT_DIR%src\main\java
set OUT_DIR=%ROOT_DIR%build\out

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

for /R "%OUT_DIR%" %%f in (*.class) do del /Q "%%f"

dir /s /b "%SRC_DIR%\*.java" > "%ROOT_DIR%build\java_sources.txt"
javac -encoding UTF-8 -d "%OUT_DIR%" @"%ROOT_DIR%build\java_sources.txt"
if errorlevel 1 exit /b 1

java -cp "%OUT_DIR%" com.dpolaris.javaapp.DPolarisJavaApp

