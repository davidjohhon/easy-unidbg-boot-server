# 写这个框架目的就是不用去写api 在unidbg调试好后，直接命令行即可实现api接口调用
## 不需要去启动服务，你只需要把文件路径指向那个class文件，即可实现热更新
## 你也不需要做任何的开发
## 甚至你只需要把路径指向class路径即可
## 快速使用
### linux 或者mac
```js
./start.sh
```

### window 
```shell
./start.bat
```

## 注意事项
### 资源文件 放入 assets目录下
```shell
//比如java开发环境下应该这样放置你得静态资源,你需要把静态资源复制到这个assets目录下面
private static final String filePath = "assets/dcgc/libwtf.so";
```
### 寻找你得模块文件对应得.class文件，这个文件在你得target目录下得classes中进行寻找
### 找到这个文件后，你需要把这个文件复制到tasks目录下面
### main函数必须有，并且在你得代码中返回结果用System.out.println("结果") 返回
```shell
//例如:
 public static void main(String[] args) throws Exception {
        DcWtf dcWtf = new DcWtf();
        String str = "552ff1b3-0b9c-4bef-b7eb-b121119067f6";
        String str2 = "";
        String str3 = "1736135309751";
        String sign = dcWtf.getSign(str, str2, str3);
        System.out.println("sign=" + sign);
        dcWtf.destroy();
    }
 
```
### 修改端口 application.yml 默认8080 
```yml
spring:
  application:
    name: easy-unidbg-boot-server
server:
  port: 8080
```
### 如果你有一些第三方依赖包，未找到，请自行添加到lib目录下

### python sdk 查询结果
### api-get请求
```python 
import requests

url = "http://localhost:8080/api/common/invoke"
# module 为类路径
# args 为main函数参数
params = {
    "module": "com.sum.dcgc.DcWtf",
    "args": "1,2"
}
response = requests.get(url, params=params)

print(response.text)
print(response)

```
### api-post请求
```python 
import json

import requests

url = "http://localhost:8080/api/common/invoke"
# module 为类路径
module = "com.sum.dcgc.DcWtf"
# args 为main函数参数
args = [1, 2]

data = {
    "module": module,
    "args": [1, 2]
}
headers = {
    'Content-Type': 'application/json',
}
response = requests.post(url, headers=headers, json=data)

print(response.text)
print(response)
```

