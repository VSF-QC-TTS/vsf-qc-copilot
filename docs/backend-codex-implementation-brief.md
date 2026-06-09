# Backend Codex Implementation Brief — VSF QC Copilot

> Single-file backend summary generated from:
>
> - `Development Setup — VSF QC Copilot`
> - `API Contract MVP — VSF QC Copilot`
> - `Database Schema MVP — VSF QC Copilot`
>
> Purpose: give Codex enough context to implement the Spring Boot backend without reading the full docs repeatedly.

---

## 0. Product Context

VSF QC Copilot is an internal platform for evaluating chatbot / LLM API answers.

MVP backend flow:

```text
Login
→ Project
→ Dynamic Target API Connector
→ Requirement
→ Dataset / Test Cases
→ Rubric / Criteria
→ Evaluation Run / Job
→ Evaluation Results
→ QC Review Decision
→ Export Files
```

The backend owns:

```text
REST API
Authentication
Database persistence
Target API connector configuration
Secret handling
Promptfoo orchestration
Async job state
Evaluation result parsing
QC review persistence
Export metadata / download
Mock chatbot API for demo
```

Promptfoo is used as a CLI evaluation engine, not as a permanent service.

---

## 1. Repository and Runtime Decisions

### 1.1 Repository folders

Use these top-level names only:

```text
client = React + Vite web application
server = Spring Boot API application
infra  = Docker Compose, Nginx config, deployment scripts, runtime setup
```

Do not use `frontend` / `backend` as top-level folder names.

### 1.2 Spring profiles

Allowed profiles:

```text
dev
prod
```

Allowed config files:

```text
application.yml
application-dev.yml
application-prod.yml
```

Do not create MVP profiles like:

```text
local
worker
demo
```

Worker behavior should be controlled by environment variables:

```text
API_ENABLED=true
WORKER_ENABLED=true
```

### 1.3 Server package root

All backend Java code must live under:

```text
me.nghlong3004.vqc.api
```

Meaning:

```text
me.nghlong3004 = owner namespace
vqc           = VSF QC Copilot short name
api           = server-side application package
```

---

## 2. Mandatory Java File Header Rule

Every Java class / record / interface / enum created for the backend must include this class-level header.

Use this pattern:

```java
package me.nghlong3004.vqc.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since 6/9/2026
 */
@SpringBootApplication
public class ServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ServerApplication.class, args);
  }
}
```

For other files, keep the same header format and place it immediately before the class / record / interface / enum declaration:

```java
/**
 * @author nghlong3004 (Long Nguyen Hoang)
 * @since {DATE_TIME}
 */
```

Rules:

```text
Do not add random generated comments.
Do not add license text unless explicitly requested.
Do not expose internal BIGINT id in any response class.
Prefer records for simple request/response DTOs.
```

---

## 3. Backend Package Convention

Use feature-first package structure.

For each feature:

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

Recommended package tree:

```text
me.nghlong3004.vqc.api
├── auth
├── user
├── project
├── targetconnector
├── requirement
├── dataset
├── rubric
├── evaluation
├── job
├── review
├── export
├── mockchatbot
├── integration
│   ├── promptfoo
│   ├── llm
│   └── targetapi
├── worker
└── shared
    ├── config
    ├── security
    ├── exception
    ├── enums
    ├── pagination
    ├── problem
    └── util
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
ConnectorSecretRepository
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

Rules:

```text
Feature-specific logic stays inside feature package.
Cross-cutting code stays inside shared.
External adapters stay inside integration.
Worker orchestration can call feature services but should not bypass business rules.
```

---

## 4. Global API Rules

Base path:

```text
/api/v1
```

Transport:

```text
JSON over HTTP
UTF-8
```

Authentication:

```text
Authorization: Bearer {accessToken}
```

Response naming:

```text
API JSON fields use camelCase.
Database columns use snake_case.
```

Timestamp format:

```text
ISO-8601 with timezone
Example: 2026-06-08T10:30:00+07:00
```

Recommended Java types:

```text
OffsetDateTime for API DTOs
Instant or OffsetDateTime for persistence
```

Pagination response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

Problem Details error format:

```json
{
  "type": "https://vqc.nghlong3004.me/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Request body contains invalid fields.",
  "instance": "/api/v1/projects",
  "errors": [
    {
      "field": "name",
      "message": "Project name is required."
    }
  ]
}
```

Common status codes:

```text
200 Success
201 Resource created
202 Long-running job accepted
204 Success with no body
400 Invalid request
401 Not authenticated
403 Not allowed
404 Resource not found
409 Conflict / invalid state transition
422 Business validation failed
500 Unexpected server error
```

---

## 5. ID and Mapping Rules

### 5.1 Database identity

Every main table must use:

```sql
id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY
public_id UUID NOT NULL DEFAULT gen_random_uuid()
```

Enable UUID generation:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### 5.2 API identity

API must expose only:

```text
publicId
```

API must never expose:

```text
id
```

Relationship fields also use public IDs:

```json
{
  "projectPublicId": "project-uuid",
  "datasetPublicId": "dataset-uuid"
}
```

### 5.3 Service mapping flow

```text
1. Controller receives publicId from URL.
2. Service finds entity by public_id.
3. Service uses internal id for FK writes and joins.
4. Mapper converts entity.publicId to DTO.publicId.
5. Internal id stays inside backend only.
```

Repository examples:

```java
Optional<ProjectEntity> findByPublicId(UUID publicId);

