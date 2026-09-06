FROM eclipse-temurin:21-jdk-alpine

# 安装 Node.js（MCP 高德地图需要）
RUN apk add --no-cache nodejs npm

# 创建应用目录
WORKDIR /app

# 复制 JAR 包
COPY target/tree-ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8123

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]