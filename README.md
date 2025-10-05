# Servidor y Cliente FTP en Java

> Implementación personalizada de FTP en Java con servidor multi-hilo con control de acceso basado en roles y cliente de consola interactivo construido sobre Apache Commons Net.

[![Licencia: MIT](https://img.shields.io/badge/Licencia-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)

---

## Descripción General

Este proyecto implementa el protocolo FTP desde cero como ejercicio educativo, demostrando programación de redes, manejo concurrente de clientes e implementación de protocolos en Java.

**Componentes:**
- **Servidor** ([JavaFtpServer.java](src/FTP/Server/JavaFtpServer.java)) — Servidor FTP personalizado que soporta modos de transferencia de datos ACTIVE y PASSIVE con manejo concurrente de clientes
- **Cliente** ([JavaFtpClient.java](src/FTP/Client/JavaFtpClient.java)) — Cliente FTP de consola interactivo que utiliza la librería Apache Commons Net
- **Dependencia** — `commons-net-3.11.1.jar` (incluida en [lib/](lib/))

---

## Características

### Capacidades del Servidor
- **Conexiones Concurrentes:** Arquitectura multi-hilo usando `ExecutorService` para manejar múltiples clientes simultáneamente
- **Sistema de Autenticación:** Autenticación basada en usuarios con credenciales en texto plano almacenadas en `files/users/users.txt`
- **Control de Acceso Basado en Roles (RBAC):** Tres niveles de permisos controlando la disponibilidad de comandos:
  - `BASICO` — Acceso de solo lectura (LIST, RETR, CWD, PWD)
  - `INTERMEDIO` — Acceso de lectura/escritura (añade STOR, MKD, RMD, RNFR, RNTO, DELE)
  - `ADMINISTRADOR` — Acceso administrativo completo
- **Modos Duales de Transferencia de Datos:**
  - **Modo PASSIVE** — El servidor abre puerto de datos, el cliente se conecta (compatible con NAT/firewall)
  - **Modo ACTIVE** — El cliente abre puerto de datos, el servidor se conecta (requiere reenvío de puertos en el cliente)
- **Comandos FTP Estándar:** USER, PASS, SYST, PASV, PORT, LIST, STOR, RETR, DELE, MKD, RMD, RNFR, RNTO, CWD, CDUP, PWD, QUIT

### Características del Cliente
- Menú de consola interactivo con 10 operaciones
- Configuración automática de conexión con host/puerto configurable
- Selección de modo (PASSIVE/ACTIVE) con configuración guiada
- Carga/descarga de archivos con retroalimentación de progreso
- Navegación y manipulación de directorios
- Operaciones de renombrado y eliminación de archivos/directorios

---

## Estructura del Proyecto

```
java-ftp/
├── src/
│   └── FTP/
│       ├── Client/
│       │   ├── JavaFtpClient.java      # Punto de entrada principal del cliente
│       │   └── ClientFuntions.java     # Implementaciones de comandos del cliente
│       ├── Server/
│       │   ├── JavaFtpServer.java      # Punto de entrada principal del servidor
│       │   ├── FtpClientHandler.java   # Manejador por conexión (Runnable)
│       │   ├── ServerFunctions.java    # Implementaciones de comandos FTP
│       │   ├── User.java               # Modelo de credenciales de usuario
│       │   └── UserProfile.java        # Enum de RBAC
│       └── Util/
│           └── Util.java               # Utilidades compartidas
├── lib/
│   └── commons-net-3.11.1.jar          # Librería Apache Commons Net
├── files/
│   └── users/
│       └── users.txt                   # Base de datos de credenciales
├── bin/                                # Clases compiladas (generado)
├── LICENSE
└── README.md
```

---

## Requisitos

- **JDK:** 8 o superior
- **Shell:** PowerShell (Windows) o Bash (Linux/macOS)
- **Red:** El puerto 21 debe estar disponible (o modificar `CONTROL_PORT` para pruebas en puertos no privilegiados >1024)

---

## Instalación y Configuración

### 1. Configuración de Usuarios

Crear el archivo de usuarios en `files/users/users.txt` con el siguiente formato:

```
username:password:PERFIL
```

**Perfiles:** `BASICO`, `INTERMEDIO`, `ADMINISTRADOR`

**Ejemplo:**
```
alice:pass123:BASICO
bob:secret456:INTERMEDIO
admin:admin789:ADMINISTRADOR
```

### 2. Compilación (PowerShell)

```powershell
# Crear directorio de salida
mkdir -Force bin

# Compilar todas las fuentes con classpath
javac -d bin -cp "lib/commons-net-3.11.1.jar" `
  src\FTP\Client\*.java `
  src\FTP\Server\*.java `
  src\FTP\Util\*.java
```

**Linux/macOS (Bash):**
```bash
mkdir -p bin
javac -d bin -cp "lib/commons-net-3.11.1.jar" \
  src/FTP/Client/*.java \
  src/FTP/Server/*.java \
  src/FTP/Util/*.java
```

---

## Uso

### Iniciar el Servidor

**PowerShell:**
```powershell
java -cp "bin;lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer
```

**Linux/macOS:**
```bash
java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer
```

Al iniciar:
1. El servidor muestra un banner en ASCII
2. Solicita la ruta del directorio raíz (directorio desde el cual se servirán archivos)
3. Valida la existencia del directorio `files/` y `files/users/users.txt`
4. Comienza a escuchar en el puerto 21

**Nota:** En sistemas Unix, enlazar al puerto 21 requiere privilegios root/sudo. Para pruebas, modificar la constante `CONTROL_PORT` en [JavaFtpServer.java:21](src/FTP/Server/JavaFtpServer.java#L21) a un puerto >1024.

### Iniciar el Cliente

**PowerShell:**
```powershell
java -cp "bin;lib/commons-net-3.11.1.jar" FTP.Client.JavaFtpClient
```

**Linux/macOS:**
```bash
java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Client.JavaFtpClient
```

**Flujo de Conexión:**
1. Ingresar hostname (por defecto: `localhost`)
2. Ingresar puerto (por defecto: `21`)
3. Proporcionar nombre de usuario y contraseña
4. Seleccionar modo de transferencia de datos (`PASSIVE` o `ACTIVE`)
5. Se muestra el menú interactivo

**Menú del Cliente:**
```
[1]  Listar archivos
[2]  Subir archivo
[3]  Descargar archivo
[4]  Eliminar archivo
[5]  Crear directorio
[6]  Eliminar directorio
[7]  Renombrar archivo/directorio
[8]  Cambiar directorio de trabajo
[9]  Cambiar al directorio superior
[10] Salir
```

---

## Modos de Transferencia de Datos FTP

### Modo PASSIVE (Recomendado)
- **Cuándo usar:** Cliente detrás de NAT/firewall
- **Cómo funciona:**
  1. El cliente envía comando `PASV`
  2. El servidor abre un puerto aleatorio y envía los detalles de conexión
  3. El cliente se conecta a ese puerto para la transferencia de datos
- **Configuración:** Manejado automáticamente por el cliente

### Modo ACTIVE
- **Cuándo usar:** Cliente con IP pública o reenvío de puertos adecuado
- **Cómo funciona:**
  1. El cliente abre un puerto local y envía comando `PORT` con los detalles de conexión
  2. El servidor se conecta al puerto especificado del cliente para la transferencia de datos
- **Configuración:** El cliente solicita el puerto de datos (se recomienda >5000)

---

## Permisos Basados en Roles

| Comando | BASICO | INTERMEDIO | ADMINISTRADOR |
|---------|:------:|:----------:|:-------------:|
| LIST    | ✓      | ✓          | ✓             |
| RETR    | ✓      | ✓          | ✓             |
| CWD     | ✓      | ✓          | ✓             |
| CDUP    | ✓      | ✓          | ✓             |
| PWD     | ✓      | ✓          | ✓             |
| STOR    | ✗      | ✓          | ✓             |
| DELE    | ✗      | ✓          | ✓             |
| MKD     | ✗      | ✓          | ✓             |
| RMD     | ✗      | ✓          | ✓             |
| RNFR    | ✗      | ✓          | ✓             |
| RNTO    | ✗      | ✓          | ✓             |

*Los comandos no autorizados retornan: `530 Not logged in` o código de error apropiado*

---

## Consideraciones de Seguridad

**⚠️ ESTE ES UN PROYECTO EDUCATIVO — NO LISTO PARA PRODUCCIÓN**

**Limitaciones de Seguridad Conocidas:**
- Almacenamiento de contraseñas en texto plano en `users.txt`
- Sin cifrado TLS/SSL para canales de control o datos
- Validación de entrada básica sin sanitización comprehensiva
- Sin limitación de tasa o protección contra fuerza bruta
- Protección de traversal de directorio de nivel único

**Antes de uso en producción, implementar:**
- Hashing de contraseñas (bcrypt/Argon2) con sales
- Cifrado TLS (FTPS)
- Validación comprehensiva de traversal de rutas
- Limitación de tasa de autenticación
- Registro de auditoría
- Cuotas de recursos por usuario
- Alternativas modernas (SFTP, APIs de archivos HTTPS)

---

## Desarrollo y Depuración

### `.gitignore` Recomendado
```gitignore
bin/
files/
*.class
*.log
.DS_Store
```

### Solución de Problemas

**Permiso Denegado en Puerto 21 (Linux/macOS):**
```bash
# Opción 1: Ejecutar con sudo (no recomendado para desarrollo)
sudo java -cp "bin:lib/commons-net-3.11.1.jar" FTP.Server.JavaFtpServer

# Opción 2: Usar puerto no privilegiado (recomendado)
# Modificar CONTROL_PORT en JavaFtpServer.java a 2121
```

**Conexión Rechazada:**
- Verificar que el servidor esté ejecutándose (`netstat -an | grep 21`)
- Revisar que las reglas del firewall permitan tráfico en el puerto 21
- Confirmar que el cliente se está conectando al host/puerto correcto

**Fallo de Autenticación:**
- Verificar que `files/users/users.txt` exista y tenga el formato correcto
- Revisar espacios finales en las entradas de usuario/contraseña
- Asegurar que el perfil de usuario sea un valor enum válido

---

## Contribuciones

¡Las contribuciones son bienvenidas! Por favor, siéntete libre de enviar:
- Reportes de errores vía Issues
- Mejoras de funcionalidad vía Pull Requests
- Mejoras de documentación
- Reportes de vulnerabilidades de seguridad (por favor divulgar responsablemente)

**Áreas de Mejora:**
- Añadir soporte TLS/SSL
- Implementar almacenamiento seguro de contraseñas
- Añadir pruebas unitarias comprehensivas
- Soportar comandos FTP adicionales (ABOR, REST, STAT)
- Soporte IPv6
- Archivo de configuración para ajustes del servidor

---

## Licencia

Este proyecto está licenciado bajo la **Licencia MIT** — ver archivo [LICENSE](LICENSE) para detalles.

---

## Autor

**Eduardo Díaz Sánchez**

Para preguntas, issues o contribuciones, por favor usar la página de [GitHub Issues](../../issues).

---

## Agradecimientos

- Construido con la librería [Apache Commons Net](https://commons.apache.org/proper/commons-net/)
- Protocolo FTP: [RFC 959](https://tools.ietf.org/html/rfc959)
- Desarrollado con fines educativos para entender protocolos de red y programación de sockets en Java
