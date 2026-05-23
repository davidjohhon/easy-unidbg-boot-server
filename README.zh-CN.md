[English](./README.md)

# Easy Unidbg Boot Server

在 unidbg 中调试好算法后，**无需编写任何 API 代码**，直接将编译好的 `.class` 文件放入 `tasks/` 目录，即可通过 HTTP 接口调用。内置完整的管理后台，支持中英文切换。

> **默认管理员：** `admin` / `admin123` | 登录地址 [http://localhost:8080/admin/login](http://localhost:8080/admin/login)

## 功能总览

### 核心 API
- **零代码 API 暴露** — 放入 `.class` 文件即获得 REST 接口
- **GET/POST /api/common/invoke** — 通过 HTTP 调用任意已加载模块的 `main()` 方法
- **System.out 捕获** — `main()` 输出自动作为 JSON 响应返回
- **动态类加载** — 运行时通过自定义 ClassLoader 加载类
- **热更新** — 每 5 秒扫描 `tasks/` 目录，自动加载/卸载

### 管理后台（`/admin/`）
- **仪表盘** — 模块数量、总请求数概览
- **服务管理** — 上传 `.class`，查看状态（ONLINE/OFFLINE/ERROR），上下线、重载、删除
- **资源文件** — 上传 `.so` 文件，自动放置到 `assets/<包名>/`，随服务级联删除
- **API Key 管理** — 生成 `sk-` 前缀密钥（SHA-256 哈希存储），列表掩码显示，删除
- **访问日志** — 查看 API 调用记录，按状态/模块/日期筛选，清空
- **操作日志** — 记录所有后台操作，单条删除或清空
- **用户管理** — 添加/删除操作员，修改密码，admin 用户受保护
- **个人中心** — 查看当前用户，修改密码

### 安全
- **Spring Security** — 表单登录，BCrypt 密码加密
- **API Key 鉴权** — 所有 API 调用需携带 `apikey` 参数，与 SHA-256 哈希值比对
- **会话管理** — 基于 Session 的后台认证

### 国际化
- **英文 / 中文** — 通过 `?lang=en` 或 `?lang=zh` 切换，session 持久化
- **界面翻译** — 所有管理页面完全双语
- **API 错误** — 错误提示随语言自动切换

### 数据存储
- **H2 嵌入式数据库** — 基于文件，零外部依赖
- **JPA 自动建表** — 6 张表自动创建/更新
- **定时清理** — 自动清理超过保留天数的日志（默认 30 天）

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
./sh/start.sh              # 热更新（默认）
./sh/start.sh basic        # 基础模式，通过后台上传
./sh/start.sh static tasks/DcWtf.class    # 静态加载
./sh/start.sh both tasks/DcWtf.class      # 静态 + 热更新

# Windows
./sh/start.bat             # 热更新（默认）
./sh/start.bat basic       # 基础模式
./sh/start.bat static DcWtf.class         # 静态加载
./sh/start.bat both DcWtf.class           # 静态 + 热更新
```

四种启动模式均测试通过（200 OK）：

| 模式 | 状态 |
|------|------|
| `basic` | ✅ |
| `hot`（默认） | ✅ |
| `static tasks/DcWtf.class` | ✅ |
| `both tasks/DcWtf.class` | ✅ |

服务默认启动在 **http://localhost:8080**。

### 后台管理地址

| 管理后台 | http://localhost:8080/admin/login |
| 默认账号 | **`admin` / `admin123`** |
| 语言切换 | 页面右上角 EN / 中文 按钮，或 URL 加 `?lang=en` / `?lang=zh` |

## API 使用

所有 API 调用需要 **API Key**，从管理后台 → API Keys → Generate New Key 生成。

### 调用模块

**接口地址：** `/api/common/invoke?apikey=sk-xxxxxxxx...`

#### GET 请求

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

#### POST 请求

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

#### 响应格式

```json
// 成功
{"ts": 1700000000000, "status": "ok", "data": "sign=xxxx"}

// 模块不存在
{"ts": 1700000000000, "errorCode": 500, "errorMsg": "模块不存在", "status": "fail"}

// API Key 无效
{"errorCode": 401, "errorMsg": "Invalid or missing API key", "status": "fail"}
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

## 项目结构

```
easy-unidbg-boot-server/
├── assets/           # .so 资源文件，按包名组织
├── data/             # H2 数据库文件
├── sh/               # 启动脚本
├── sdk/python/       # Python SDK 示例
├── tasks/            # 存放编译好的 .class 文件
├── src/
│   ├── main/java/com/easy/unidbg/
│   │   ├── config/       # 安全配置、拦截器、国际化
│   │   ├── controller/   # API + 后台控制器
│   │   ├── service/      # 业务逻辑、日志清理
│   │   ├── entity/       # JPA 实体（6 张表）
│   │   ├── repository/   # 数据仓库
│   │   ├── components/   # 模块容器、输出捕获
│   │   ├── dto/          # 请求/响应 DTO
│   │   └── utils/        # MD5 工具
│   │── resources/templates/  # Thymeleaf 后台页面（9 个）
│   │── resources/static/     # CSS/JS/字体（本地）
│   │── resources/messages*.properties  # 中英文语言包
│   └── test/            # 集成测试（33 个用例）
├── pom.xml
└── README.md
```

## 启动模式

| 模式 | 命令 | 说明 |
|------|------|------|
| 基础模式 | `./sh/start.sh basic` | 仅启动服务，通过管理后台上传模块 |
| 热更新 | `./sh/start.sh hot`（默认） | 监听 `tasks/` 目录，自动加载 |
| 静态加载 | `./sh/start.sh static tasks/DcWtf.class` | 启动时加载指定 class 文件 |
| 组合模式 | `./sh/start.sh both tasks/DcWtf.class` | 静态加载 + 热更新同时运行 |

四种模式均已测试验证通过。

## 配置说明

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080                       # HTTP 端口

app:
  version: 2.0.0-RELEASE           # 版本号
  cleanup:
    access-log:
      enabled: true                # 是否启用日志自动清理
      keep-days: 30                # 日志保留天数
      cron: 0 0 3 * * ?            # 每天凌晨 3 点执行

spring:
  servlet:
    multipart:
      max-file-size: 200MB         # 上传文件大小限制
      max-request-size: 200MB
```

## 管理后台功能一览

| 页面 | 路径 | 功能 |
|------|------|------|
| **仪表盘** | `/admin/dashboard` | 模块数量、总请求数 |
| **服务管理** | `/admin/modules` | 上传 .class、状态标识、上下线、删除、资源文件 |
| **API Keys** | `/admin/apikeys` | 生成（一次性显示）、列表（掩码）、删除、分页 |
| **访问日志** | `/admin/logs` | 按状态/模块/日期筛选、分页、清空 |
| **操作日志** | `/admin/oplogs` | 管理员操作审计、分页、单删/清空 |
| **用户管理** | `/admin/users` | 添加/删除操作员 |
| **个人中心** | `/admin/profile` | 用户名、修改密码 |

## License

MIT
