# ===========================================
# BUILD STAGE
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar POMs
COPY pom.xml .
COPY saas-common/pom.xml saas-common/pom.xml
COPY config-server/pom.xml config-server/pom.xml

# Descargar dependencias
RUN mvn -B dependency:go-offline -DskipTests || true

# Copiar código
COPY saas-common/src saas-common/src
COPY config-server/src config-server/src

# Compilar
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

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8888

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
