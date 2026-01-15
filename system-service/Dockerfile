# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar POMs primero
COPY pom.xml .
COPY saas-common/pom.xml saas-common/
COPY system-service/pom.xml system-service/

# Descargar dependencias
RUN mvn -B -pl system-service -am dependency:go-offline || true

# Copiar código fuente
COPY saas-common/ saas-common/
COPY system-service/ system-service/

# Compilar
RUN mvn -B -pl system-service -am clean package -DskipTests

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/system-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8083

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]