import requests

url = "http://localhost:8080/api/common/invoke"
# module 为类路径
# args 为main函数参数
# apikey 在后台管理 -> API Keys 中生成
params = {
    "module": "com.sum.dcgc.DcWtf",
    "args": "1,2",
    "apikey": "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
}
response = requests.get(url, params=params)

print(response.text)
print(response)
