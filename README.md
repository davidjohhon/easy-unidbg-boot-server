# 写这个框架目的就是不想去写api 在unidbg调试好后，直接命令行即可实现api接口调用
# 打包使用
mvn clean package -Dmaven.test.skip=true 
# 复制target目录下的lib和easy-unidbg-boot-server-x.x.x-SNAPSHOT.jar
和你的class文件放在一起
# class 文件在自己目录下自己找，放在一起即可
# 启动方法：
java -jar easy-unidbg-boot-server-1.1.0-SNAPSHOT.jar --F=./XX.class 
# 支持多个class文件
# 多个文件为
java -jar easy-unidbg-boot-server-1.1.0-SNAPSHOT.jar --F=./XX1.class --F=./XX2.class
即可完成启动

# 修改端口同 springboot 方式

