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
