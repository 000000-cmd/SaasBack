# ============================================
# DOCKERFILE MULTI-STAGE - SAAS PLATFORM
# Linea Base para Microservicios
# ============================================

# ===========================================
# STAGE 1: CACHE DE DEPENDENCIAS
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS dependencies

WORKDIR /app

COPY pom.xml .
COPY saas-common/pom.xml saas-common/
COPY config-server/pom.xml config-server/
COPY discovery-service/pom.xml discovery-service/
COPY gateway-service/pom.xml gateway-service/
COPY auth-service/pom.xml auth-service/
COPY system-service/pom.xml system-service/

RUN mvn dependency:go-offline -B -q || true

# ===========================================
# STAGE 2: BUILD
# ===========================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

ENV MAVEN_OPTS="-Xmx1024m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

WORKDIR /app

COPY --from=dependencies /root/.m2 /root/.m2

COPY pom.xml .
COPY saas-common/ saas-common/
COPY config-server/ config-server/
COPY discovery-service/ discovery-service/
COPY gateway-service/ gateway-service/
COPY auth-service/ auth-service/
COPY system-service/ system-service/

RUN echo "=== Compilando TODOS los servicios ===" && \
    mvn clean package -DskipTests -B && \
    echo "=== Compilacion completada ===" && \
    ls -la */target/*.jar

# ===========================================
# STAGE 3: BASE RUNTIME IMAGE
# ===========================================
FROM eclipse-temurin:21-jre-alpine AS base-runtime

RUN apk add --no-cache curl bash tzdata && rm -rf /var/cache/apk/*

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

ENV TZ=America/Bogota
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

WORKDIR /app

# ===========================================
# STAGE 4: CONFIG SERVER
# ===========================================
FROM base-runtime AS config-server
COPY --from=builder --chown=appuser:appgroup /app/config-server/target/*.jar app.jar
USER appuser
EXPOSE 8888
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8888/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 5: DISCOVERY SERVICE
# ===========================================
FROM base-runtime AS discovery-service
COPY --from=builder --chown=appuser:appgroup /app/discovery-service/target/*.jar app.jar
USER appuser
EXPOSE 8761
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=10s --start-period=50s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 6: GATEWAY SERVICE
# ===========================================
FROM base-runtime AS gateway-service
COPY --from=builder --chown=appuser:appgroup /app/gateway-service/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 7: AUTH SERVICE
# ===========================================
FROM base-runtime AS auth-service
COPY --from=builder --chown=appuser:appgroup /app/auth-service/target/*.jar app.jar
USER appuser
EXPOSE 8082
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ===========================================
# STAGE 8: SYSTEM SERVICE
# ===========================================
FROM base-runtime AS system-service
COPY --from=builder --chown=appuser:appgroup /app/system-service/target/*.jar app.jar
USER appuser
EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]