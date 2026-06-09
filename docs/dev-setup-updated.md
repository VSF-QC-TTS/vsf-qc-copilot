# 07. Development Setup — VSF QC Copilot

> Implementation file for local development and VPS demo deployment.  
> Use this document when setting up the repository, running services, configuring environment variables, running promptfoo, and troubleshooting deployment issues.

---

## 1. Purpose

This document answers:

```text
How should the repository be structured?
How do I run the project in development?
How should client, server, database, Redis, and promptfoo run together?
What should be installed on the developer machine?
What should be installed on the VPS host?
How should Docker Compose package the demo deployment?
```

This file should be updated whenever the development or deployment setup changes.

---

## 2. Key Decisions

### 2.1 Repository naming

Use these names consistently:

```text
client = React + Vite web application
server = Spring Boot API application
infra  = Docker Compose, Nginx config, deployment scripts, runtime setup
```

Do not use `frontend` / `backend` as top-level package or folder names in this project.

### 2.2 Spring profiles

The server uses only two application profiles:

```text
dev
prod
```

Allowed application config files:

```text
application.yml
application-dev.yml
application-prod.yml
```

Do not create these runtime profiles for the MVP:

```text
local
worker
demo
```

Worker behavior should be controlled by environment variables, not by a separate Spring profile.

### 2.3 Server package root

The server parent package is:

```text
me.nghlong3004.vqc.api
```

Meaning:

```text
me.nghlong3004 = owner namespace
vqc           = short name for VSF QC Copilot
api           = server-side application package
```

### 2.4 Package style

Use feature-first package structure:

```text
<feature>/controller
<feature>/service
<feature>/service/impl
<feature>/repository
<feature>/entity
<feature>/mapper
<feature>/request
<feature>/response
```

### 2.5 Promptfoo deployment decision

Use promptfoo as a CLI evaluation engine.

For Docker/VPS demo:

```text
Do not install Node.js, npm, npx, or promptfoo directly on the VPS host.
Install Node.js + a pinned promptfoo CLI version inside the server Docker image.
The server worker calls the promptfoo command from inside the server container.
```

For local development:

```text
Use project-local promptfoo under tooling/promptfoo-runner, or use npx only for quick one-off checks.
Do not vendor the promptfoo source repository into the app repo unless you intentionally maintain a custom fork.
```

---

## 3. Recommended Stack

| Layer | Tool | Purpose |
|---|---|---|
| Server | Spring Boot + Java 21 | REST API, authentication, orchestration, worker, export |
| Client | React + Vite | QC workflow UI |
| Database | PostgreSQL | Durable source of truth |
| Queue | Redis | Job queue / progress coordination |
| Evaluation Engine | promptfoo CLI | Evaluation runner |
| Runtime | Docker / Docker Compose | Containerized services |
| Migration | Flyway | Database schema migration |
| Reverse Proxy | Nginx inside client image | Serve React build and proxy `/api` to server |

---

## 4. Machine Requirements

## 4.1 Developer machine

Install these locally:

```text
Java 21
Maven Wrapper support
Node.js compatible with promptfoo
npm
Docker Desktop or Docker Engine
Docker Compose plugin
Git
```

Recommended checks:

```bash
java -version
node -v
npm -v
docker version
docker compose version
git --version
```

Promptfoo compatibility should be checked with the pinned version used by the project.

---

## 4.2 VPS host

The VPS host should only need:

```text
Docker Engine
Docker Compose plugin
Git, only if you pull source code directly on the VPS
```

The VPS host should not need:

```text
Java installed on host
Node.js installed on host
npm or npx installed on host
promptfoo installed on host
PostgreSQL installed on host
Redis installed on host
Nginx installed on host
```

Reason:

```text
Java runs inside the server container.
Node.js and promptfoo run inside the server container.
The client is built inside the client Docker image and served by Nginx inside that image.
PostgreSQL and Redis run as Docker Compose services.
```

---

## 5. Repository Structure

