# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar todo el monorepo (Maven reactor necesita módulos)
COPY pom.xml .
COPY saas-common/ saas-common/
COPY config-server/ config-server/
COPY discovery-service/ discovery-service/
COPY gateway-service/ gateway-service/
COPY auth-service/ auth-service/
COPY system-service/ system-service/

# Descargar dependencias solo del módulo
RUN mvn -B -pl config-server -am dependency:go-offline

# Compilar solo el módulo
RUN mvn -B -pl config-server -am clean package -DskipTests

# ===========================================
# RUNTIME STAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/config-server/target/*.jar app.jar

USER appuser

EXPOSE 8888

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