Optional<TargetConnectorEntity> findByPublicIdAndProjectId(
    UUID publicId,
    Long projectId
);
```

---

## 6. Security and Secret Rules

Secrets must never be returned raw in API responses.

Use placeholders in connector config:

```json
{
  "Authorization": "Bearer {{secret:CHATBOT_API_TOKEN}}"
}
```

Return masked values only:

```json
{
  "secretKey": "CHATBOT_API_TOKEN",
  "maskedValue": "ogw_live_****f7"
}
```

Never log:

```text
Authorization header
API key values
Bearer token values
Raw connector secret values
Fully rendered target API request if it contains secrets
```

Storage rules:

```text
Do not store raw tokens in headers_json.
Do not store raw tokens in body_template_json.
Do not store raw tokens in auth_config_json.
Store encrypted secret values in connector_secrets.encrypted_value.
Store safe display string in connector_secrets.masked_value.
```

---

## 7. Database Tables Summary

### 7.1 Main table list

```text
users
projects
target_api_connectors
connector_secrets
business_requirements
datasets
test_cases
rubrics
rubric_versions
rubric_criteria
jobs
job_events
evaluation_runs
evaluation_results
review_decisions
export_files
```

### 7.2 Entity relationship overview

```text
users create projects, connectors, requirements, datasets, rubrics, jobs

projects
  → target_api_connectors
  → business_requirements
  → datasets
  → rubrics
  → jobs
  → evaluation_runs

target_api_connectors
  → connector_secrets
  → evaluation_runs

datasets
  → test_cases
  → evaluation_runs

rubrics
  → rubric_versions
  → rubric_criteria
  → evaluation_runs

jobs
  → job_events
  → evaluation_runs / export_files

evaluation_runs
  → evaluation_results
  → export_files

evaluation_results
  → review_decisions
