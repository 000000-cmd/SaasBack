# ===========================================
# STAGE 1: BUILD ALL SERVICES
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

ENV MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /app

# Copiar TODO el monorepo
COPY pom.xml .
COPY saas-common/ saas-common/
COPY config-server/ config-server/
COPY discovery-service/ discovery-service/
COPY gateway-service/ gateway-service/
COPY auth-service/ auth-service/
COPY system-service/ system-service/

# Compilar TODO de una vez
RUN echo "=== Compilando TODOS los servicios ===" && \
    mvn clean package -DskipTests -B && \
    echo "=== Compilación completada ===" && \
    ls -la */target/*.jar

# ===========================================
# STAGE 2: CONFIG SERVER
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS config-server

RUN apk add --no-cache curl bash tzdata && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/config-server/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8888

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 3: DISCOVERY SERVICE
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS discovery-service

RUN apk add --no-cache curl bash tzdata && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/discovery-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8761

ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 4: GATEWAY SERVICE
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS gateway-service

RUN apk add --no-cache curl bash tzdata && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/gateway-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 5: AUTH SERVICE
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS auth-service

RUN apk add --no-cache curl bash tzdata && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8082

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 6: SYSTEM SERVICE
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS system-service

RUN apk add --no-cache curl bash tzdata && \
    addgroup -g 1001 appgroup && \
    adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/system-service/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser
EXPOSE 8083

ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]