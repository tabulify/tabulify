@ECHO OFF
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

SET SCRIPT_PATH=%~dp0

REM This script call the tabli task defined in the gradle.kts build
REM The properties are passed via argument

if ".%*" == "." (
  @call %SCRIPT_PATH%/../gradlew tabli --project-dir %SCRIPT_PATH% -PcurrentDirectory=%cd%
) ELSE (
	@call %SCRIPT_PATH%/../gradlew tabli --project-dir %SCRIPT_PATH% -PcurrentDirectory=%cd% -Parguments="%*"
);

if "%OS%"=="Windows_NT" endlocal

