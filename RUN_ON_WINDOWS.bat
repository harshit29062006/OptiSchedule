@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME if exist "C:\Program Files\Java\jdk-21.0.12\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
if exist "C:\Program Files\Apache\Maven\apache-maven-3.10.1\bin\mvn.cmd" set "PATH=C:\Program Files\Apache\Maven\apache-maven-3.10.1\bin;%PATH%"
echo Starting OptiSchedule JavaFX...
call mvn javafx:run
if errorlevel 1 (
  echo.
  echo OptiSchedule could not start. Copy the complete error above and share it for troubleshooting.
  pause
)
