# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /app

# ARG para determinar qué servicio construir
ARG SERVICE_NAME

# Copiar estructura completa del monorepo
COPY pom.xml .
COPY saas-common/ saas-common/
COPY config-server/ config-server/
COPY discovery-service/ discovery-service/
COPY gateway-service/ gateway-service/
COPY auth-service/ auth-service/
COPY system-service/ system-service/

# Descargar dependencias (una sola vez para todos)
RUN mvn -B dependency:go-offline -DskipTests || true

# Compilar el servicio específico con logs detallados
RUN echo "=== Building ${SERVICE_NAME} ===" && \
    mvn -B -pl ${SERVICE_NAME} -am clean package -DskipTests -X && \
    echo "=== Build completed for ${SERVICE_NAME} ===" && \
    ls -la ${SERVICE_NAME}/target/

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

ARG SERVICE_NAME
ARG SERVICE_PORT=8080

RUN apk add --no-cache curl bash && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

# Copiar el JAR compilado
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE ${SERVICE_PORT}

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
  CMD curl -f http://localhost:${SERVICE_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
