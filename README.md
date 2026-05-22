[中文版](./README.zh-CN.md)

# easy-unidbg-boot-server

Expose your unidbg debugging results as HTTP APIs **without writing any API code**. Just drop your compiled `.class` file into the `tasks/` directory, and you're done.

## Features

- **Zero API development** — no Controller, no Service, no boilerplate
- **Hot reload** — class files are watched and reloaded automatically every 5 seconds (by MD5 change), no server restart required
- **Dual HTTP methods** — supports both GET and POST requests
- **Custom ClassLoader** — dynamically loads and manages class modules at runtime
- **System.out capture** — whatever your `main()` prints becomes the API response
- **Static resource support** — place `.so` / other assets in the `assets/` directory

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
./sh/start.sh

# Windows
./sh/start.bat
```

Server starts on **http://localhost:8080** by default.

## Project Structure

```
easy-unidbg-boot-server/
├── assets/           # Static resource files (e.g. .so, images)
│   └── dcgc/
│       └── libwtf.so
├── sh/               # Start scripts
│   ├── start.sh      #   Linux / macOS
│   └── start.bat     #   Windows
├── sdk/              # Client SDK examples
│   └── python/
├── tasks/            # Drop your .class files here
│   └── DcWtf.class
├── src/              # Source code
└── pom.xml
```

## API Usage

### Invoke a module

**Endpoint:** `/api/common/invoke`

#### GET request

```
GET /api/common/invoke?module=com.sum.dcgc.DcWtf&args=arg1,arg2
```

```python
import requests

url = "http://localhost:8080/api/common/invoke"
params = {
    "module": "com.sum.dcgc.DcWtf",
    "args": "1,2"
}
response = requests.get(url, params=params)
print(response.text)
```

#### POST request

```python
import requests

url = "http://localhost:8080/api/common/invoke"
data = {
    "module": "com.sum.dcgc.DcWtf",
    "args": ["1", "2"]
}
headers = {"Content-Type": "application/json"}
response = requests.post(url, headers=headers, json=data)
print(response.text)
```

#### Response

```json
{
  "ts": 1700000000000,
  "status": "ok",
  "data": "sign=xxxx"
}
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
  port: 8080    # change port here
```

## Hot Reload Details

- When started with `--U=<directory>`, the server scans `.class` files every 5 seconds
- MD5 is computed for each file; only changed files trigger class reload
- Removed files are automatically unloaded
- This allows you to modify and recompile your unidbg code without restarting the server

## Adding External Dependencies

If your module depends on third-party JARs, place them in the `lib/` directory next to the JAR.

## Tech Stack

| Component | Version |
|-----------|---------|
| Spring Boot | 2.6.6 |
| unidbg | 0.9.8 |
| Java | 8+ |
| Lombok | 1.18.30 |
| Gson | 2.8.9 |

## License

MIT
