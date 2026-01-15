# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Instalar herramientas de debugging
RUN apk add --no-cache bash tree

WORKDIR /app

# Copiar archivos de configuración Maven primero
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd 2>/dev/null || true

# Copiar POMs de todos los módulos
COPY saas-common/pom.xml saas-common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

# Debug: Ver estructura
RUN echo "=== Estructura de archivos ===" && \
    ls -la && \
    echo "=== Contenido de auth-service ===" && \
    ls -la auth-service/ 2>/dev/null || echo "No existe auth-service/"

# Descargar dependencias (permitir fallos)
RUN mvn -B dependency:go-offline -DskipTests 2>&1 || \
    echo "WARNING: dependency:go-offline falló, continuando..."

# Copiar código fuente
COPY saas-common/src saas-common/src
COPY auth-service/src auth-service/src

# Debug: Verificar que se copió el código
RUN echo "=== Verificando código fuente ===" && \
    find auth-service/src -type f -name "*.java" | head -5 || \
    echo "ERROR: No se encontró código Java en auth-service/src"

# Compilar con output detallado
RUN echo "=== Iniciando compilación ===" && \
    mvn -B -X -pl auth-service -am clean package -DskipTests 2>&1 | tee /tmp/maven-build.log || \
    (echo "=== ERROR EN COMPILACIÓN ===" && \
     tail -100 /tmp/maven-build.log && \
     exit 1)

# Verificar que el JAR se generó
RUN ls -lh auth-service/target/*.jar || \
    (echo "ERROR: JAR no generado" && exit 1)

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

# Copiar JAR con verificación
COPY --from=builder /app/auth-service/target/*.jar app.jar

RUN ls -lh app.jar && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8082

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=120s \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]