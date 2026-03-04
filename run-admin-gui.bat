@echo off
REM Script para ejecutar el panel de administración de usuarios FTP (SQLite)
REM By Eduardo Díaz Sánchez
REM Requiere: bin\ compilado, lib\jbcrypt-0.4.jar, lib\sqlite-jdbc-3.44.1.0.jar

echo ==================================
echo   Panel Admin FTP - Usuarios
echo ==================================
echo.

set CP=bin;lib/commons-net-3.11.1.jar;lib/jbcrypt-0.4.jar;lib/sqlite-jdbc-3.44.1.0.jar
java -cp "%CP%" FTP.Admin.AdminGUI

pause
