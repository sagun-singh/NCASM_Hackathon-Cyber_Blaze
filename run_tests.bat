@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot
set ANDROID_HOME=C:\android-sdk
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\ANKIT KUMAR\.bob\playground"
echo Starting test...
call gradlew.bat test --no-daemon
if %ERRORLEVEL%==0 (
    echo TEST_SUCCESS
) else (
    echo TEST_FAILED with code %ERRORLEVEL%
)
