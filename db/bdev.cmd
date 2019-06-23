@echo off

SET SCRIPT_PATH=%~dp0

powershell -ExecutionPolicy ByPass -Command "& {%SCRIPT_PATH%bdev.ps1 %*}"