```text
vsf-qc-copilot/
├── server/
│   ├── Dockerfile
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   ├── src/main/java/me/nghlong3004/vqc/api/...
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/
│   └── src/test/java/me/nghlong3004/vqc/api/...
│
├── client/
│   ├── Dockerfile
│   ├── package.json
│   ├── package-lock.json
│   ├── index.html
│   └── src/
│       ├── api/
│       ├── components/
│       ├── pages/
│       ├── routes/
│       └── main.tsx
│
├── docs/
│   ├── 02-architecture.final.md
│   ├── 03-delivery-plan.final.md
│   ├── 06-work-assignment-long-truong.final.md
│   ├── 07-dev-setup.final.md
│   ├── 08-api-contract-mvp.final.md
│   └── 09-db-schema-mvp.final.md
│
├── infra/
│   ├── docker-compose.dev.yml
│   ├── docker-compose.prod.yml
│   ├── nginx/
│   │   └── default.conf
│   └── scripts/
│       └── run-promptfoo.sh
│
├── tooling/
│   └── promptfoo-runner/
│       ├── package.json
│       └── package-lock.json
│
├── runs/
│   └── .gitkeep
│
├── exports/
│   └── .gitkeep
│
├── .env.example
├── .gitignore
└── README.md
```

Notes:

```text
runs/ stores generated promptfoo config, tests, and result files.
exports/ stores generated Excel/JSON files.
tooling/promptfoo-runner/ is for project-local promptfoo during development.
Do not place generated run/export files in Git.
```

---

## 6. Server Package Convention

Parent package:

```text
me.nghlong3004.vqc.api
```

Recommended package tree:

```text
me.nghlong3004.vqc.api
├── auth
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── user
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── project
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── targetconnector
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── requirement
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── dataset
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── rubric
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── evaluation
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── job
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── review
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── export
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── repository
│   ├── entity
│   ├── mapper
│   ├── request
│   └── response
│
├── mockchatbot
│   ├── controller
│   ├── service
│   │   └── impl
│   ├── request
│   └── response
│
├── integration
│   ├── promptfoo
│   ├── llm
│   └── targetapi
│
├── worker
│
└── shared
    ├── config
    ├── security
    ├── exception
    ├── enums
    ├── pagination
    ├── problem
    └── util
```

Rules:

```text
Keep feature-specific code inside the feature package.
Keep cross-cutting code inside shared.
Keep external system adapters inside integration.
Do not expose internal BIGINT id in response DTOs.
Expose publicId only in API response and API URL.
```

Example class names:

```text
ProjectController
ProjectService
ProjectServiceImpl
ProjectRepository
ProjectEntity
ProjectMapper
CreateProjectRequest
ProjectResponse
```

Target connector example:

```text
TargetConnectorController
TargetConnectorService
TargetConnectorServiceImpl
TargetConnectorRepository
TargetConnectorEntity
ConnectorSecretEntity
TargetConnectorMapper
CreateTargetConnectorRequest
TargetConnectorResponse
```

Promptfoo integration classes:

```text
PromptfooConfigGenerator
PromptfooCommandExecutor
PromptfooResultParser
PromptfooRunDirectoryResolver
PromptfooExecutionException
```

---

## 7. Environment Variables

Create `.env` from `.env.example`.

### 7.1 `.env.example`

```bash
# App
APP_ENV=dev
APP_BASE_URL=http://localhost:8080
CLIENT_BASE_URL=http://localhost:5173

# Server
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
API_ENABLED=true
WORKER_ENABLED=true
JWT_SECRET=change-me-dev-secret
JWT_ACCESS_TOKEN_TTL_MINUTES=120
JAVA_OPTS=-Xms256m -Xmx768m

# PostgreSQL
POSTGRES_DB=vsf_qc_copilot
POSTGRES_USER=vsf_qc
POSTGRES_PASSWORD=vsf_qc_password
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vsf_qc_copilot
SPRING_DATASOURCE_USERNAME=vsf_qc
SPRING_DATASOURCE_PASSWORD=vsf_qc_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Promptfoo
PROMPTFOO_VERSION=0.121.12
PROMPTFOO_WORK_DIR=./runs
PROMPTFOO_COMMAND=./infra/scripts/run-promptfoo.sh
PROMPTFOO_MAX_CONCURRENCY=1

# Export
EXPORT_DIR=./exports

# LLM Providers for judge/generation
OPENAI_API_KEY=
GEMINI_API_KEY=

# Mock target API
MOCK_CHATBOT_ENABLED=true
```

