FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q test package && \
    JAR_FILE="$(find target -maxdepth 1 -name '*.jar' ! -name '*original*' | head -n 1)" && \
    cp "$JAR_FILE" /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R spring:spring /app

ENV JAVA_OPTS="" \
    DB_URL="jdbc:postgresql://postgres:5432/javafx_audit" \
    DB_USER="javafx_audit" \
    DB_PASSWORD="changeme" \
    FRONTEND_ORIGIN="http://localhost:4200"

USER spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
