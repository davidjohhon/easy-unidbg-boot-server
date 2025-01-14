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
