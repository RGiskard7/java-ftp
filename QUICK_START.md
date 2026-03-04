# Guía de Inicio Rápido — Java FTP

Pasos mínimos para compilar, configurar y ejecutar servidor y cliente. Para opciones detalladas (Maven, panel Admin, FTPS, solución de problemas), consulta el [README.md](README.md).

---

## Requisitos

- **JDK 8+** (`java -version`, `javac -version`)
- Estar en la raíz del proyecto (`java-ftp/`)

**Classpath:** En Windows usa `;` (ej. `bin;lib\*`). En Linux/macOS usa `:` (ej. `bin:lib/*`).

---

## 1. Compilar (una vez)

### Windows (PowerShell)

```powershell
if (-not (Test-Path bin)) { New-Item -ItemType Directory -Path bin }
javac -d bin -cp "lib\*" -encoding UTF-8 src\FTP\Client\*.java src\FTP\Server\*.java src\FTP\Util\*.java src\FTP\Admin\*.java
```

### Linux/macOS

```bash
mkdir -p bin
javac -d bin -cp "lib/*" -encoding UTF-8 src/FTP/Client/*.java src/FTP/Server/*.java src/FTP/Util/*.java src/FTP/Admin/*.java
```

Comprueba que exista `bin/FTP/Server/JavaFtpServer.class`.

---

## 2. Configurar usuarios y servidor

Elige **una** de estas dos opciones.

### Opción A — SQLite (recomendado)

1. Crea la carpeta `files` si no existe: `mkdir files` (o `mkdir -Force files` en PowerShell).
2. Ejecuta el panel Admin:
   - **Windows:** `java -cp "bin;lib\*" FTP.Admin.AdminGUI`
   - **Linux/macOS:** `java -cp "bin:lib/*" FTP.Admin.AdminGUI`
3. En el panel: ruta `files/ftp_users.db` → **Cargar / Abrir** (se crea la base y la tabla).
4. **Añadir usuario:** nombre `admin`, contraseña la que quieras, perfil **ADMINISTRADOR**.
5. En `server.properties` (raíz del proyecto) pon:
   - `ftp.root.directory=files`
   - `ftp.users.database=files/ftp_users.db`  
   **En Windows** usa siempre `/` en las rutas, nunca `\`.

### Opción B — Fichero TXT

1. Crea el primer usuario (el fichero se crea solo):
   - **Windows:** `java -cp "bin;lib\*" FTP.Server.PasswordTool adduser admin tuPassword ADMINISTRADOR files/users/users.txt`
   - **Linux/macOS:** `java -cp "bin:lib/*" FTP.Server.PasswordTool adduser admin tuPassword ADMINISTRADOR files/users/users.txt`
2. En `server.properties` pon `ftp.root.directory=files` y deja `ftp.users.database=` vacío.

Con esto el servidor arrancará sin pedir el directorio raíz.

---

## 3. Arrancar el servidor

**Windows:**

```powershell
java -cp "bin;lib\*" FTP.Server.JavaFtpServer
```

**Linux/macOS** (puerto 21 puede requerir `sudo`):

```bash
java -cp "bin:lib/*" FTP.Server.JavaFtpServer
```

Deberías ver el banner ASCII y, si configuraste `server.properties`, el mensaje de directorio raíz y usuarios sin preguntas. Mantén esta terminal abierta.

---

## 4. Arrancar el cliente (GUI)

En **otra** terminal:

**Windows:**

```powershell
java -cp "bin;lib\*" FTP.Client.ClientGUI
```

**Linux/macOS:**

```bash
java -cp "bin:lib/*" FTP.Client.ClientGUI
```

O usa los scripts: `run-client-gui.bat` (Windows) / `./run-client-gui.sh` (Linux/macOS).

En la ventana del cliente:

| Campo      | Valor                          |
|-----------|---------------------------------|
| **HOST**  | `localhost`                     |
| **PORT**  | `21`                            |
| **USER**  | `admin`                         |
| **PASS**  | La que definiste en el paso 2   |
| **MODE**  | `PASSIVE`                       |
| **Use FTPS (TLS)** | Desmarcado (salvo que hayas configurado TLS en el servidor) |

Pulsa **▶ CONNECT**. Deberías ver el listado de archivos.

---

## 5. Operaciones en la GUI

| Botón        | Función                    |
|-------------|----------------------------|
| **↻ REFRESH** | Actualizar lista           |
| **↑ UPLOAD**  | Subir archivo              |
| **↓ DOWNLOAD** | Descargar seleccionado     |
| **✕ DELETE**  | Eliminar archivo            |
| **+ MKD**     | Crear directorio            |
| **- RMD**     | Eliminar directorio         |
| **✎ RENAME**  | Renombrar                   |
| **→ CD**      | Entrar en directorio        |
| **← CDUP**    | Directorio padre            |
| **? PWD**     | Ver ruta actual             |

Doble clic en una carpeta = cambiar a ese directorio.

---

## Perfiles de usuario

- **BASICO:** LIST, RETR, CWD, PWD, CDUP (solo lectura).
- **INTERMEDIO:** Lo anterior + STOR, DELE, renombrar.
- **ADMINISTRADOR:** Todo + MKD, RMD (gestión de directorios).

---

## Solución rápida de problemas

| Problema | Qué hacer |
|----------|-----------|
| Servidor pide directorio raíz | Pon `ftp.root.directory=files` (o tu ruta) en `server.properties`. En Windows usa `/` en la ruta. |
| "Archivo de usuarios no existe" | Usa SQLite (`ftp.users.database=files/ftp_users.db`) o crea usuarios con PasswordTool; en Windows usa `/` en rutas. |
| Puerto 21 en uso / Permission denied | En `server.properties` pon `ftp.control.port=2121` y conecta al puerto 2121. |
| ClassNotFoundException | Usa classpath `bin;lib\*` (Windows) o `bin:lib/*` (Linux/macOS). |
| FTPS no conecta | Marca "Use FTPS (TLS)" solo si configuraste TLS en el servidor (keystore + `server.properties`). Ver README → Habilitar FTPS (TLS). |

---

Para compilar con Maven, ejecutar el cliente de consola, migrar usuarios TXT→SQLite o desplegar en producción, consulta el [README.md](README.md).

**By Eduardo Díaz © 2025**
