@echo off

@REM The script path to reference the included JRE java file
SET SCRIPT_PATH=%~dp0

:APPEND_CLASSPATH
SET CLASSPATH="%SCRIPT_PATH%\lib\*"
::@echo %CLASSPATH%;

REM Start the application
REM The java system property BCLI_APP_HOME send the app home (used to find the default config file)
%SCRIPT_PATH%\jre\bin\java -classpath %CLASSPATH% -DBCLI_APP_HOME=%SCRIPT_PATH% ${Main.Class} %*
SET EXIT_CODE=%ERRORLEVEL%

REM Enabling colors
REM enable ANSI console color for Windows 10
REM https://github.com/Microsoft/WSL/issues/1173
set KEY_NAME=HKEY_CURRENT_USER\Console
set VALUE_NAME=VirtualTerminalLevel
set VALUE_VALUE=
SETLOCAL ENABLEEXTENSIONS
REM The ^ in the command of the for loop is an escape character
FOR /F "usebackq tokens=1-3" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul ^| find "%VALUE_NAME%"`) DO (
    set VALUE_VALUE=%%C
)
if not defined VALUE_VALUE (
  echo The console is not color aware - The key %KEY_NAME%\%VALUE_NAME% was not found
  echo Adding the key in the registry
  REG ADD %KEY_NAME% /v %VALUE_NAME% /t REG_DWORD /d 1
  echo The colors were enabled, you need to restart your console to see colors on Windows 10
)

REM exit with the exit code of the Java run
exit /B %EXIT_CODE%