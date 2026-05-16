# syntax=docker/dockerfile:1.6

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package && \
    cp target/*.jar app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app && \
    mkdir -p /app/uploads && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
ENV SPRING_PROFILES_ACTIVE=prod
ENV UPLOAD_DIR=/app/uploads

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -q -O- http://localhost:8080/api/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
