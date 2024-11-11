# fast-and-mercury-router-reverse-proxy-server

#### 介绍

fast-and-mercury-router-reverse-proxy-server

这段Kotlin代码实现了一个简单的反向代理服务器，主要功能如下：
命令行参数解析：通过CliktCommand解析命令行参数，包括上游服务器地址和端口号。
启动Netty服务器：根据解析的命令行参数启动一个Netty服务器，监听指定端口。
请求拦截与处理：拦截所有请求，将请求转发到上游服务器，并根据响应内容类型进行不同处理：
HTML内容：下载完整HTML内容，插入自定义脚本并替换URL，然后返回给客户端。
其他内容：直接将内容从上游服务器流式传输到客户端

#### 软件架构

软件架构说明

通过JavaScript脚本重新定义函数pageRedirect，禁止重定向到http://melogin.cn/

#### 安装教程

```shell
 gradle buildFatJar
```

#### 使用说明

```shell
java --add-opens java.base/jdk.internal.misc=ALL-UNNAMED  -jar "fast-and-mercury-router-reverse-proxy-server-all.jar"  -p 8080 -u https://www.example.com
```

# Reverse Proxy Application

This sample shows how to write a reverse proxy application.

## Running

Execute this command to run this sample:

```bash
./gradlew run --args="-p 8080 -u https://www.example.com"
```
