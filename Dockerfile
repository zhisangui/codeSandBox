# 基础镜像
# ★上线的时候为了匹配服务器的java版本，修改为17
 FROM openjdk:8-jdk-alpine
#FROM openjdk:17-alpine
# 指定工作目录
WORKDIR /app

# 将 jar 包添加到工作目录，比如 target/zoj-backend-user-service-0.0.1-SNAPSHOT.jar
ADD target/zoj-code-sandbox-0.0.1-SNAPSHOT.jar .

# 暴露端口
EXPOSE 8081

# 启动命令
ENTRYPOINT ["java","-jar","/app/zoj-code-sandbox-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]