```

### 7.3 Table implementation notes

#### `users`

Purpose: simple MVP users.

Required fields:

```text
id
public_id
username
password_hash
display_name
status
last_login_at
created_at
updated_at
```

Constraints / indexes:

```text
unique public_id
unique username
index status
```

MVP seed:

```text
username: qc_demo
password: password123
status: ACTIVE
```

API login uses `email` in request contract, but DB can store it as `username` for MVP if consistent mapping is implemented.

#### `projects`

Purpose: evaluation scope.

Required fields:

```text
id
public_id
name
description
evaluation_scope
retention_days
status
created_by
archived_at
created_at
updated_at
```

Status:

```text
ACTIVE
ARCHIVED
```

Delete API should archive, not hard-delete.

#### `target_api_connectors`

Purpose: dynamic configuration for target chatbot/API being evaluated.

Important: table name must be exactly:

```text
target_api_connectors
```

Public API resource path must be:

```text
target-api-connectors
```

Core fields:

```text
id
public_id
project_id
name
description
raw_curl
protocol
method
base_url
path
url
headers_json
query_params_json
path_params_json
body_type
body_template_json
body_template_text
auth_type
auth_config_json
secret_refs_json
is_streaming
streaming_type
streaming_event_selector
response_selector
response_format
timeout_seconds
retry_count
active
created_by
created_at
updated_at
```

MVP defaults:

```text
protocol = HTTP
body_type = RAW_JSON
auth_type = NONE unless provided
response_format = JSON
response_selector = $.answer
timeout_seconds = 60
retry_count = 1
active = true
```

#### `connector_secrets`

Purpose: encrypted secret values for connectors.

Core fields:

```text
id
public_id
connector_id
secret_key
encrypted_value
masked_value
description
created_by
created_at
updated_at
```

Rules:

```text
unique public_id
unique connector_id + secret_key
encrypted_value is never returned
masked_value can be returned
```

#### `business_requirements`

Purpose: free-text business requirement.

Core fields:

```text
id
public_id
project_id
content
version
status
created_by
created_at
updated_at
```

Status:

```text
ACTIVE
ARCHIVED
```

#### `datasets`

Purpose: dataset versions.

Core fields:

```text
id
public_id
project_id
requirement_id
name
description
version
source_type
status
created_by
approved_by
approved_at
created_at
updated_at
```

Source types:

```text
MANUAL
IMPORTED_EXCEL
SAMPLE_DEMO
GENERATED
```

Status:

```text
DRAFT
APPROVED
ARCHIVED
```

Rules:

```text
Only APPROVED datasets should be used for official evaluation runs.
A dataset with 0 active test cases cannot be approved.
MVP should warn or reject if active test cases exceed 100.
Recommended demo size: 30–80 cases.
```

#### `test_cases`

Purpose: individual test case.

Core fields:

```text
id
public_id
dataset_id
external_id
question
precondition JSONB
ground_truth
metadata_json JSONB
status
sort_order
created_at
updated_at
```

Status:

```text
ACTIVE
INACTIVE
```

#### `rubrics`

Purpose: rubric identity.

Core fields:

```text
id
public_id
project_id
name
description
current_version
created_by
created_at
updated_at
```

#### `rubric_versions`

Purpose: versioned rubric snapshot.

Core fields:

```text
id
public_id
rubric_id
version
status
created_by
created_at
published_at
```

Status:

```text
DRAFT
PUBLISHED
ARCHIVED
```

#### `rubric_criteria`

Purpose: dynamic judge criteria.

Core fields:

```text
id
public_id
rubric_version_id
name
description
weight
pass_condition
fail_condition
judge_instruction
metric_key
is_critical
sort_order
created_at
updated_at
```

#### `jobs`

Purpose: async job state.

Core fields:

```text
id
public_id
job_type
status
resource_type
resource_id
project_id
created_by
progress_current
progress_total
error_message
retry_count
max_retries
created_at
started_at
completed_at
updated_at
```

Job types:

```text
DATASET_GENERATION
EVALUATION_RUN
EXPORT_EXCEL
EXPORT_JSON
CONNECTOR_TEST
```

Job status:

```text
PENDING
RUNNING
COMPLETED
FAILED
CANCELLED
```

#### `job_events`

Purpose: job progress timeline.

Core fields:

```text
id
public_id
job_id
event_type
payload_json
created_at
```

#### `evaluation_runs`

Purpose: one evaluation execution.

Core fields:

```text
id
public_id
project_id
dataset_id
rubric_version_id
target_api_connector_id
job_id
status
total_cases
passed_cases
failed_cases
warning_cases
error_cases
pass_rate
max_concurrency
created_by
started_at
completed_at
created_at
updated_at
```

Status:

```text
PENDING
RUNNING
COMPLETED
FAILED
CANCELLED
```

#### `evaluation_results`

Purpose: one test case result inside a run.

Expected fields:

```text
id
public_id
evaluation_run_id
test_case_id
external_id
question
ground_truth
actual_answer
raw_request_json
raw_response_json
judge_score
judge_status
judge_reason
criteria_results_json
latency_ms
error_message
created_at
updated_at
```

Judge status:

```text
PASS
FAIL
WARNING
ERROR
```

Rule:

```text
judge_status is automated.
qc_status is human final decision and belongs to review_decisions.
Do not overwrite judge_status when QC reviews a result.
```

#### `review_decisions`

Purpose: human QC final decision.

Expected fields:

```text
id
public_id
evaluation_result_id
qc_status
qc_note
pic_bug
reviewed_by
reviewed_at
created_at
updated_at
```

QC status:

```text
NOT_REVIEWED
PASS
FAIL
NEED_FIX
IGNORED
```

#### `export_files`

Purpose: generated export metadata.

Expected fields:

```text
id
public_id
project_id
evaluation_run_id
job_id
file_type
status
file_name
file_path
download_url
created_by
created_at
ready_at
expires_at
updated_at
```

File type:

```text
EXCEL
JSON
```

Export status:

```text
PENDING
READY
FAILED
EXPIRED
```

---

## 8. Flyway Migration Order

Recommended:

```text
V1__enable_extensions.sql
V2__create_users.sql
V3__create_projects.sql
V4__create_target_api_connectors.sql
V5__create_connector_secrets.sql
V6__create_requirements_datasets_test_cases.sql
V7__create_rubrics.sql
V8__create_jobs.sql
V9__create_evaluation_runs_results_reviews.sql
V10__create_export_files.sql
V11__seed_demo_user.sql
```

For faster MVP, acceptable:

```text
V1__init_schema.sql
V2__seed_demo_data.sql
```

Schema Definition of Done:

```text
[ ] Flyway migration runs from empty database
[ ] pgcrypto extension is enabled
[ ] All main tables use BIGINT id as internal PK
[ ] All main tables include UUID public_id with unique constraint
[ ] FK columns use BIGINT internal id
[ ] Demo user can be seeded
[ ] Project CRUD can persist data
[ ] target_api_connectors can persist Postman-like request config
[ ] connector_secrets can store encrypted connector secrets
[ ] Dataset and test cases can persist JSON metadata
[ ] Rubric and criteria can persist dynamic judge config
[ ] Jobs and events can track async execution
[ ] Evaluation runs reference target_api_connectors
[ ] Evaluation results can store judge output
[ ] QC review decision can be upserted
[ ] Export file metadata can be tracked
[ ] Core indexes exist for dashboard queries
```

---

## 9. API Endpoint Summary

### 9.1 Auth and user

```http
POST   /api/v1/sessions
GET    /api/v1/users/me
DELETE /api/v1/sessions/current
```

Login request:

```json
{
  "email": "qc.demo@example.com",
  "password": "password123"
}
```

Login response must include:

```text
accessToken
tokenType = Bearer
expiresInSeconds
user.publicId
user.email
user.displayName
user.roleCode
user.status
```

Logout can return `204 No Content`.

### 9.2 Projects

```http
POST   /api/v1/projects
GET    /api/v1/projects
GET    /api/v1/projects/{projectPublicId}
PATCH  /api/v1/projects/{projectPublicId}
DELETE /api/v1/projects/{projectPublicId}
```

Project request fields:

```text
name
description
evaluationScope
retentionDays
```

Delete archives the project.

### 9.3 Target API connector

```http
POST   /api/v1/target-api-connector-drafts
POST   /api/v1/projects/{projectPublicId}/target-api-connectors
GET    /api/v1/projects/{projectPublicId}/target-api-connectors
GET    /api/v1/target-api-connectors/{connectorPublicId}
PATCH  /api/v1/target-api-connectors/{connectorPublicId}
POST   /api/v1/target-api-connectors/{connectorPublicId}/test-runs
GET    /api/v1/target-api-connectors/{connectorPublicId}/secrets
PUT    /api/v1/target-api-connectors/{connectorPublicId}/secrets/{secretKey}
DELETE /api/v1/target-api-connectors/{connectorPublicId}/secrets/{secretKey}
```

Connector create request fields:

```text
name
description
rawCurl
method
baseUrl
path
url
headers
queryParams
pathParams
bodyType
bodyTemplate
bodyTemplateText
authType
authConfig
secretValues
responseFormat
responseSelector
isStreaming
streamingType
streamingEventSelector
timeoutSeconds
retryCount
active
```

Rules:

```text
secretValues is write-only.
headers/body/auth store placeholders, not raw secrets.
Responses return secretRefs with maskedValue only.
```

Connector test response should include:

```text
success
requestPreview with masked secrets
rawResponse if safe
extractedAnswer
latencyMs
```

### 9.4 Requirements

```http
POST  /api/v1/projects/{projectPublicId}/requirements
GET   /api/v1/projects/{projectPublicId}/requirements
GET   /api/v1/requirements/{requirementPublicId}
PATCH /api/v1/requirements/{requirementPublicId}
```

Requirement fields:

```text
content
version
status
```

### 9.5 Datasets and test cases

```http
POST   /api/v1/projects/{projectPublicId}/datasets
GET    /api/v1/projects/{projectPublicId}/datasets
GET    /api/v1/datasets/{datasetPublicId}
PATCH  /api/v1/datasets/{datasetPublicId}

