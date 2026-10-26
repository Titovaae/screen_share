# ---- сборка ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- запуск ----
FROM eclipse-temurin:17-jre
# Xvfb даёт виртуальный экран, чтобы java.awt.Robot работал внутри контейнера
RUN apt-get update && apt-get install -y --no-install-recommends \
        xvfb libxtst6 libxi6 libxrender1 libxext6 libfreetype6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/screen-server-1.0.0.jar app.jar
COPY docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENV LOG_DIR=/app/logs
VOLUME /app/logs
EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
