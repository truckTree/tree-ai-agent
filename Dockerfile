FROM quay.io/lib/eclipse-temurin:21

# 安装 Node.js 和 npm（MCP 高德地图需要 npx）
RUN apt update ; apt install nodejs npm -y

# 预装高德 MCP 服务，避免运行时 npx 现下载导致初始化超时
RUN npm install -g @amap/amap-maps-mcp-server

# 创建应用目录
WORKDIR /app

# 复制 JAR 包
COPY target/tree-ai-agent-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8123

# 启动
ENTRYPOINT ["java", "-jar", "app.jar"]