Do not commit real `.env` files.

### 7.2 Production overrides

For Docker/VPS deployment, use these important differences:

```bash
APP_ENV=prod
SPRING_PROFILES_ACTIVE=prod
CLIENT_BASE_URL=https://your-domain.example
APP_BASE_URL=https://your-domain.example
JWT_SECRET=replace-with-long-random-secret
PROMPTFOO_COMMAND=promptfoo
PROMPTFOO_WORK_DIR=/app/runs
PROMPTFOO_MAX_CONCURRENCY=1
EXPORT_DIR=/app/exports
```

Important:

```text
In Docker/VPS deployment, promptfoo must already exist inside the server image.
Do not rely on npx downloading promptfoo during a live evaluation job.
```

---

## 8. Docker Compose for Development Dependencies

Development Compose only starts dependencies.
The server and client usually run on the developer machine for fast reload.

Create `infra/docker-compose.dev.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: vqc-postgres-dev
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-vsf_qc_copilot}
      POSTGRES_USER: ${POSTGRES_USER:-vsf_qc}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-vsf_qc_password}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-vsf_qc} -d ${POSTGRES_DB:-vsf_qc_copilot}"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: vqc-redis-dev
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  postgres_data:
  redis_data:
```

Start dependencies from project root:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d
```

Stop dependencies:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml down
```

Reset development data:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml down -v
```

---

## 9. Server Development Run

Recommended: run server commands from project root so relative paths like `./runs` and `./infra/scripts/run-promptfoo.sh` are stable.

```bash
./server/mvnw -f server/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Alternative:

```bash
./server/mvnw -f server/pom.xml clean package
java -jar server/target/*.jar --spring.profiles.active=dev
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

Worker behavior in development:

```text
API and worker can run in the same Spring Boot process for MVP.
Set WORKER_ENABLED=true to consume jobs.
Set WORKER_ENABLED=false if you only want to test REST APIs.
```

---

## 10. Client Development Run

From `client/`:

```bash
npm install
npm run dev
```

Default URL:

```text
http://localhost:5173
```

Client environment example:

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Production note:

```text
Do not run npm run dev on the VPS.
The client is built inside the client Docker image and served by Nginx in that image.
```

---

## 11. Promptfoo Setup

## 11.1 Runtime model

For this MVP:

```text
Spring worker
→ Generate runs/{runPublicId}/promptfooconfig.yaml
→ Generate runs/{runPublicId}/tests.json
→ Execute promptfoo CLI
→ Save runs/{runPublicId}/results.json
→ Parse results
→ Save evaluation results into PostgreSQL
```

promptfoo is not a permanent service in this MVP.
It only runs when an evaluation job starts.

---

## 11.2 Local promptfoo setup

For reproducible local development, install promptfoo under `tooling/promptfoo-runner`:

```bash
mkdir -p tooling/promptfoo-runner
cd tooling/promptfoo-runner
npm init -y
npm install promptfoo@0.121.12
./node_modules/.bin/promptfoo --version
```

Create `infra/scripts/run-promptfoo.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PROMPTFOO_BIN="$ROOT_DIR/tooling/promptfoo-runner/node_modules/.bin/promptfoo"

if [ ! -x "$PROMPTFOO_BIN" ]; then
  echo "promptfoo is not installed at $PROMPTFOO_BIN" >&2
  echo "Run: cd tooling/promptfoo-runner && npm install" >&2
  exit 1
fi

