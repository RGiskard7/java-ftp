#!/bin/bash
# Script para ejecutar el panel de administración de usuarios FTP (SQLite)
# By Eduardo Díaz Sánchez
# Requiere: bin/ compilado, lib/jbcrypt-0.4.jar, lib/sqlite-jdbc-3.44.1.0.jar
# Opcional: server.properties en el directorio actual (para ftp.users.database)

echo "=================================="
echo "  Panel Admin FTP - Usuarios"
echo "=================================="
echo ""

CP="bin:lib/commons-net-3.11.1.jar:lib/jbcrypt-0.4.jar:lib/sqlite-jdbc-3.44.1.0.jar"
java -cp "$CP" FTP.Admin.AdminGUI
