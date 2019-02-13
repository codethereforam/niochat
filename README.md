# niochat

> 也许你我时常出现在彼此梦里

> 可醒来后又要重新调整距离

> 最难忍受不能拥有共同的温柔

> 心中默默祈祷上帝保佑

## 项目介绍
Java实现的端到端加密的聊天室，包括NIO版本和Netty版本

### 组织结构
```
├── common -- 常量
├── netty
│   ├── client -- Netty实现的客户端
│   └── server -- Netty实现的服务器
├── nio
│   ├── client -- NIO实现的客户端
│   └── server -- NIO实现的服务器
├── tool -- 日志解密工具
└── util -- 工具类
```

### 模块
1. Netty实现的聊天客户端和服务器
1. NIO实现的聊天客户端和服务器
1. 日志解密工具

### 打包文件
- netty-chat-server.jar: Netty实现的服务器
- netty-chat-client.jar: Netty实现的客户端
- chat-server.jar: NIO实现的服务器
- chat-client.jar: NIO实现的客户端
- decrypt-tool.jar: 日志解密工具

## 功能及特性
1. 聊天
1. 安全，端到端加密

## 项目运行
1. 下载：git clone
1. 修改配置
    - 修改`ChatClient.java`、`NettyClientHandler.java`和`DecryptTool.java`中的密钥，保持一致
    - 修改`ChatClient.java`和`NettyChatClient.java`中的服务器IP
1. 打包：mvn clean package
1. 复制相关jar包到服务器：scp chat-server.jar、netty-chat-server.jar和decrypt-tool.jar
1. 启动服务器：nohup java -jar chat-server.jar(netty-chat-server.jar) > chat.log 2>&1 &
1. 查看服务器日志：tail -20f chat.log
1. 运行多个客户端：java -jar chat-client.jar(netty-chat-client.jar)
1. 解密聊天记录：java -jar decrypt-tool.jar chat.log

## 运行截图

![niochat](/images/niochat1.png)

## 许可证

[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)
