@echo off
setlocal
cd /d "%~dp0\.."
java -cp out darts.client.Client %*
