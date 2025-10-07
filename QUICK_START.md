# 🚀 Guía de Inicio Rápido - FTP Java

## Paso 1: Compilar (solo una vez)

### Windows (PowerShell)
```powershell
javac -d bin -cp "lib/commons-net-3.11.1.jar" src\FTP\Client\*.java src\FTP\Server\*.java src\FTP\Util\*.java
```

### Linux/macOS
```bash
javac -d bin -cp "lib/commons-net-3.11.1.jar" src/FTP/Client/*.java src/FTP/Server/*.java src/FTP/Util/*.java
```

---

## Paso 2: Ejecutar el Proyecto

### 🖥️ Servidor (Consola)

**Windows:**
```bash
java -cp "bin;lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer
```

**Linux/macOS:**
```bash
java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer
```

**¿Qué hacer?**
1. Cuando pida el directorio raíz, escribe: `files`
2. El servidor mostrará un banner ASCII
3. Verás logs en tiempo real en la consola

---

### 💻 Cliente (Interfaz Gráfica Retro)

**Windows:**
```bash
.\run-client-gui.bat
```

**Linux/macOS:**
```bash
./run-client-gui.sh
```

**¿Qué hacer?**
1. Se abre la ventana retro ámbar
2. Completar campos de conexión:
   - **Host:** `localhost`
   - **Port:** `21`
   - **User:** `admin`
   - **Pass:** `admin789`
   - **Mode:** `PASSIVE`
3. Clic en **"▶ CONNECT"**
4. ¡Navega con los botones!


---

## 🎮 Botones del Cliente GUI

| Botón | Función |
|-------|---------|
| **↻ REFRESH** | Actualizar lista de archivos |
| **↑ UPLOAD** | Subir archivo desde tu PC |
| **↓ DOWNLOAD** | Descargar archivo seleccionado |
| **✖ DELETE** | Eliminar archivo |
| **+ MKD** | Crear nuevo directorio |
| **- RMD** | Eliminar directorio vacío |
| **✎ RENAME** | Renombrar archivo/directorio |
| **→ CD** | Entrar a directorio |
| **← CDUP** | Volver al directorio padre |
| **? PWD** | Ver directorio actual |

---

## 📋 Usuarios de Prueba

El archivo `files/users/users.txt` debe contener:

```
alice:pass123:BASICO
bob:secret456:INTERMEDIO
admin:admin789:ADMINISTRADOR
```

### Permisos por Perfil:

- **BASICO** 📖
  - LIST, RETR (descargar), CWD, PWD, CDUP

- **INTERMEDIO** ✏️
  - Todo de BASICO +
  - STOR (subir), DELE (eliminar)

- **ADMINISTRADOR** 👑
  - Acceso completo +
  - MKD (crear dir), RMD (eliminar dir), RNFR/RNTO (renombrar)

---

## 🎨 Características Visuales del Cliente GUI

- **Estilo:** Terminal retro ámbar/cobre
- **Tabla de archivos** con tipo, nombre, tamaño, fecha
- **10 botones** de operación
- **Log** con mensajes coloreados
- **Indicador** de directorio actual

---

## ⚡ Comandos Alternativos (Sin Scripts)

### Servidor
```bash
# Windows
java -cp "bin;lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer

# Linux/macOS
java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer
```

### Cliente GUI
```bash
# Windows
java -cp "bin;lib/commons-net-3.11.1.jar" FTP.Client.ClientGUI

# Linux/macOS
java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Client.ClientGUI
```

---

## 🔧 Solución de Problemas

### "Puerto 21 en uso"
En Windows, el puerto 21 puede estar ocupado. Modifica `CONTROL_PORT` en `JavaFtpServer.java` a `2121` o cualquier puerto >1024.

### "No se encuentra lib/commons-net"
Asegúrate de estar en el directorio raíz del proyecto (`java-ftp/`)

### "Archivo users.txt no existe"
Crea la estructura:
```
files/
  └── users/
      └── users.txt
```

### GUI del cliente no aparece en Linux
Asegúrate de tener entorno gráfico (X11 o Wayland) y Java con soporte Swing.

---

## 📞 Ayuda

Para más detalles, consulta el [README.md](README.md) completo.

**By Eduardo Díaz Sánchez © 2025**
