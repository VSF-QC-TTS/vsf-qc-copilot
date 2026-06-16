# ============================================================
# VF QC Copilot — Backend Dockerfile
# Multi-stage build: Maven build → Promptfoo runner → runtime
# ============================================================

# -----------------------------------------------------------
# Stage 1: Build the JAR with Maven
# -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY server/.mvn .mvn
COPY server/mvnw mvnw
COPY server/pom.xml pom.xml

RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B -q

COPY server/src src

RUN ./mvnw package -DskipTests -B -q \
    && find target -maxdepth 1 -type f -name "*.jar" \
        ! -name "*sources.jar" \
        ! -name "*javadoc.jar" \
        -exec cp {} app.jar \; \
        -quit

# -----------------------------------------------------------
# Stage 2: Install pinned Promptfoo runner
# -----------------------------------------------------------
FROM node:22-bookworm-slim AS promptfoo

WORKDIR /promptfoo-runner

COPY tooling/promptfoo-runner/package.json tooling/promptfoo-runner/package-lock.json ./

RUN npm ci --omit=dev --no-audit --no-fund

# Copy runner source/config too, if any
COPY tooling/promptfoo-runner/ ./

# Optional sanity check
RUN node --version \
    && ./node_modules/.bin/promptfoo --version

# -----------------------------------------------------------
# Stage 3: Production runtime
# -----------------------------------------------------------
FROM eclipse-temurin:21-jre

LABEL maintainer="nghlong3004"
LABEL description="VF QC Copilot Backend API"

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV HOME=/app
ENV PATH="/app/tooling/promptfoo-runner/node_modules/.bin:${PATH}"

RUN mkdir -p /app/runs /app/exports /app/logs \
    && addgroup --system vqc \
    && adduser --system --ingroup vqc vqc

COPY --chown=vqc:vqc --from=builder /build/app.jar /app/app.jar
COPY --from=promptfoo /usr/local/bin/node /usr/local/bin/node
COPY --chown=vqc:vqc --from=promptfoo /promptfoo-runner /app/tooling/promptfoo-runner

# Validate Node + Promptfoo in the final runtime image
RUN node --version \
    && cd /app/tooling/promptfoo-runner \
    && ./node_modules/.bin/promptfoo --version \
    && chown -R vqc:vqc /app

USER vqc

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]