@ECHO OFF
SET SCRIPT_PATH=%~dp0

SET RUN_GRADLE_TASK=runTowerHotReload

@if ".%*" == "." (
	@%SCRIPT_PATH%/../gradlew vertxRun --project-dir %SCRIPT_PATH% -PcurrentDirectory=%cd%
) ELSE (
	@%SCRIPT_PATH%/../gradlew vertxRun --project-dir %SCRIPT_PATH% -PcurrentDirectory=%cd% --args="%*"
)
