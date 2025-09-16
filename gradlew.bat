@echo off
setlocal
set DIR=%~dp0
set WRAPPER_DIR=%DIR%gradle\wrapper
if exist "%WRAPPER_DIR%\gradle-wrapper.jar" (
  java -jar "%WRAPPER_DIR%\gradle-wrapper.jar" %*
) else (
  rem Fallback to gradle command if wrapper jar isn't present
  gradle %*
)
endlocal
