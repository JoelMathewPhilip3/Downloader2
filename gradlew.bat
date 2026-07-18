@echo off
setlocal
set DIR=%~dp0
set JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
if not exist "%JAR%" (
  echo Downloading the official Gradle 8.9 wrapper JAR...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%'"
  if errorlevel 1 exit /b 1
)
java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
