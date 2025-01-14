#!/bin/bash

# jvm参数
jvm_ops="-Xms256m -Xmx256m"

# 热启动
java ${jvm_ops}  -jar easy-unidbg-boot-server-*.jar --U=./tasks

# --F 指定class启动 可以指定多个
# java ${jvm_ops}  -jar easy-unidbg-boot-server-*.jar  --F=./tasks/DcWtf.class
# --F 指定class启动 可以指定多个
# java ${jvm_ops}  -jar easy-unidbg-boot-server-*.jar  --F=./tasks/DcWtf.class --F=./tasks/DcWtf.class

# 热更新启动 和 指定class启动 同时会监听文件变动
# java ${jvm_ops}  -jar easy-unidbg-boot-server-*.jar  --F=./tasks/DcWtf.class --U=./tasks

# 在只有配置--U得时候 热更新启动 需要把class文件放在tasks目录下面
