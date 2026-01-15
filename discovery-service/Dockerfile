FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

RUN apk add --no-cache bash

WORKDIR /app

COPY pom.xml .
COPY saas-common/pom.xml saas-common/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml

RUN mvn -B dependency:go-offline -DskipTests 2>&1 || true

COPY saas-common/src saas-common/src
COPY discovery-service/src discovery-service/src

RUN echo "=== Building discovery-service ===" && \
    mvn -B -pl discovery-service -am clean package -DskipTests 2>&1 | tee /tmp/build.log || \
    (tail -100 /tmp/build.log && exit 1)

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl bash \
 && addgroup -g 1001 appgroup \
 && adduser -D -u 1001 -G appgroup appuser

WORKDIR /app

COPY --from=builder /app/discovery-service/target/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8761

ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseContainerSupport"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
