@echo off
REM ========== 配置区域 ==========
set appName=Saafin_online.jar
set jarPath=%USERPROFILE%\Desktop\wwwroot\%appName%
set springProfile=dev
REM =============================

REM 查找Java进程
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo csv ^| findstr /i "%appName%"') do (
    set PID=%%i
    goto :found
)

REM 如果没有找到进程
echo Application is already stopped
goto :start

:found
echo kill %PID%
taskkill /pid %PID% /f
if errorlevel 1 (
    echo Error: Failed to kill process %PID%
    pause
    exit /b 1
)

:start
REM 检查jar文件是否存在
if not exist "%jarPath%" (
    echo Error: Jar file not found at %jarPath%
    echo Please check the file path and ensure the jar file exists
    pause
    exit /b 1
)

REM 启动应用
echo Starting application...
echo Using jar file: %jarPath%
echo Using profile: %springProfile%
java -jar "%jarPath%" --spring.profiles.active=%springProfile%
if errorlevel 1 (
    echo Error: Failed to start application
    echo Please check the error message above
    pause
    exit /b 1
) else (
    echo Application started successfully
    pause
)