POST   /api/v1/datasets/{datasetPublicId}/test-cases
GET    /api/v1/datasets/{datasetPublicId}/test-cases
PATCH  /api/v1/test-cases/{testCasePublicId}
DELETE /api/v1/test-cases/{testCasePublicId}
```

Dataset fields:

```text
requirementPublicId
sourceType
name
description
version
status
totalCases
```

Test case fields:

```text
externalId
question
precondition
groundTruth
metadata
status
sortOrder
```

Deleting a test case should mark it inactive or archive-like for MVP, not necessarily hard-delete.

### 9.6 Rubrics and criteria

```http
POST   /api/v1/projects/{projectPublicId}/rubrics
POST   /api/v1/rubrics/{rubricPublicId}/versions
POST   /api/v1/rubric-versions/{rubricVersionPublicId}/criteria
GET    /api/v1/rubric-versions/{rubricVersionPublicId}
PATCH  /api/v1/rubric-criteria/{criterionPublicId}
DELETE /api/v1/rubric-criteria/{criterionPublicId}
```

Criterion fields:

```text
name
description
weight
passCondition
failCondition
judgeInstruction
metricKey
isCritical
sortOrder
```

Suggested demo criteria:

```text
Correctness
Completeness
Clarity
Safety / No hallucination
```

### 9.7 Evaluation runs

```http
POST /api/v1/projects/{projectPublicId}/evaluation-runs
GET  /api/v1/projects/{projectPublicId}/evaluation-runs
GET  /api/v1/evaluation-runs/{runPublicId}
GET  /api/v1/evaluation-runs/{runPublicId}/results
GET  /api/v1/evaluation-runs/{runPublicId}/events
```

Create evaluation run request:

```json
{
  "datasetPublicId": "dataset-uuid",
  "rubricVersionPublicId": "rubric-version-uuid",
  "targetConnectorPublicId": "connector-uuid",
  "maxConcurrency": 3
}
```

Create response:

```text
202 Accepted
runPublicId
jobPublicId
status = PENDING
```

Validation rules:

```text
Dataset must exist under same project.
Dataset must be APPROVED.
Rubric version must exist and be PUBLISHED, unless preview run is explicitly supported.
Target connector must exist under same project and be active.
Dataset should contain fewer than 100 active cases for MVP.
```

Evaluation result list must support filters:

```text
judgeStatus
qcStatus
page
size
```

Result response item must include enough fields for QC dashboard:

```text
publicId
evaluationRunPublicId
testCasePublicId
externalId
question
groundTruth
actualAnswer
judgeScore
judgeStatus
judgeReason
criteriaResults
latencyMs
qcStatus
qcNote
picBug
createdAt
```

### 9.8 Jobs

```http
GET /api/v1/jobs/{jobPublicId}
```

Job response should include:

```text
publicId
jobType
status
resourceType
resourcePublicId if resolvable
progressCurrent
progressTotal
errorMessage
createdAt
startedAt
completedAt
updatedAt
```

### 9.9 QC review decisions

```http
PUT   /api/v1/evaluation-results/{resultPublicId}/review-decision
GET   /api/v1/evaluation-results/{resultPublicId}/review-decision
PATCH /api/v1/review-decisions/{reviewDecisionPublicId}
```

Review request:

```json
{
  "qcStatus": "NEED_FIX",
  "qcNote": "Answer is too vague. It should include exact step count.",
  "picBug": "Long"
}
```

Rule:

```text
Upsert by evaluationResultPublicId.
QC review updates qc_status only.
Do not mutate judge_status.
```

### 9.10 Exports

```http
POST /api/v1/evaluation-runs/{runPublicId}/exports
GET  /api/v1/exports/{exportPublicId}
GET  /api/v1/exports/{exportPublicId}/file
```

Create export request:

```json
{
  "fileType": "EXCEL"
}
```

Allowed:

```text
EXCEL
JSON
```

Create response:

```text
202 Accepted
exportPublicId
jobPublicId
status = PENDING
```

Download rules:

```text
If export status is not READY, return 409 Conflict.
If file is missing, return 404 or 500 depending on DB/file state.
Export should not fail because optional fields are missing.
Fill mapped fields and leave unavailable fields blank.
```

### 9.11 Mock chatbot

```http
POST /mock-chatbot/chat
```

Purpose:

```text
Local demo endpoint for connector testing and evaluation runs.
Can return simple JSON: { "answer": "..." }.
```

---

## 10. Promptfoo Runtime Model

Promptfoo is a CLI evaluation engine.

Runtime flow:

```text
Spring worker
→ Generate runs/{runPublicId}/promptfooconfig.yaml
→ Generate runs/{runPublicId}/tests.json
→ Execute promptfoo CLI
→ Save runs/{runPublicId}/results.json
→ Parse results
→ Save evaluation results into PostgreSQL
```

Promptfoo is not a permanent service in MVP.

Local command should use project-local promptfoo:

```text
PROMPTFOO_COMMAND=./infra/scripts/run-promptfoo.sh
PROMPTFOO_WORK_DIR=./runs
```

Docker/VPS command should use globally installed promptfoo inside server image:

```text
PROMPTFOO_COMMAND=promptfoo
PROMPTFOO_WORK_DIR=/app/runs
```

Do not use this for production/demo jobs:

```bash
npx -y promptfoo@latest eval ...
```

Reasons:

```text
It may download dependencies during a user-triggered job.
It may pull a different latest version.
It may fail if the container cannot reach npm.
It slows down first evaluation run.
```

---

## 11. Target API Connector Runtime Rendering

At runtime, render `target_api_connectors` into an HTTP request:

```text
1. Load connector by publicId.
2. Load connector_secrets by connector_id.
3. Replace {{secret:KEY}} placeholders in headers/body/auth config.
4. Replace test-case variables:
   - {{question}}
   - {{precondition}}
   - {{metadata}}
   - optionally {{groundTruth}}
