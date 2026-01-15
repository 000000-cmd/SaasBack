# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /app

# Copiar POMs necesarios
COPY pom.xml .
COPY saas-common/pom.xml saas-common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

# Pre-descargar dependencias (NO romper build)
RUN mvn -B dependency:go-offline -DskipTests || true

# Copiar código fuente
COPY saas-common/src saas-common/src
COPY auth-service/src auth-service/src

# Compilar auth-service
RUN echo "=== Building auth-service ===" && \
    mvn -B -pl auth-service -am clean package -DskipTests 2>&1 | tee /tmp/build.log || \
    (tail -100 /tmp/build.log && exit 1)

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/auth-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
