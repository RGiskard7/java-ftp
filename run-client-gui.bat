@echo off
REM Script para ejecutar el cliente FTP con interfaz gráfica retro
REM By Eduardo Díaz Sánchez

echo ==================================
echo   Iniciando Cliente FTP - Modo GUI
echo ==================================
echo.

java -cp "bin;lib/commons-net-3.11.1.jar;lib/jbcrypt-0.4.jar" FTP.Client.ClientGUI

pause