5. Send HTTP request to target API.
6. Extract answer using response_selector.
7. Store safe result in evaluation_results.
```

MVP should support first:

```text
HTTP
RAW_JSON body
JSON response
response_selector like $.answer
non-streaming request
BEARER / CUSTOM_HEADER / NONE auth
```

Future-compatible but not required for first implementation:

```text
GRAPHQL
FORM_DATA
X_WWW_FORM_URLENCODED
SSE
CHUNKED streaming
```

---

## 12. Environment Variables

Use `.env` from `.env.example`.

Important variables:

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

Production overrides:

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

Do not commit real `.env`.

---

## 13. Docker / VPS Rules

VPS host should only need:

```text
Docker Engine
Docker Compose plugin
Git only if pulling source directly on VPS
```

VPS host should not need:

```text
Java
Node.js
npm
npx
promptfoo
PostgreSQL
Redis
Nginx
```

Reason:

```text
Java runs inside server container.
Node.js + promptfoo run inside server container.
Client is built inside client Docker image and served by Nginx inside that image.
PostgreSQL and Redis run as Docker Compose services.
```

Server Docker image must contain:

```text
Java 21 runtime
Node.js 22
npm global promptfoo@0.121.12
/app/runs
/app/exports
```

Server Dockerfile behavior:

```text
Build jar with Maven.
Install Node.js and promptfoo in runtime image.
Run: java $JAVA_OPTS -jar app.jar
```

---

## 14. Local Development Commands

Start development dependencies:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d
```

