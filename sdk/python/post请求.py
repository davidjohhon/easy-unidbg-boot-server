import json

import requests

url = "http://localhost:8080/api/common/invoke"
# module 为类路径
module = "com.sum.dcgc.DcWtf"
# apikey 在后台管理 -> API Keys 中生成
apikey = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
# POST 方式（apikey 放在 URL 参数中）
data = {
    "module": module,
    "args": [1, 2]
}
headers = {
    'Content-Type': 'application/json',
}
response = requests.post(url + "?apikey=" + apikey, headers=headers, json=data)

print(response.text)
