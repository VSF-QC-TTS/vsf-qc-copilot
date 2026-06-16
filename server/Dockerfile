# ============================================================
# VSF QC Copilot — Backend Dockerfile
# Multi-stage build: Maven build → Promptfoo runner → runtime
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
# Stage 2: Install pinned Promptfoo runner
# -----------------------------------------------------------
FROM node:22-bookworm-slim AS promptfoo

WORKDIR /promptfoo-runner

COPY tooling/promptfoo-runner/package.json tooling/promptfoo-runner/package-lock.json ./

RUN npm ci --omit=dev --no-audit --no-fund

# -----------------------------------------------------------
# Stage 3: Production runtime
# -----------------------------------------------------------
FROM eclipse-temurin:21-jre

LABEL maintainer="nghlong3004"
LABEL description="VSF QC Copilot Backend API"

WORKDIR /app

COPY --from=builder /build/app.jar app.jar
COPY --from=promptfoo /usr/local/bin/node /usr/local/bin/node
COPY --from=promptfoo /promptfoo-runner /app/tooling/promptfoo-runner

# Default Spring profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV HOME=/app

# Create runtime directories + non-root user
RUN mkdir -p /app/runs /app/exports /app/logs \
    && addgroup --system vqc \
    && adduser --system --ingroup vqc vqc \
    && chown -R vqc:vqc /app

USER vqc

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]