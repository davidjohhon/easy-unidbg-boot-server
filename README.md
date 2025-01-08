# 写这个框架目的就是不想去写api 在unidbg调试好后，直接命令行即可实现api接口调用
## 不需要去启动服务，你只需要把文件路径指向那个class文件，即可实现热更新
## 你也不需要做任何的开发
## 快速使用
### 下载安装包和你的class文件放在一起
### class 文件在自己目录下自己找，放在一起即可
# 启动方法：
java -jar easy-unidbg-boot-server-1.1.0-SNAPSHOT.jar --F=./XX.class 
# 支持多个class文件
# 多个文件为
java -jar easy-unidbg-boot-server-1.1.0-SNAPSHOT.jar --F=./XX1.class --F=./XX2.class
即可完成启动

# 修改端口同 springboot 方式

# 如何使用？
访问：启动中发现打印日志：模块==> xxxx 记住这个，或者就是记住的类路，不想记忆的就直接copy日志中的内容

## http://localhost:8080/api/common/get?module=类全路径&params=1,2
 比如：com.XXX.类
## module 为上面说的这个，params 多个参数 用英文,隔开传递
params为传递参数public static void main(String[] args) 

#注意事项：
接口内部已经做完了所有的事情，这个测试用例就不要删除了，不然获取不到值，参考的是python管道获取值的思路
你需要用System.out.println把那个结果输出 记得把，无关的打印记得删干净，保留一个就好了

#最后，你只需要找到这个class文件，放进去，即可实现访问

# 打包使用
mvn clean package -Dmaven.test.skip=true 
# 复制target目录下的lib和easy-unidbg-boot-server-x.x.x-SNAPSHOT.jar
