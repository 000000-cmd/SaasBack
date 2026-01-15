# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /app

# Copiar POMs (Principal + Common + Service)
COPY pom.xml .
COPY saas-common/pom.xml saas-common/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml

# Descargar dependencias
RUN mvn -B dependency:go-offline -DskipTests || true

# Copiar código fuente
COPY saas-common/src saas-common/src
COPY gateway-service/src gateway-service/src

# Compilar
RUN echo "=== Building gateway-service ===" && \
    mvn -B -pl gateway-service -am clean package -DskipTests

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/gateway-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