Stop dependencies:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml down
```

Reset dev data:

```bash
docker compose --env-file .env -f infra/docker-compose.dev.yml down -v
```

Run server from project root:

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

Promptfoo local setup:

```bash
mkdir -p tooling/promptfoo-runner
cd tooling/promptfoo-runner
npm init -y
npm install promptfoo@0.121.12
./node_modules/.bin/promptfoo --version
```

---

## 15. Recommended Backend Implementation Order

### Phase 1 — Base project setup

```text
[ ] Create Spring Boot app under server/
[ ] Configure package root me.nghlong3004.vqc.api
[ ] Add application.yml, application-dev.yml, application-prod.yml
[ ] Add Flyway
[ ] Add PostgreSQL connection
[ ] Add Redis connection
[ ] Add Problem Details error handling
[ ] Add pagination response model
[ ] Add common enums
```

### Phase 2 — Auth, users, projects

```text
[ ] users table + seed user
[ ] JWT login endpoint
[ ] current user endpoint
[ ] logout endpoint
[ ] project CRUD
[ ] archive project behavior
```

### Phase 3 — Target connectors and secrets

```text
[ ] target_api_connectors table/entity/repository
[ ] connector_secrets table/entity/repository
[ ] create connector draft from cURL
[ ] create/list/detail/update connector
[ ] upsert/list/delete secrets
[ ] test connector endpoint
[ ] masking and secret replacement
```

### Phase 4 — Requirements, datasets, test cases

```text
[ ] business_requirements CRUD
[ ] datasets CRUD
[ ] test_cases CRUD
[ ] dataset approval validation
[ ] active case count validation
```

### Phase 5 — Rubrics

```text
[ ] rubrics
[ ] rubric_versions
[ ] rubric_criteria
[ ] publish/draft behavior if needed
[ ] map criteria to promptfoo judge config
```

### Phase 6 — Jobs and evaluation

```text
[ ] jobs + job_events
[ ] evaluation_runs
[ ] evaluation_results
[ ] worker loop / queue consumer
[ ] generate promptfoo run directory
[ ] execute promptfoo
[ ] parse results
[ ] persist run summary and result rows
```

### Phase 7 — QC review and export

```text
[ ] review_decisions upsert/get/update
[ ] export_files
[ ] export job
[ ] Excel/JSON file generation
[ ] download endpoint
```

### Phase 8 — Demo support

```text
[ ] mockchatbot endpoint
[ ] demo project seed if useful
[ ] demo connector seed if useful
[ ] demo rubric seed if useful
[ ] local smoke tests
```

---

## 16. Business Rules Checklist

```text
[ ] API URLs use public IDs, not BIGINT IDs.
[ ] API response bodies use publicId, not id.
[ ] Auth APIs use email in contract.
[ ] Connector APIs use target-api-connectors, not api-connectors.
[ ] Connector secret values are write-only and never returned raw.
[ ] Request preview masks secrets.
[ ] Error format is standardized with Problem Details.
[ ] Long-running evaluation/export returns 202.
[ ] Dataset must be APPROVED before official evaluation run.
[ ] Dataset with 0 active cases cannot be approved.
[ ] MVP warns/rejects if active test cases exceed 100.
[ ] Target connector must be active before evaluation run.
[ ] Rubric version should be PUBLISHED before official evaluation run.
[ ] judgeStatus is automated.
[ ] qcStatus is human final decision.
[ ] QC review must not overwrite judgeStatus.
[ ] Export should not fail because optional fields are missing.
[ ] Worker should not log raw secrets or fully rendered sensitive requests.
[ ] No generated runs/exports are committed to Git.
```

---

## 17. Definition of Done for Codex

Backend MVP is acceptable when:

```text
[ ] Server starts with dev profile.
[ ] Flyway creates schema from empty PostgreSQL database.
[ ] Demo user can login.
[ ] JWT protects private APIs.
[ ] Project CRUD works.
[ ] Target connector CRUD works.
[ ] Connector secrets are masked in responses.
[ ] Connector test endpoint can call mock chatbot.
[ ] Requirement / dataset / test case APIs work.
[ ] Rubric / criteria APIs work.
[ ] Creating evaluation run creates job and returns 202.
[ ] Worker can execute evaluation flow.
[ ] Promptfoo command is invoked from configured PROMPTFOO_COMMAND.
[ ] Evaluation results are saved to DB.
[ ] Dashboard result API returns judgeStatus + qcStatus fields.
[ ] QC review decision can be upserted.
[ ] Export job creates EXCEL or JSON metadata/file.
[ ] Download export endpoint works when READY.
[ ] No API response exposes internal BIGINT id.
[ ] No API response exposes raw secret values.
[ ] All Java files include the required author/since header.
```

---

## 18. Do Not Do

```text
Do not install Node.js, npm, npx, or promptfoo directly on the VPS host.
Do not use promptfoo@latest for reproducible demo execution.
Do not vendor promptfoo source into the app repo unless intentionally maintaining a custom fork.
Do not create extra Spring profiles like local/worker/demo.
Do not call the table api_connectors; use target_api_connectors.
Do not expose internal id in DTOs.
Do not accept internal id from client.
Do not log Authorization/API key/Bearer token values.
Do not store raw token values in JSONB connector templates.
Do not hard-delete important records during MVP unless explicitly requested.
Do not eager-load large collections like all test cases/results by default.
```
