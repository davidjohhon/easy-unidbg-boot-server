[中文版](./README.zh-CN.md)

# Easy Unidbg Boot Server

Expose your unidbg debugging results as HTTP APIs **without writing any API code**. Just drop your compiled `.class` file into the `tasks/` directory, and you're done.

> **Default Admin:** `admin` / `admin123` | Login at [http://localhost:8080/admin/login](http://localhost:8080/admin/login)

## Overview

Easy Unidbg Boot Server is a Spring Boot-based web service that dynamically loads compiled Java classes (unidbg modules) and exposes their `main()` output as REST APIs. It includes a full-featured admin panel for managing services, users, API keys, and logs with bilingual (English/Chinese) support.

## Features

### Core API
- **Zero-code API exposure** — drop `.class` files, get REST endpoints automatically
- **GET/POST /api/common/invoke** — invoke any loaded module's `main()` method via HTTP
- **System.out capture** — `main()` output is captured and returned as JSON response
- **Dynamic ClassLoader** — loads classes at runtime via custom ClassLoader
- **Hot reload** — watches `tasks/` directory every 5 seconds, auto-loads/unloads on MD5 change

### Admin Panel (`/admin/`)
- **Dashboard** — overview of module count and total API requests
- **Service Management** — upload `.class` files, view status (ONLINE/OFFLINE/ERROR), take online/offline, reload, delete
- **Resource Files** — upload `.so` files per service, auto-placed in `assets/<package>/`, cascading delete with service
- **API Key Management** — generate `sk-` prefixed keys (SHA-256 hashed), list with masked display, delete
- **Access Logs** — view all API calls with module name, args, response, client IP, status, timestamp; filter by status/module/date; clear all
- **Operation Logs** — track all admin operations (upload, delete, offline, online, etc.); single delete or clear all
- **User Management** — add/delete admin users, change password, admin user protected from deletion
- **Profile** — view current user, change password with old password verification

### Security
- **Spring Security** — form login with BCrypt password hashing
- **API Key Authentication** — all API calls require a valid `apikey` parameter, matched against SHA-256 hashed keys
- **Session Management** — session-based admin authentication, session fixation protection disabled
- **Role-based Access** — admin-only panel access

### Internationalization
- **English / Chinese** — toggle via `?lang=en` or `?lang=zh` parameter, persisted in session
- **UI translations** — all admin pages fully bilingual
- **API error messages** — error responses switch language based on request locale

### Data Storage
- **H2 Embedded Database** — file-based, zero external dependencies
- **JPA Auto Schema** — tables created/updated automatically
- **Scheduled Cleanup** — automatic cleanup of access logs and operation logs older than configured days (default 30)

### Logging & Audit
- **Access Logs** — every API call recorded with module, args, response, client IP, status
- **Operation Logs** — every admin action recorded with operator, action, target, detail, result
- **Logback** — rolling file appenders with configurable retention and size

### Module Lifecycle
| State | Description |
|-------|-------------|
| **ONLINE** | Loaded in container, accessible via API |
| **OFFLINE** | Unloaded from container, files retained for future online |
| **ERROR** | Failed to load (file not found, class loading error) |

- Upload → ONLINE
- Offline → unload from container, keep files on disk
- Online → reload from disk back to container
- Delete → requires OFFLINE state first, removes container + files + DB record
- Reload → re-reads file from disk and reloads class

### Resource File Management
- Upload `.so` files alongside modules via admin panel
- Files stored in `assets/<package-segment>/` (auto-derived from class package)
- Multiple files per module supported
- Cascading delete: removing a module also deletes its resource files
- Individual `.so` file deletion also supported

### Startup Modes
| Mode | Command | Description |
|------|---------|-------------|
| Basic | `./start.sh basic` | Start server only, manage modules via admin panel |
| Hot Reload | `./start.sh hot` (default) | Watch `tasks/` directory for auto-loading |
| Static | `./start.sh static DcWtf.class` | Load specified `.class` files at startup |
| Combined | `./start.sh both DcWtf.class` | Static load + hot reload simultaneously |

## Quick Start

### Prerequisites

- Java 8+
- Maven (to build)

### Build

```bash
mvn clean package
```

### Run

```bash
# Hot reload mode (recommended) — watches ./tasks directory
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --U=./tasks

# Static mode — load a specific class
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --F=./tasks/DcWtf.class

# Combined — static load + hot reload watching
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --F=./tasks/DcWtf.class --U=./tasks
```

### Using start scripts

```bash
# Linux / macOS
./sh/start.sh              # hot reload (default)
./sh/start.sh basic        # basic mode, manage via admin panel
./sh/start.sh static tasks/DcWtf.class    # load specific class
./sh/start.sh both tasks/DcWtf.class      # static + hot reload

# Windows
./sh/start.bat             # hot reload (default)
./sh/start.bat basic       # basic mode
./sh/start.bat static DcWtf.class         # load specific class
./sh/start.bat both DcWtf.class           # static + hot reload
```

All four modes verified (200 OK):

| Mode | Status |
|------|--------|
| `basic` | ✅ |
| `hot` (default) | ✅ |
| `static tasks/DcWtf.class` | ✅ |
| `both tasks/DcWtf.class` | ✅ |

Server starts on **http://localhost:8080** by default.

### Admin Panel Access

| URL | http://localhost:8080/admin/login |
| Default credentials | **`admin` / `admin123`** |
| Language switch | Add `?lang=en` or `?lang=zh` to URL |

## Project Structure

```
easy-unidbg-boot-server/
├── assets/           # .so resource files, organized by package
├── data/             # H2 embedded database files
├── sh/               # Start scripts (start.sh / start.bat)
├── sdk/python/       # Python SDK examples
├── tasks/            # Drop compiled .class files here
├── src/
│   ├── main/
│   │   ├── java/com/easy/unidbg/
│   │   │   ├── config/       # Security, interceptors, i18n
│   │   │   ├── controller/   # API + Admin controllers
│   │   │   ├── service/      # Business logic, log cleanup
│   │   │   ├── entity/       # JPA entities (6 tables)
│   │   │   ├── repository/   # Spring Data repositories
│   │   │   ├── components/   # ModuleContainer, SystemOutCapture
│   │   │   ├── dto/          # Request/Response DTOs
│   │   │   └── utils/        # MD5 utility
│   │   └── resources/
│   │       ├── templates/    # Thymeleaf admin panel (9 pages)
│   │       ├── static/       # CSS, JS, fonts (Bootstrap local)
│   │       ├── messages*.properties  # i18n (EN/ZH)
│   │       └── application.yml
│   └── test/java/            # Integration tests (33 test cases)
├── pom.xml
└── README.md
```

## API Usage

All API calls require an **API Key**. Generate one from Admin Panel → API Keys → Generate New Key.

### Invoke a module

**Endpoint:** `/api/common/invoke?apikey=sk-xxxxxxxx...`

#### GET request

```
GET /api/common/invoke?module=com.sum.dcgc.DcWtf&args=arg1,arg2&apikey=sk-xxxx
```

```python
import requests

url = "http://localhost:8080/api/common/invoke"
params = {
    "module": "com.sum.dcgc.DcWtf",
    "args": "1,2",
    "apikey": "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
response = requests.get(url, params=params)
print(response.text)
```

#### POST request

```python
import requests

url = "http://localhost:8080/api/common/invoke"
apikey = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
data = {
    "module": "com.sum.dcgc.DcWtf",
    "args": ["1", "2"]
}
headers = {"Content-Type": "application/json"}
response = requests.post(url + "?apikey=" + apikey, headers=headers, json=data)
print(response.text)
```

#### Response

```json
// Success
{"ts": 1700000000000, "status": "ok", "data": "sign=xxxx"}

// Module not found
{"ts": 1700000000000, "errorCode": 500, "errorMsg": "Module not found", "status": "fail"}

// Invalid API key
{"errorCode": 401, "errorMsg": "Invalid or missing API key", "status": "fail"}
```

## How to Write a Module

1. **Your class must have a `main(String[] args)` method**
2. **Return results via `System.out.println()`** — stdout is captured and returned as the API response
3. **Put the compiled `.class` file into the `tasks/` directory**

Example:

```java
public class DcWtf {
    private final AndroidEmulator emulator;
    private final DvmClass dvmClass;

    public DcWtf() {
        // ... unidbg initialization ...
    }

    public String getSign(String p1, String p2, String p3) {
        // ... call native method via unidbg ...
        return result;
    }

    public static void main(String[] args) throws Exception {
        DcWtf dcWtf = new DcWtf();
        String sign = dcWtf.getSign(args[0], args[1], args[2]);
        System.out.println("sign=" + sign);
        dcWtf.destroy();
    }
}
```

## Static Resources

Place resource files (`.so`, images, etc.) in the `assets/` directory.

```java
// In your unidbg code, reference them as:
private static final String filePath = "assets/dcgc/libwtf.so";
```

## Configuration

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080                       # HTTP port

app:
  version: 2.0.0-RELEASE           # Display version
  cleanup:
    access-log:
      enabled: true                # Enable auto log cleanup
      keep-days: 30                # Retention period
      cron: 0 0 3 * * ?            # Daily at 3 AM

spring:
  servlet:
    multipart:
      max-file-size: 200MB        # Max .class/.so upload size
      max-request-size: 200MB
```

## Admin Panel

The server includes a built-in web admin panel at **http://localhost:8080/admin/login**.

Default credentials: `admin` / `admin123`

### Pages Overview

| Page | Path | Features |
|------|------|----------|
| **Dashboard** | `/admin/dashboard` | Module count, total request stats |
| **Services** | `/admin/modules` | Upload .class, status badges, offline/online, delete, resource files |
| **API Keys** | `/admin/apikeys` | Generate (shown once), list (masked), delete, paginated |
| **Access Logs** | `/admin/logs` | Filter by status/module/date, paginated, clear all |
| **Operation Logs** | `/admin/oplogs` | Admin action audit, paginated, single delete, clear all |
| **Users** | `/admin/users` | Add/delete admin users |
| **Profile** | `/admin/profile` | Username display, change password |

### Language Switch

Toggle between English and Chinese via the top-right buttons. The setting persists in your session.

## Data Storage

- **H2 Embedded Database** (`./data/easy-unidbg.mv.db`) — file-based, zero external dependencies
- **Tables**: `module_info`, `access_log`, `operation_log`, `admin_user`, `api_key`, `resource_file`
- Tables are auto-created/updated by JPA (`ddl-auto: update`)
- Scheduled cleanup removes logs older than configured retention (default 30 days)

## Hot Reload Details

- Start with `--U=<directory>` (e.g. `--U=./tasks`)
- Server polls `.class` files every 5 seconds
- MD5 computed for each file; only changed files trigger reload
- Removed files are auto-unloaded from container
- OFFLINE modules in DB are skipped during hot reload
- Allows modifying and recompiling code without restart

## Startup Modes

All four modes have been tested and verified (200 OK):

| Mode | Command | Description |
|------|---------|-------------|
| Basic | `./sh/start.sh basic` | Start server only, manage via admin panel |
| Hot Reload | `./sh/start.sh hot` (default) | Watch `tasks/` directory for auto-loading |
| Static | `./sh/start.sh static tasks/DcWtf.class` | Load specified `.class` files at startup |
| Combined | `./sh/start.sh both tasks/DcWtf.class` | Static load + hot reload simultaneously |

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot** | 2.6.6 | Web framework, security, JPA |
| **unidbg** | 0.9.8 | Android ARM emulation |
| **H2 Database** | 2.6.6 | Embedded file-based DB |
| **Thymeleaf** | 3.0.x | Server-side HTML templates |
| **Spring Security** | 5.6.x | Authentication, authorization |
| **Bootstrap** | 5.3.0 | Admin UI framework |
| **Bootstrap Icons** | 1.11.0 | Icon set |
| **Java** | 8+ | Runtime |
| **Lombok** | 1.18.30 | Boilerplate reduction |

## License

MIT
