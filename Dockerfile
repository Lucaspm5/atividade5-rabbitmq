# ---------- Estágio 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Estágio 2: execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar

# Classe principal escolhida por serviço no docker-compose.yml
ARG MAIN_CLASS
ENV MAIN_CLASS=${MAIN_CLASS}

ENTRYPOINT ["sh", "-c", "exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dorg.slf4j.simpleLogger.defaultLogLevel=warn -cp app.jar $MAIN_CLASS"]
