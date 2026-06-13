# ============================================================
# VSF QC Copilot — Backend Dockerfile
# Multi-stage build: build → runtime (no promptfoo)
# ============================================================

# -----------------------------------------------------------
# Stage 1: Build the JAR with Maven
# -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Cache Maven wrapper + pom first
COPY server/.mvn .mvn
COPY server/mvnw mvnw
COPY server/pom.xml pom.xml

RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B -q

COPY server/src src

RUN ./mvnw package -DskipTests -B -q \
    && mv target/*.jar app.jar

# -----------------------------------------------------------
# Stage 2: Production runtime (lightweight, no promptfoo)
# -----------------------------------------------------------
FROM eclipse-temurin:21-jre

LABEL maintainer="nghlong3004"
LABEL description="VSF QC Copilot Backend API"

WORKDIR /app

# Copy the built JAR
COPY --from=builder /build/app.jar app.jar

# Default Spring profile
ENV SPRING_PROFILES_ACTIVE=prod

# Create runtime directories + non-root user
RUN mkdir -p /app/runs /app/exports /app/logs \
    && addgroup --system vqc \
    && adduser --system --ingroup vqc vqc \
    && chown -R vqc:vqc /app

USER vqc

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]