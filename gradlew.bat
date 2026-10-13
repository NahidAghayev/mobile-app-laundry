@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\gradle-bootstrap.ps1" %*
exit /b %ERRORLEVEL%
