@echo off
cd /d "%~dp0"
echo Starting OptiSchedule JavaFX...
mvn clean javafx:run
if errorlevel 1 (
  echo.
  echo OptiSchedule could not start. Copy the complete error above and share it for troubleshooting.
  pause
)
