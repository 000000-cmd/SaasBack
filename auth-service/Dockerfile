FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache bash
WORKDIR /app

COPY pom.xml .
COPY saas-common/pom.xml saas-common/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

RUN mvn -B dependency:go-offline -DskipTests || true

COPY saas-common/src saas-common/src
COPY auth-service/src auth-service/src

RUN echo "=== Building auth-service ===" && \
    mvn -B -pl auth-service -am clean package -DskipTests

# -------------------------
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app
COPY --from=builder /app/auth-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8082
ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

