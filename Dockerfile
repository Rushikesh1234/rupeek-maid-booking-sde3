# ============================================================================
# Stage 1: Build Stage (Java 17 + Maven)
# ============================================================================
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace/app

RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests -B

# ============================================================================
# Stage 2: Production Runtime Stage (Minimal JRE 17, Non-Root User)
# ============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /workspace/app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
