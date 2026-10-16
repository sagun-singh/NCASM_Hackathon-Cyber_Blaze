@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot
set ANDROID_HOME=C:\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%
set LOGFILE=C:\Users\ANKITK~1\AppData\Local\Temp\build_out.log
cd /d C:\Users\ANKITK~1\BOB~1\PLAYGR~1
echo [START] assembleDebug > "%LOGFILE%"
call gradlew.bat assembleDebug --no-daemon >> "%LOGFILE%" 2>&1
if %ERRORLEVEL%==0 (
    echo BUILD_SUCCESS >> "%LOGFILE%"
) else (
    echo BUILD_FAILED errorlevel=%ERRORLEVEL% >> "%LOGFILE%"
)
echo [END] >> "%LOGFILE%"
