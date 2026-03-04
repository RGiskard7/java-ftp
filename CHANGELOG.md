# Changelog

Todos los cambios notables del proyecto FTP Java serán documentados aquí.

---

## [1.2.0] - 2025-03-04

### Cambios en el Cliente GUI
- **Panel de conexión restaurado** al diseño original de 4 columnas:
  - Fila 1: HOST + PORT (etiqueta y campo al lado, como en la referencia).
  - Fila 2: USER + PASS.
  - Fila 3: MODE + checkbox «Use FTPS (TLS)».
  - Fila 4: botones CONNECT / DISCONNECT a la derecha.
- El checkbox **Use FTPS (TLS)** ya no se activa/desactiva al pulsar CONNECT (botones y checkbox en filas distintas, sin solapamiento).

### Servidor (correcciones)
- **Directorio raíz y usuarios son pasos independientes:** si `ftp.root.directory` está definido en `server.properties`, el servidor **no pide** el directorio raíz al arrancar. Si `ftp.users.database` está definido, se usa **solo SQLite** y no se muestra aviso de `users.txt`.
- **`checkDirs()` sustituido por `checkFilesDir()`:** solo comprueba que exista la carpeta `files/`; ya no exige ni avisa por la existencia de `users.txt` cuando se usa SQLite.
- **AUTH TLS (FTPS):** la respuesta `234 Proceed with negotiation` se envía **antes** del handshake TLS (conforme a RFC 4217). Añadido `sslSocket.setUseClientMode(false)` para uso en modo servidor.

