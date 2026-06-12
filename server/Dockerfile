# ============================================================
# VSF QC Copilot — Backend Dockerfile
# Multi-stage build: build → promptfoo → runtime
# ============================================================

# -----------------------------------------------------------
# Stage 1: Build the JAR with Maven
# -----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Cache Maven dependencies first
COPY server/.mvn   .mvn
COPY server/mvnw   mvnw
COPY server/pom.xml pom.xml
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -q

# Copy source and package (skip tests — they need Testcontainers / DB)
COPY server/src src
RUN ./mvnw package -DskipTests -B -q \
    && mv target/*.jar app.jar

# -----------------------------------------------------------
# Stage 2: Install promptfoo CLI (Node layer)
# -----------------------------------------------------------
FROM node:22-slim AS promptfoo

WORKDIR /promptfoo-runner
COPY tooling/promptfoo-runner/package.json tooling/promptfoo-runner/package-lock.json ./
RUN npm ci --omit=dev --ignore-scripts

# -----------------------------------------------------------
# Stage 3: Production runtime
# -----------------------------------------------------------
FROM eclipse-temurin:21-jre

LABEL maintainer="nghlong3004"
LABEL description="VSF QC Copilot Backend API"

WORKDIR /app

# Copy Node.js runtime from the node stage (avoids nodesource.com / extra apt)
COPY --from=promptfoo /usr/local/bin/node /usr/local/bin/node
COPY --from=promptfoo /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

# Copy the built JAR
COPY --from=builder /build/app.jar app.jar

# Copy promptfoo runner (includes node_modules/.bin/promptfoo)
COPY --from=promptfoo /promptfoo-runner ./tooling/promptfoo-runner

# Create directories for runtime data + non-root user
RUN mkdir -p /app/runs /app/exports /app/logs \
    && addgroup --system --gid 1001 vqc \
    && adduser --system --uid 1001 --ingroup vqc vqc \
    && chown -R vqc:vqc /app

USER vqc

# Expose default server port
EXPOSE 8080


# Default to prod profile
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