exec "$PROMPTFOO_BIN" "$@"
```

Make it executable:

```bash
chmod +x infra/scripts/run-promptfoo.sh
```

Local check from project root:

```bash
./infra/scripts/run-promptfoo.sh --version
```

Optional one-off check only:

```bash
npx -y promptfoo@0.121.12 --version
```

Do not use `promptfoo@latest` for repeatable development or demo deployment.

---

## 11.3 Docker/VPS promptfoo setup

For Docker/VPS deployment:

```text
promptfoo is installed during server image build.
The worker calls PROMPTFOO_COMMAND=promptfoo.
The VPS host does not install Node.js, npm, npx, or promptfoo.
```

Production command executed inside the server container:

```bash
promptfoo eval \
  -c /app/runs/{runPublicId}/promptfooconfig.yaml \
  -o /app/runs/{runPublicId}/results.json
```

Do not use this in production/demo job execution:

```bash
npx -y promptfoo@latest eval ...
```

Reasons:

```text
It may download dependencies during a user-triggered job.
It may pull a different latest version.
It may fail if the container temporarily cannot reach npm.
It slows down the first evaluation run.
```

---

## 12. Docker Files

## 12.1 `server/Dockerfile`

The server image contains Java runtime, Node.js, and pinned promptfoo CLI.

```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace/server

COPY server/.mvn .mvn
COPY server/mvnw server/pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline

COPY server/src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

ARG PROMPTFOO_VERSION=0.121.12