### Configuración y documentación
- **Rutas en Windows:** en `server.properties` hay que usar **`/`** y no `\` (Java trata `\` como escape en `.properties`). Comentarios y README actualizados con ejemplos.
- **FTPS:** en `server.properties` y README documentados los pasos para habilitar TLS (generación de keystore con `keytool`, propiedades `ftp.tls.enabled`, `ftp.tls.keystore.path`, etc.).
- **README:**
  - Una sola fuente de verdad para **compilar y ejecutar** (sección «Compilación y ejecución»); eliminadas repeticiones en Instalación, Inicio Rápido y Uso.
  - Nueva subsección **Rutas en Windows** y tabla de propiedades principales de `server.properties`.
  - Sección **Habilitar FTPS (TLS)** con comandos `keytool` para Windows y Linux/macOS.
  - Solución de problemas ampliada: rutas en Windows, FTPS no conecta, servidor pide root, error de archivo de usuarios.
  - Classpath unificado a `bin;lib\*` (Windows) y `bin:lib/*` (Linux/macOS) en todo el documento.

### Archivos modificados (resumen)
- `ClientGUI.java` – layout del panel de conexión.
- `JavaFtpServer.java` – flujo de arranque (root + user store separados), `checkFilesDir()`.
- `FtpClientHandler.java` – AUTH TLS (orden 234 + handshake, `setUseClientMode(false)`).
- `server.properties` – comentarios sobre rutas y FTPS.
- `README.md` – deduplicación, configuración, Windows, FTPS.
- `CHANGELOG.md` – esta entrada.

---

## [1.1.0] - 2025-08-07

### Corrección crítica
- Archivos binarios (PDFs, ZIPs, imágenes) se descargaban corruptos porque el cliente no enviaba el comando `TYPE I`. Ambos clientes (GUI y consola) configuran ahora `BINARY_FILE_TYPE` tras el login. Archivos modificados: `ClientGUI.java`, `JavaFtpClient.java`.

### Nuevas características

#### ClientGUI - Mejoras de UX
1. **Barras de Progreso para Transferencias**
   - Diálogos modales con barras de progreso en tiempo real
   - Implementado con `CopyStreamListener` de Apache Commons Net
   - Muestra porcentaje de transferencia para uploads y downloads
   - Transferencias en hilos separados para no bloquear la UI
   - **Archivos**: `ClientGUI.java:574-645` (upload), `ClientGUI.java:647-738` (download)

2. **Selección Múltiple de Archivos**
   - Activado `ListSelectionModel.MULTIPLE_INTERVAL_SELECTION`
   - Permite seleccionar múltiples archivos con Ctrl+Click o Shift+Click
   - **Archivo**: `ClientGUI.java:249`

3. **Doble Click para Cambiar Directorio**
   - Doble click en carpetas cambia directorio automáticamente
   - Mejora navegación estilo explorador de archivos
   - **Archivo**: `ClientGUI.java:252-274`

#### Servidor - Sistema de Configuración
1. **Archivo de Configuración Externo**
   - Nuevo archivo `server.properties` para configurar sin recompilar
   - Parámetros configurables:
     - `ftp.control.port` - Puerto del servidor (default: 21)
     - `ftp.root.directory` - Directorio raíz
     - `ftp.users.file` - Ruta al archivo de usuarios
     - `ftp.max.connections` - Máximo de conexiones concurrentes
     - `ftp.session.timeout` - Timeout de sesión
     - `ftp.verbose.logging` - Habilitar logs detallados
   - **Archivos nuevos**:
     - `server.properties`
     - `src/FTP/Server/ServerConfig.java`
   - **Archivos modificados**: `JavaFtpServer.java:102-131`

2. **Sistema de Logging a Archivo**
   - Logs automáticos en `logs/ftp-server.log`
   - Registro de eventos con timestamp
   - Métodos especializados: `info()`, `error()`, `warning()`, `logAuth()`, `logCommand()`, etc.
   - **Archivos nuevos**:
     - `src/FTP/Util/FileLogger.java`
   - **Archivos modificados**:
     - `JavaFtpServer.java:114-115, 141, 145, 151, 154`
     - `FtpClientHandler.java:271, 274`

#### Soporte de Protocolo
1. **Comando TYPE Implementado**
   - Soporte para `TYPE A` (ASCII) y `TYPE I` (Binary)
   - Necesario para cumplir estándar RFC 959
   - **Archivo**: `FtpClientHandler.java:285-300`

### Correcciones
1. **Typo en JavaFtpClient**
   - Corregido `"PASIVE"` → `"PASSIVE"`
   - **Archivo**: `JavaFtpClient.java:241`

2. **Nombres de archivo con `&` rechazados**
   - **Problema**: Archivos como `D&D.zip` eran rechazados por validación de seguridad
   - **Solución**: Carácter `&` ahora permitido (solo peligroso en shells, no en filesystems)
   - **Archivo**: `ServerFunctions.java:74-77`

3. **Nombres con acentos/ñ corrompidos**
   - **Problema**: `CV Eduardo Díaz Sánchez.pdf` → se recibía como `CV Eduardo D?az S?nchez.pdf`
   - **Causa**: Encoding por defecto (ISO-8859-1) en lugar de UTF-8
   - **Solución**: UTF-8 configurado explícitamente en servidor y clientes
   - **Archivos modificados**:
     - `FtpClientHandler.java:102` - InputStreamReader con UTF-8
     - `ClientGUI.java:503` - setControlEncoding("UTF-8")
     - `JavaFtpClient.java:55` - setControlEncoding("UTF-8")

4. **Nombres de archivo con espacios truncados**
   - **Problema**: `Eduardo Diaz Sanchez.pdf` → se subía como `Eduardo.pdf`
   - **Causa**: Parsing de comandos FTP con `split(" ")` sin límite
   - **Solución**: Cambio a `split(" ", 2)` para preservar todo después del comando
   - **Archivo**: `FtpClientHandler.java:112`

### Archivos nuevos
```
server.properties                     # Configuración del servidor
src/FTP/Server/ServerConfig.java     # Clase de configuración
src/FTP/Util/FileLogger.java         # Sistema de logging
.gitignore                            # Exclusiones de git
CHANGELOG.md                          # Este archivo
```

### Mejoras técnicas
- Importación selectiva en ClientGUI para evitar conflictos de nombres
- Transferencias en hilos separados usando `SwingWorker` pattern
- Manejo de errores mejorado con try-catch-finally consistente
- Logs estructurados con niveles INFO/ERROR/WARNING

---

## [1.0.0] - Versión Inicial

### Características iniciales
- Servidor FTP multihilo con ExecutorService
- Sistema RBAC con 3 perfiles (BASICO, INTERMEDIO, ADMINISTRADOR)
- Soporte modos ACTIVE y PASSIVE
- Cliente de consola interactivo
- Cliente GUI con estética retro ámbar
- Comandos FTP: USER, PASS, SYST, PASV, PORT, LIST, STOR, RETR, DELE, MKD, RMD, RNFR, RNTO, CWD, CDUP, PWD, QUIT
- Validaciones de seguridad contra path traversal
- Javadoc completo

### Bugs conocidos (corregidos en v1.1.0)
- Archivos binarios se corrompían en transferencia (modo ASCII por defecto).
- Typo "PASIVE" en código.
- Comando TYPE no implementado.

---

## Formato del Changelog

Este changelog sigue los principios de [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).

### Categorías
- **Added**: Nuevas características.
- **Changed**: Cambios en funcionalidad existente.
- **Deprecated**: Características marcadas como obsoletas.
- **Removed**: Características eliminadas.
- **Fixed**: Correcciones de bugs.
- **Security**: Correcciones de seguridad.
