[English](./README.md)

# easy-unidbg-boot-server

在 unidbg 中调试好算法后，**无需编写任何 API 代码**，直接将编译好的 `.class` 文件放入 `tasks/` 目录，即可通过 HTTP 接口调用。

## 功能特性

- **零 API 开发** — 不需要写 Controller、Service 等样板代码
- **热更新** — 每 5 秒扫描 `.class` 文件 MD5 变化，自动加载/卸载模块，无需重启服务
- **双请求方式** — 同时支持 GET 和 POST 请求
- **自定义类加载器** — 运行态动态加载和管理 Class 模块
- **System.out 捕获** — `main()` 方法中输出的内容自动作为 API 返回值
- **静态资源支持** — `.so` 等资源文件放入 `assets/` 目录即可

## 快速开始

### 环境要求

- Java 8+
- Maven（用于编译）

### 编译

```bash
mvn clean package
```

### 启动

```bash
# 热更新模式（推荐）— 监听 ./tasks 目录
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --U=./tasks

# 静态模式 — 加载指定 class 文件
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --F=./tasks/DcWtf.class

# 组合模式 — 静态加载 + 热更新监听
java -Xms256m -Xmx256m -jar target/easy-unidbg-boot-server-*.jar --F=./tasks/DcWtf.class --U=./tasks
```

### 使用启动脚本

```bash
# Linux / macOS
./sh/start.sh

# Windows
./sh/start.bat
```

服务默认启动在 **http://localhost:8080**。

## 项目结构

```
easy-unidbg-boot-server/
├── assets/           # 静态资源文件（.so、图片等）
│   └── dcgc/
│       └── libwtf.so
├── sh/               # 启动脚本
│   ├── start.sh      #   Linux / macOS
│   └── start.bat     #   Windows
├── sdk/              # 客户端 SDK 示例
│   └── python/
├── tasks/            # 存放编译好的 .class 文件
│   └── DcWtf.class
├── src/              # 源代码
└── pom.xml
```

## API 使用

### 调用模块

**接口地址：** `/api/common/invoke`

#### GET 请求

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

#### POST 请求

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

#### 响应格式

```json
{
  "ts": 1700000000000,
  "status": "ok",
  "data": "sign=xxxx"
}
```

## 如何编写模块

1. **类中必须包含 `main(String[] args)` 方法**
2. **通过 `System.out.println()` 返回结果** — stdout 输出会被捕获并作为 API 返回值
3. **将编译后的 `.class` 文件放入 `tasks/` 目录**

示例：

```java
public class DcWtf {
    private final AndroidEmulator emulator;
    private final DvmClass dvmClass;

    public DcWtf() {
        // ... unidbg 初始化 ...
    }

    public String getSign(String p1, String p2, String p3) {
        // ... 通过 unidbg 调用 native 方法 ...
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

## 静态资源

将 `.so`、图片等资源文件放入 `assets/` 目录，在代码中按相对路径引用：

```java
private static final String filePath = "assets/dcgc/libwtf.so";
```

## 配置修改

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080    # 在此处修改端口号
```

## 热更新原理

- 启动时传入 `--U=<目录路径>` 参数
- 服务每 5 秒扫描指定目录下的所有 `.class` 文件
- 计算每个文件的 MD5，仅发生变化的文件触发重新加载
- 被删除的文件自动卸载
- 修改和重新编译 unidbg 代码后无需重启服务，立即生效

## 添加外部依赖

如果模块依赖第三方 JAR 包，请将其放入 JAR 同级的 `lib/` 目录中。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 2.6.6 |
| unidbg | 0.9.8 |
| Java | 8+ |
| Lombok | 1.18.30 |
| Gson | 2.8.9 |

## License

MIT