RUN apt-get update \
    && apt-get install -y curl ca-certificates gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g promptfoo@${PROMPTFOO_VERSION} \
    && promptfoo --version \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/server/target/*.jar app.jar

RUN mkdir -p /app/runs /app/exports

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Notes:

```text
promptfoo is installed once during image build.
The VPS host does not need Node.js or npx.
The running server container already has the promptfoo command.
```

---

## 12.2 `client/Dockerfile`

The client image builds React and serves static files with Nginx.

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app

COPY client/package*.json ./
RUN npm ci

COPY client/ ./

ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

FROM nginx:1.27-alpine

COPY infra/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
```

Important:

```text
This avoids building client/dist on the VPS host.
The VPS host does not need Node.js or npm.
```

---

## 12.3 `infra/nginx/default.conf`

```nginx
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://server:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /mock-chatbot/ {
        proxy_pass http://server:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 13. Docker Compose for VPS Deployment

Production deployment uses Docker Compose to run all runtime services.
The VPS host only needs Docker and Docker Compose.

Create `infra/docker-compose.prod.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: vqc-postgres-prod
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: vqc-redis-prod
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
    restart: unless-stopped

  server:
    build:
      context: ..
      dockerfile: server/Dockerfile
      args:
        PROMPTFOO_VERSION: ${PROMPTFOO_VERSION:-0.121.12}
    container_name: vqc-server-prod
    environment:
      APP_ENV: prod
      SPRING_PROFILES_ACTIVE: prod
      API_ENABLED: "true"
      WORKER_ENABLED: "true"
      SERVER_PORT: 8080
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx768m}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      PROMPTFOO_COMMAND: promptfoo
      PROMPTFOO_WORK_DIR: /app/runs
      PROMPTFOO_MAX_CONCURRENCY: ${PROMPTFOO_MAX_CONCURRENCY:-1}
      EXPORT_DIR: /app/exports
      OPENAI_API_KEY: ${OPENAI_API_KEY:-}
      GEMINI_API_KEY: ${GEMINI_API_KEY:-}
      MOCK_CHATBOT_ENABLED: ${MOCK_CHATBOT_ENABLED:-true}
    volumes:
      - runs_data:/app/runs
      - exports_data:/app/exports
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped

  client:
    build:
      context: ..
      dockerfile: client/Dockerfile
      args:
        VITE_API_BASE_URL: /api/v1
    container_name: vqc-client-prod
    ports:
      - "80:80"
    depends_on:
      - server
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:
  runs_data:
  exports_data:
```

Deploy from project root:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml up -d --build
```

Check services:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml ps
```

Check promptfoo inside the server container:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml exec server promptfoo --version
```

Important:

```text
Do not run npm run dev on the VPS.
Do not install promptfoo on the VPS host.
Do not run npx on the VPS host for production evaluation jobs.
Do not mount client/dist from the host in production.
The client image builds and serves the React app itself.
The server image contains Java, Node.js, npm, and promptfoo.
```

---

## 14. Local Development Flow

```text
1. Start PostgreSQL and Redis with infra/docker-compose.dev.yml
2. Start server with dev profile
3. Start client with npm run dev
4. Login as demo user
5. Create project
6. Create mock target API connector
7. Create sample dataset
8. Create rubric
9. Run evaluation
10. Wait for job completed
11. Open result dashboard
12. Save QC review decision
13. Export Excel
14. Export JSON
```

Start dependencies:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d
```

Start server:

```bash
./server/mvnw -f server/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

Start client:

```bash
cd client
npm install
npm run dev
```

---

## 15. VPS Demo Flow

Recommended VPS shape for a 4GB RAM demo:

```text
[VPS Host]
  └── Docker Compose
      ├── client container
      │   ├── Nginx
      │   ├── React static files
      │   └── Reverse proxy to server
      │
      ├── server container
      │   ├── Java runtime
      │   ├── Node.js runtime
      │   ├── promptfoo CLI
      │   ├── REST API
      │   ├── Worker
      │   ├── Mock target API endpoint
      │   └── Export generator
      │
      ├── PostgreSQL container
      └── Redis container
```

For 4GB RAM:

```text
Run server API and worker in one process for MVP.
Set PROMPTFOO_MAX_CONCURRENCY=1.
Use small demo dataset: 30–80 test cases.
Avoid running multiple evaluation jobs at the same time.
Avoid self-hosted promptfoo UI.
Avoid building promptfoo source on the VPS.
Avoid running client dev server on the VPS.
```

---

## 16. Demo Seed Data

MVP should seed at least:

```text
1 demo user
1 demo project optional
1 mock target API connector optional
1 sample dataset optional
1 sample rubric optional
```

Suggested demo user:

```text
email: qc_demo@local.test
password: password123
roleCode: QC_MEMBER
```

Use this only for development/demo data.

---

## 17. Useful Local API Calls

Login:

```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"email":"qc_demo@local.test","password":"password123"}'
```

Health:

```bash
curl http://localhost:8080/actuator/health
```

Mock target API:

```bash
curl -X POST http://localhost:8080/mock-chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"How many steps did I walk today?","context":{"steps":8200}}'
```

Promptfoo version in local development:

```bash
./infra/scripts/run-promptfoo.sh --version
```

Promptfoo version in Docker/VPS:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml exec server promptfoo --version
```

---

## 18. Git Ignore Rules

Add to `.gitignore`:

```gitignore
# environment
.env
.env.*
!.env.example

# server
server/target/

# client
client/node_modules/
client/dist/

# promptfoo runner dependencies
tooling/promptfoo-runner/node_modules/

# runtime files
runs/*
!runs/.gitkeep
exports/*
!exports/.gitkeep

# logs
*.log

# IDE
.idea/
.vscode/
```

---

## 19. Branch and Commit Convention

Suggested branch naming:

```text
feature/server-auth-project
feature/server-target-connector
feature/server-promptfoo-runner
feature/client-dashboard
fix/export-missing-fields
```

Suggested commit style:

```text
feat: add project CRUD API
feat: add target api connector entity
feat: add promptfoo command executor
fix: mask authorization header in connector response
docs: add development setup
```

---

## 20. Troubleshooting

### 20.1 PostgreSQL port already used in development

Check:

```bash
netstat -ano | findstr 5432
```

Fix:

```text
Stop local PostgreSQL service or change compose port mapping.
```

### 20.2 Redis port already used in development

Check:

```bash
netstat -ano | findstr 6379
```

Fix:

```text
Stop existing Redis or change compose port mapping.
```

### 20.3 Flyway migration fails

Common causes:

```text
SQL syntax error
Table already exists after manual DB changes
Enum value mismatch
Database was partially migrated
```

Development reset:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml down -v
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d
```

### 20.4 Server starts with wrong profile

Check:

```bash
echo $SPRING_PROFILES_ACTIVE
```

Expected:

```text
dev or prod only
```

Fix:

```bash
SPRING_PROFILES_ACTIVE=dev
```

or run explicitly:

```bash
./server/mvnw -f server/pom.xml spring-boot:run -Dspring-boot.run.profiles=dev
```

### 20.5 Promptfoo command fails locally

Check:

```bash
node -v
./infra/scripts/run-promptfoo.sh --version
```

Common causes:

```text
tooling/promptfoo-runner/node_modules is missing
promptfoo version mismatch
PROMPTFOO_COMMAND path is wrong
Generated promptfooconfig.yaml path is wrong
Generated config is invalid
```

Fix:

```bash
cd tooling/promptfoo-runner
npm install
./node_modules/.bin/promptfoo --version
```

### 20.6 Promptfoo command fails in Docker/VPS

Check:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml exec server promptfoo --version
docker compose --env-file .env -f infra/docker-compose.prod.yml exec server node -v
```

Common causes:

```text
Node.js was not installed during server image build
promptfoo was not installed during server image build
PROMPTFOO_COMMAND is not set to promptfoo
Generated config path is wrong inside /app/runs
```

Fix:

```bash
docker compose --env-file .env -f infra/docker-compose.prod.yml build --no-cache server
docker compose --env-file .env -f infra/docker-compose.prod.yml up -d server
docker compose --env-file .env -f infra/docker-compose.prod.yml exec server promptfoo --version
```

Do not fix Docker/VPS promptfoo issues by installing Node.js or promptfoo directly on the VPS host.
Fix the server Docker image instead.

### 20.7 Worker does not consume jobs

Check:

```text
WORKER_ENABLED=true
Redis connection is correct
Job was inserted into Redis queue
Job row exists in PostgreSQL
Job status is PENDING
```

### 20.8 Evaluation run starts but no results are saved

Check:

```text
runs/{runPublicId}/promptfooconfig.yaml exists
runs/{runPublicId}/tests.json exists
promptfoo command exits with code 0
runs/{runPublicId}/results.json exists
Result parser supports the actual promptfoo output format
Server has DB write permission
```

### 20.9 VPS runs out of memory during demo

Check:

```bash
free -h
docker stats
ps aux --sort=-%mem | head
```

Fix:

```text
Set PROMPTFOO_MAX_CONCURRENCY=1
Use smaller dataset
Stop unused services
Use one server process for API and worker during demo
Avoid self-hosted promptfoo UI
Do not run client dev server on VPS
```

### 20.10 Export file is not downloadable

Check:

```text
EXPORT_DIR exists
Server has write permission
export_files.file_path is correct
Server download endpoint can access the file
```

---

## 21. Development Setup Definition of Done

Development setup is complete when:

```text
[ ] PostgreSQL runs through infra/docker-compose.dev.yml
[ ] Redis runs through infra/docker-compose.dev.yml
[ ] Server runs with dev profile
[ ] Client runs with npm run dev
[ ] Flyway migrations pass
[ ] Demo user can login
[ ] Mock target API endpoint returns answer
[ ] Local promptfoo wrapper version command works
[ ] A manual promptfoo dry run can write results.json
[ ] runs/ and exports/ folders exist
[ ] Server package root is me.nghlong3004.vqc.api
[ ] Feature packages follow <feature>/controller/service/service/impl/repository/entity/mapper/request/response
```

---

## 22. VPS Demo Definition of Done

VPS demo setup is complete when:

```text
[ ] VPS host has Docker and Docker Compose only
[ ] Client image builds React and serves static files through Nginx
[ ] Server image contains Java, Node.js, and promptfoo
[ ] Server runs with prod profile
[ ] PostgreSQL and Redis are healthy
[ ] Server API is reachable through client/Nginx proxy
[ ] Demo user can login
[ ] Worker can execute promptfoo --version inside the server container
[ ] Evaluation run with 30–80 cases completes
[ ] Result dashboard loads saved results from PostgreSQL
[ ] QC review decision can be saved
[ ] Excel/JSON export can be downloaded
[ ] Memory stays stable during one complete evaluation run
```
