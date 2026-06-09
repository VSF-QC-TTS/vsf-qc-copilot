# 08. API Contract MVP — VSF QC Copilot

> Implementation contract for backend and frontend.  
> Use this document to keep request/response fields consistent while coding the Week 3–4 MVP.

---

## 1. API Principles

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

MVP style:

```text
RESTful resources where possible
Action-like behavior should be modeled as subresources when reasonable
Long-running work returns 202 Accepted with job/run/export public IDs
```

Important naming rule:

```text
Database uses:
- id BIGINT as internal primary key
- public_id UUID as public identifier

API uses:
- publicId in response bodies
- {resourcePublicId} in URLs
- never expose internal BIGINT id
```

---

## 2. Common Conventions

### 2.1 Public ID Format

Use UUID strings for persisted resources in API responses.

```json
{
  "publicId": "0f6d90c2-7410-4db2-86be-8adfd3140f63"
}
```

Relationship fields should also use public IDs.

```json
{
  "projectPublicId": "5a4edcc1-cd1e-44ef-a144-31f5f3d2f653",
  "datasetPublicId": "0f6d90c2-7410-4db2-86be-8adfd3140f63"
}
```

Do not return this from public APIs:

```json
{
  "id": 123
}
```

### 2.2 Field Naming

API request/response fields use `camelCase`.

Database columns use `snake_case`.

Examples:

```text
publicId          -> public_id
createdAt         -> created_at
targetConnectorId -> target_connector_id internally only
```

### 2.3 Timestamp Format

Use ISO-8601 with timezone.

```text
2026-06-08T10:30:00+07:00
```

Recommended Java type:

```text
OffsetDateTime for API DTOs
Instant or OffsetDateTime for persistence
```

### 2.4 Pagination

For list APIs:

```http
GET /api/v1/projects?page=0&size=20&sort=createdAt,desc
```

Response:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

MVP can return a simple array for very small internal lists, but the pagination format above is preferred for consistency.

### 2.5 Error Format

Use Problem Details style.

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

| Status | Meaning |
|---|---|
| 200 | Success |
| 201 | Resource created |
| 202 | Long-running job accepted |
| 204 | Success with no body |
| 400 | Invalid request |
| 401 | Not authenticated |
| 403 | Not allowed |
| 404 | Resource not found |
| 409 | Conflict / invalid state transition |
| 422 | Valid JSON but business validation failed |
| 500 | Unexpected server error |

### 2.6 Secret Handling

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

Do not log:

```text
Authorization header
API key values
Bearer token values
Raw connector secret values
```

---

## 3. Authentication APIs

### 3.1 Login

```http
POST /api/v1/sessions
```

Request:

```json
{
  "email": "qc.demo@example.com",
  "password": "password123"
}
```

Response `200 OK`:

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresInSeconds": 7200,
  "user": {
    "publicId": "user-uuid",
    "email": "qc.demo@example.com",
    "firstName": "QC",
    "lastName": "Demo",
    "displayName": "QC Demo",
    "roleCode": "QC_MEMBER",
    "authProvider": "LOCAL",
    "emailVerified": true,
    "avatarUrl": null,
    "status": "ACTIVE"
  }
}
```

Errors:

```text
401 if email/password is invalid
403 if user status is DISABLED
```

---

### 3.2 Current User

```http
GET /api/v1/users/me
```

Response `200 OK`:

```json
{
  "publicId": "user-uuid",
  "email": "qc.demo@example.com",
  "firstName": "QC",
  "lastName": "Demo",
  "displayName": "QC Demo",
  "roleCode": "QC_MEMBER",
  "authProvider": "LOCAL",
  "emailVerified": true,
  "avatarUrl": null,
  "status": "ACTIVE",
  "lastLoginAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 3.3 Logout

```http
DELETE /api/v1/sessions/current
```

Response:

```text
204 No Content
```

For JWT-only MVP, frontend can also clear token locally. Keep the endpoint for a clean contract.

---

## 4. Project APIs

### 4.1 Create Project

```http
POST /api/v1/projects
```

Request:

```json
{
  "name": "AI Health Chatbot Demo",
  "description": "Evaluate health chatbot answers for Apple Health-like scenarios.",
  "evaluationScope": "Health assistant QA evaluation",
  "retentionDays": 30
}
```

Response `201 Created`:

```json
{
  "publicId": "project-uuid",
  "name": "AI Health Chatbot Demo",
  "description": "Evaluate health chatbot answers for Apple Health-like scenarios.",
  "evaluationScope": "Health assistant QA evaluation",
  "retentionDays": 30,
  "status": "ACTIVE",
  "createdBy": {
    "publicId": "user-uuid",
    "displayName": "QC Demo"
  },
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 4.2 List Projects

```http
GET /api/v1/projects
```

Optional filters:

```http
GET /api/v1/projects?status=ACTIVE&page=0&size=20
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "project-uuid",
      "name": "AI Health Chatbot Demo",
      "status": "ACTIVE",
      "createdAt": "2026-06-08T10:30:00+07:00",
      "updatedAt": "2026-06-08T10:30:00+07:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### 4.3 Get Project Detail

```http
GET /api/v1/projects/{projectPublicId}
```

Response `200 OK`:

```json
{
  "publicId": "project-uuid",
  "name": "AI Health Chatbot Demo",
  "description": "Evaluate health chatbot answers for Apple Health-like scenarios.",
  "evaluationScope": "Health assistant QA evaluation",
  "retentionDays": 30,
  "status": "ACTIVE",
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 4.4 Update Project

```http
PATCH /api/v1/projects/{projectPublicId}
```

Request:

```json
{
  "name": "AI Health Chatbot Demo v2",
  "description": "Updated description",
  "evaluationScope": "Updated scope",
  "retentionDays": 60
}
```

Response `200 OK`: project detail.

---

### 4.5 Archive Project

```http
DELETE /api/v1/projects/{projectPublicId}
```

Response:

```text
204 No Content
```

This should archive the project, not hard-delete it.

---

## 5. Target API Connector APIs

Target API connectors represent the external/internal chatbot API that VSF QC Copilot evaluates.

The DB table should be named:

```text
target_api_connectors
```

The public API resource path should be:

```text
target-api-connectors
```

### 5.1 Create Target Connector Draft from cURL

```http
POST /api/v1/target-api-connector-drafts
```

Request:

```json
{
  "rawCurl": "curl -X POST https://example.com/chat -H 'Authorization: Bearer ogw_live_xxx' -H 'Content-Type: application/json' -d '{\"message\":\"{{question}}\"}'"
}
```

Response `200 OK`:

```json
{
  "method": "POST",
  "baseUrl": "https://example.com",
  "path": "/chat",
  "url": "https://example.com/chat",
  "headers": {
    "Authorization": "Bearer {{secret:AUTHORIZATION_TOKEN}}",
    "Content-Type": "application/json"
  },
  "queryParams": {},
  "pathParams": {},
  "bodyType": "RAW_JSON",
  "bodyTemplate": {
    "message": "{{question}}"
  },
  "bodyTemplateText": null,
  "authType": "BEARER",
  "responseFormat": "JSON",
  "responseSelector": "$.answer",
  "isStreaming": false,
  "streamingType": null,
  "timeoutSeconds": 60,
  "retryCount": 1,
  "detectedSecrets": [
    {
      "secretKey": "AUTHORIZATION_TOKEN",
      "location": "header.Authorization",
      "maskedValue": "ogw_live_****xxx"
    }
  ],
  "warnings": []
}
```

MVP fallback:

```text
If parser is incomplete, return parsed fields plus warnings and allow user to edit manually.
Detected secret values are write-only and must not be returned raw.
```

---

### 5.2 Create Target Connector

```http
POST /api/v1/projects/{projectPublicId}/target-api-connectors
```

Request:

```json
{
  "name": "Mock Health Chatbot",
  "description": "Local mock chatbot for demo.",
  "rawCurl": null,
  "method": "POST",
  "baseUrl": "http://localhost:8080",
  "path": "/mock-chatbot/chat",
  "url": "http://localhost:8080/mock-chatbot/chat",
  "headers": {
    "Content-Type": "application/json",
    "Authorization": "Bearer {{secret:CHATBOT_API_TOKEN}}"
  },
  "queryParams": {},
  "pathParams": {},
  "bodyType": "RAW_JSON",
  "bodyTemplate": {
    "message": "{{question}}",
    "context": "{{precondition}}",
    "metadata": "{{metadata}}"
  },
  "bodyTemplateText": null,
  "authType": "BEARER",
  "authConfig": {
    "tokenRef": "{{secret:CHATBOT_API_TOKEN}}"
  },
  "secretValues": {
    "CHATBOT_API_TOKEN": "write-only-token-value"
  },
  "responseFormat": "JSON",
  "responseSelector": "$.answer",
  "isStreaming": false,
  "streamingType": null,
  "streamingEventSelector": null,
  "timeoutSeconds": 60,
  "retryCount": 1,
  "active": true
}
```

Notes:

```text
secretValues is write-only.
Backend stores secretValues in connector_secrets.
Backend stores headers with secret placeholders, not raw secret values.
```

Response `201 Created`:

```json
{
  "publicId": "connector-uuid",
  "projectPublicId": "project-uuid",
  "name": "Mock Health Chatbot",
  "description": "Local mock chatbot for demo.",
  "method": "POST",
  "baseUrl": "http://localhost:8080",
  "path": "/mock-chatbot/chat",
  "url": "http://localhost:8080/mock-chatbot/chat",
  "headers": {
    "Content-Type": "application/json",
    "Authorization": "Bearer {{secret:CHATBOT_API_TOKEN}}"
  },
  "queryParams": {},
  "pathParams": {},
  "bodyType": "RAW_JSON",
  "bodyTemplate": {
    "message": "{{question}}",
    "context": "{{precondition}}",
    "metadata": "{{metadata}}"
  },
  "bodyTemplateText": null,
  "authType": "BEARER",
  "authConfig": {
    "tokenRef": "{{secret:CHATBOT_API_TOKEN}}"
  },
  "secretRefs": [
    {
      "secretKey": "CHATBOT_API_TOKEN",
      "maskedValue": "****value"
    }
  ],
  "responseFormat": "JSON",
  "responseSelector": "$.answer",
  "isStreaming": false,
  "streamingType": null,
  "streamingEventSelector": null,
  "timeoutSeconds": 60,
  "retryCount": 1,
  "active": true,
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 5.3 List Target Connectors

```http
GET /api/v1/projects/{projectPublicId}/target-api-connectors
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "connector-uuid",
      "projectPublicId": "project-uuid",
      "name": "Mock Health Chatbot",
      "method": "POST",
      "url": "http://localhost:8080/mock-chatbot/chat",
      "responseSelector": "$.answer",
      "isStreaming": false,
      "active": true,
      "createdAt": "2026-06-08T10:30:00+07:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### 5.4 Get Target Connector Detail

```http
GET /api/v1/target-api-connectors/{connectorPublicId}
```

Response `200 OK`: target connector detail with masked secret references only.

---

### 5.5 Update Target Connector

```http
PATCH /api/v1/target-api-connectors/{connectorPublicId}
```

Request: same editable fields as create, all optional.

```json
{
  "name": "Mock Health Chatbot v2",
  "timeoutSeconds": 90,
  "retryCount": 2,
  "active": true
}
```

Response `200 OK`: target connector detail with masked secret references only.

---

### 5.6 Test Target Connector

```http
POST /api/v1/target-api-connectors/{connectorPublicId}/test-runs
```

Request:

```json
{
  "question": "How many steps did I walk today?",
  "precondition": {
    "steps": 8200,
    "date": "2026-06-08"
  },
  "metadata": {
    "expectedStatus": "PASS"
  }
}
```

Response `200 OK`:

```json
{
  "success": true,
  "requestPreview": {
    "method": "POST",
    "url": "http://localhost:8080/mock-chatbot/chat",
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer ********"
    },
    "body": {
      "message": "How many steps did I walk today?",
      "context": {
        "steps": 8200,
        "date": "2026-06-08"
      },
      "metadata": {
        "expectedStatus": "PASS"
      }
    }
  },
  "rawResponse": {
    "answer": "Today you walked 8,200 steps."
  },
  "extractedAnswer": "Today you walked 8,200 steps.",
  "latencyMs": 120
}
```

Rules:

```text
requestPreview must mask secrets.
rawResponse should be safe for display. If the target returns sensitive fields, filter or mask them.
```

---

### 5.7 List Target Connector Secrets

```http
GET /api/v1/target-api-connectors/{connectorPublicId}/secrets
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "connector-secret-uuid",
      "secretKey": "CHATBOT_API_TOKEN",
      "maskedValue": "ogw_live_****f7",
      "createdAt": "2026-06-08T10:30:00+07:00",
      "updatedAt": "2026-06-08T10:30:00+07:00"
    }
  ]
}
```

---

### 5.8 Upsert Target Connector Secret

```http
PUT /api/v1/target-api-connectors/{connectorPublicId}/secrets/{secretKey}
```

Request:

```json
{
  "value": "new-secret-token-value"
}
```

Response `200 OK`:

```json
{
  "publicId": "connector-secret-uuid",
  "secretKey": "CHATBOT_API_TOKEN",
  "maskedValue": "new-secret-****alue",
  "updatedAt": "2026-06-08T10:35:00+07:00"
}
```

Rules:

```text
value is write-only.
Never return encryptedValue or raw secret value.
```

---

### 5.9 Delete Target Connector Secret

```http
DELETE /api/v1/target-api-connectors/{connectorPublicId}/secrets/{secretKey}
```

Response:

```text
204 No Content
```

---

## 6. Business Requirement APIs

### 6.1 Create Requirement

```http
POST /api/v1/projects/{projectPublicId}/requirements
```

Request:

```json
{
  "content": "Evaluate whether the chatbot can answer Apple Health step-count questions correctly."
}
```

Response `201 Created`:

```json
{
  "publicId": "requirement-uuid",
  "projectPublicId": "project-uuid",
  "content": "Evaluate whether the chatbot can answer Apple Health step-count questions correctly.",
  "version": 1,
  "status": "ACTIVE",
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 6.2 List Requirements

```http
GET /api/v1/projects/{projectPublicId}/requirements
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "requirement-uuid",
      "projectPublicId": "project-uuid",
      "content": "Evaluate whether the chatbot can answer Apple Health step-count questions correctly.",
      "version": 1,
      "status": "ACTIVE",
      "createdAt": "2026-06-08T10:30:00+07:00"
    }
  ]
}
```

---

### 6.3 Get Requirement Detail

```http
GET /api/v1/requirements/{requirementPublicId}
```

Response `200 OK`: requirement detail.

---

### 6.4 Update Requirement

```http
PATCH /api/v1/requirements/{requirementPublicId}
```

Request:

```json
{
  "content": "Updated requirement content.",
  "status": "ACTIVE"
}
```

Response `200 OK`: requirement detail.

---

## 7. Dataset and Test Case APIs

### 7.1 Create Dataset

```http
POST /api/v1/projects/{projectPublicId}/datasets
```

Request:

```json
{
  "requirementPublicId": "requirement-uuid",
  "sourceType": "SAMPLE_DEMO",
  "name": "Health Demo Dataset",
  "description": "Sample dataset for Week 4 demo."
}
```

Response `201 Created`:

```json
{
  "publicId": "dataset-uuid",
  "projectPublicId": "project-uuid",
  "requirementPublicId": "requirement-uuid",
  "name": "Health Demo Dataset",
  "description": "Sample dataset for Week 4 demo.",
  "version": 1,
  "sourceType": "SAMPLE_DEMO",
  "status": "DRAFT",
  "totalCases": 0,
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 7.2 List Datasets

```http
GET /api/v1/projects/{projectPublicId}/datasets
```

Optional filters:

```http
GET /api/v1/projects/{projectPublicId}/datasets?status=DRAFT&page=0&size=20
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "dataset-uuid",
      "projectPublicId": "project-uuid",
      "name": "Health Demo Dataset",
      "sourceType": "SAMPLE_DEMO",
      "status": "DRAFT",
      "totalCases": 0,
      "createdAt": "2026-06-08T10:30:00+07:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### 7.3 Get Dataset Detail

```http
GET /api/v1/datasets/{datasetPublicId}
```

Response includes dataset metadata, not necessarily all test cases.

---

### 7.4 Update Dataset

```http
PATCH /api/v1/datasets/{datasetPublicId}
```

Request:

```json
{
  "name": "Health Demo Dataset v2",
  "description": "Updated description",
  "status": "APPROVED"
}
```

Response `200 OK`: dataset detail.

Rules:

```text
Only APPROVED datasets should be used for official evaluation run.
A dataset with 0 active test cases cannot be approved.
MVP should warn or reject if active test cases exceed 100.
```

---

### 7.5 Create Test Case

```http
POST /api/v1/datasets/{datasetPublicId}/test-cases
```

Request:

```json
{
  "externalId": "HEALTH_001",
  "question": "How many steps did I walk today?",
  "precondition": {
    "steps": 8200,
    "date": "2026-06-08"
  },
  "groundTruth": "The user walked 8,200 steps today.",
  "metadata": {
    "userId": "demo-user-1",
    "expectedStatus": "PASS"
  },
  "status": "ACTIVE",
  "sortOrder": 1
}
```

Response `201 Created`:

```json
{
  "publicId": "test-case-uuid",
  "datasetPublicId": "dataset-uuid",
  "externalId": "HEALTH_001",
  "question": "How many steps did I walk today?",
  "precondition": {
    "steps": 8200,
    "date": "2026-06-08"
  },
  "groundTruth": "The user walked 8,200 steps today.",
  "metadata": {
    "userId": "demo-user-1",
    "expectedStatus": "PASS"
  },
  "status": "ACTIVE",
  "sortOrder": 1,
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 7.6 List Test Cases

```http
GET /api/v1/datasets/{datasetPublicId}/test-cases
```

Optional filters:

```http
GET /api/v1/datasets/{datasetPublicId}/test-cases?status=ACTIVE&page=0&size=100
```

Response `200 OK`: paginated test case list.

---

### 7.7 Update Test Case

```http
PATCH /api/v1/test-cases/{testCasePublicId}
```

Request: any editable test case fields.

Response `200 OK`: test case detail.

---

### 7.8 Delete Test Case

```http
DELETE /api/v1/test-cases/{testCasePublicId}
```

Response:

```text
204 No Content
```

Soft delete or mark inactive is preferred.

---

## 8. Rubric APIs

### 8.1 Create Rubric

```http
POST /api/v1/projects/{projectPublicId}/rubrics
```

Request:

```json
{
  "name": "Health Answer Quality Rubric",
  "description": "Checks correctness, completeness, clarity, and safety."
}
```

Response `201 Created`:

```json
{
  "publicId": "rubric-uuid",
  "projectPublicId": "project-uuid",
  "name": "Health Answer Quality Rubric",
  "description": "Checks correctness, completeness, clarity, and safety.",
  "currentVersion": 1,
  "createdAt": "2026-06-08T10:30:00+07:00",
  "updatedAt": "2026-06-08T10:30:00+07:00"
}
```

---

### 8.2 Create Rubric Version

```http
POST /api/v1/rubrics/{rubricPublicId}/versions
```

Request:

```json
{
  "version": 1,
  "status": "DRAFT"
}
```

Response `201 Created`:

```json
{
  "publicId": "rubric-version-uuid",
  "rubricPublicId": "rubric-uuid",
  "version": 1,
  "status": "DRAFT",
  "createdAt": "2026-06-08T10:30:00+07:00",
  "publishedAt": null
}
```

---

### 8.3 Add Criterion

```http
POST /api/v1/rubric-versions/{rubricVersionPublicId}/criteria
```

Request:

```json
{
  "name": "Correctness",
  "description": "The answer must match the expected facts and numbers.",
  "weight": 0.4,
  "passCondition": "All key facts and numbers match the ground truth.",
  "failCondition": "The answer contains wrong facts or wrong numbers.",
  "judgeInstruction": "Compare actual answer with ground truth. Focus on factual correctness.",
  "metricKey": "correctness",
  "isCritical": true,
  "sortOrder": 1
}
```

Response `201 Created`:

```json
{
  "publicId": "criterion-uuid",
  "rubricVersionPublicId": "rubric-version-uuid",
  "name": "Correctness",
  "description": "The answer must match the expected facts and numbers.",
  "weight": 0.4,
  "passCondition": "All key facts and numbers match the ground truth.",
  "failCondition": "The answer contains wrong facts or wrong numbers.",
  "judgeInstruction": "Compare actual answer with ground truth. Focus on factual correctness.",
  "metricKey": "correctness",
  "isCritical": true,
  "sortOrder": 1
}
```

---

### 8.4 Get Rubric Version Detail

```http
GET /api/v1/rubric-versions/{rubricVersionPublicId}
```

Response includes criteria:

```json
{
  "publicId": "rubric-version-uuid",
  "rubricPublicId": "rubric-uuid",
  "version": 1,
  "status": "PUBLISHED",
  "criteria": [
    {
      "publicId": "criterion-uuid",
      "name": "Correctness",
      "weight": 0.4,
      "metricKey": "correctness",
      "isCritical": true,
      "sortOrder": 1
    }
  ]
}
```

---

### 8.5 Update Criterion

```http
PATCH /api/v1/rubric-criteria/{criterionPublicId}
```

Request: any editable criterion fields.

Response `200 OK`: criterion detail.

---

### 8.6 Delete Criterion

```http
DELETE /api/v1/rubric-criteria/{criterionPublicId}
```

Response:

```text
204 No Content
```

---

## 9. Evaluation Run APIs

### 9.1 Create Evaluation Run

```http
POST /api/v1/projects/{projectPublicId}/evaluation-runs
```

Request:

```json
{
  "datasetPublicId": "dataset-uuid",
  "rubricVersionPublicId": "rubric-version-uuid",
  "targetConnectorPublicId": "connector-uuid",
  "maxConcurrency": 3
}
```

Response `202 Accepted`:

```json
{
  "runPublicId": "run-uuid",
  "jobPublicId": "job-uuid",
  "status": "PENDING",
  "message": "Evaluation run accepted."
}
```

Validation rules:

```text
Dataset must exist under the same project
Dataset must be APPROVED
Rubric version must exist and be PUBLISHED or explicitly allowed for preview run
Target connector must exist under the same project and be active
Dataset should contain fewer than 100 active cases for MVP
```

---

### 9.2 List Evaluation Runs

```http
GET /api/v1/projects/{projectPublicId}/evaluation-runs
```

Response:

```json
{
  "items": [
    {
      "publicId": "run-uuid",
      "projectPublicId": "project-uuid",
      "datasetPublicId": "dataset-uuid",
      "rubricVersionPublicId": "rubric-version-uuid",
      "targetConnectorPublicId": "connector-uuid",
      "jobPublicId": "job-uuid",
      "status": "COMPLETED",
      "totalCases": 50,
      "passedCases": 35,
      "failedCases": 10,
      "warningCases": 4,
      "errorCases": 1,
      "passRate": 0.7,
      "createdAt": "2026-06-08T10:30:00+07:00",
      "startedAt": "2026-06-08T10:31:00+07:00",
      "completedAt": "2026-06-08T10:36:00+07:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

---

### 9.3 Get Evaluation Run Detail

```http
GET /api/v1/evaluation-runs/{runPublicId}
```

Response `200 OK`: run summary.

---

### 9.4 Get Evaluation Results

```http
GET /api/v1/evaluation-runs/{runPublicId}/results
```

Optional filters:

```http
GET /api/v1/evaluation-runs/{runPublicId}/results?judgeStatus=FAIL&qcStatus=NOT_REVIEWED&page=0&size=50
```

Response `200 OK`:

```json
{
  "items": [
    {
      "publicId": "result-uuid",
      "evaluationRunPublicId": "run-uuid",
      "testCasePublicId": "test-case-uuid",
      "externalId": "HEALTH_001",
      "question": "How many steps did I walk today?",
      "groundTruth": "The user walked 8,200 steps today.",
      "actualAnswer": "Today you walked 8,200 steps.",
      "judgeScore": 0.95,
      "judgeStatus": "PASS",
      "judgeReason": "The answer matches the expected step count.",
      "criteriaResults": [
        {
          "metricKey": "correctness",
          "status": "PASS",
          "score": 1.0,
          "reason": "Step count matches."
        }
      ],
      "latencyMs": 1200,
      "qcStatus": "NOT_REVIEWED",
      "qcNote": null,
      "picBug": null,
      "createdAt": "2026-06-08T10:31:30+07:00"
    }
  ],
  "page": 0,
  "size": 50,
  "totalItems": 1,
  "totalPages": 1
}
```

Notes:

```text
judgeStatus is automated.
qcStatus is the human final decision.
Do not call it qcFinalStatus in API if DB enum is qc_status.
```

---

### 9.5 Get Evaluation Events

```http
GET /api/v1/evaluation-runs/{runPublicId}/events
```

Response:

```json
{
  "items": [
    {
      "publicId": "event-uuid",
      "eventType": "CASE_COMPLETED",
      "payload": {
        "completed": 10,
        "total": 50
      },
      "createdAt": "2026-06-08T10:32:00+07:00"
    }
  ]
}
```

---

## 10. Job APIs

### 10.1 Get Job Detail

```http
GET /api/v1/jobs/{jobPublicId}
```

Response:

```json
{
  "publicId": "job-uuid",
  "jobType": "EVALUATION_RUN",
  "status": "RUNNING",
  "resourceType": "EVALUATION_RUN",
  "resourcePublicId": "run-uuid",
  "projectPublicId": "project-uuid",
  "progressCurrent": 25,
  "progressTotal": 50,
  "errorMessage": null,
  "createdAt": "2026-06-08T10:30:00+07:00",
  "startedAt": "2026-06-08T10:31:00+07:00",
  "completedAt": null
}
```

---

## 11. Review Decision APIs

### 11.1 Upsert Review Decision

```http
PUT /api/v1/evaluation-results/{resultPublicId}/review-decision
```

Request:

```json
{
  "qcStatus": "NEED_FIX",
  "qcNote": "Answer is too vague. It should include exact step count.",
  "picBug": "Long"
}
```

Response `200 OK`:

```json
{
  "publicId": "review-decision-uuid",
  "evaluationResultPublicId": "result-uuid",
  "qcStatus": "NEED_FIX",
  "qcNote": "Answer is too vague. It should include exact step count.",
  "picBug": "Long",
  "reviewedBy": {
    "publicId": "user-uuid",
    "displayName": "QC Demo"
  },
  "reviewedAt": "2026-06-08T10:40:00+07:00",
  "updatedAt": "2026-06-08T10:40:00+07:00"
}
```

---

### 11.2 Get Review Decision

```http
GET /api/v1/evaluation-results/{resultPublicId}/review-decision
```

Response `200 OK`: review decision detail.

---

### 11.3 Update Review Decision

```http
PATCH /api/v1/review-decisions/{reviewDecisionPublicId}
```

Request:

```json
{
  "qcStatus": "PASS",
  "qcNote": "Confirmed by QC.",
  "picBug": null
}
```

Response `200 OK`: review decision detail.

---

## 12. Export APIs

### 12.1 Create Export

```http
POST /api/v1/evaluation-runs/{runPublicId}/exports
```

Request:

```json
{
  "fileType": "EXCEL"
}
```

Allowed file types:

```text
EXCEL
JSON
```

Response `202 Accepted`:

```json
{
  "exportPublicId": "export-uuid",
  "jobPublicId": "job-uuid",
  "status": "PENDING",
  "message": "Export job accepted."
}
```

---

### 12.2 Get Export Detail

```http
GET /api/v1/exports/{exportPublicId}
```

Response:

```json
{
  "publicId": "export-uuid",
  "projectPublicId": "project-uuid",
  "evaluationRunPublicId": "run-uuid",
  "jobPublicId": "job-uuid",
  "fileType": "EXCEL",
  "status": "READY",
  "fileName": "ai-health-chatbot-demo-run-001.xlsx",
  "downloadUrl": "/api/v1/exports/export-uuid/file",
  "createdAt": "2026-06-08T10:40:00+07:00",
  "readyAt": "2026-06-08T10:41:00+07:00"
}
```

---

### 12.3 Download Export File

```http
GET /api/v1/exports/{exportPublicId}/file
```

Response:

```text
200 OK with file stream
```

Rules:

```text
If export status is not READY, return 409 Conflict.
If file is missing, return 404 or 500 depending on DB/file state.
```

---

## 13. Mock Chatbot API

This endpoint is not under `/api/v1` because it behaves as a target API, not a product management API.

```http
POST /mock-chatbot/chat
```

Request:

```json
{
  "message": "How many steps did I walk today?",
  "context": {
    "steps": 8200,
    "date": "2026-06-08"
  },
  "metadata": {
    "expectedStatus": "PASS"
  }
}
```

Response `200 OK`:

```json
{
  "answer": "Today you walked 8,200 steps."
}
```

Suggested behavior by metadata:

| `metadata.expectedStatus` | Mock behavior |
|---|---|
| PASS | Return correct answer |
| FAIL | Return wrong answer |
| WARNING | Return incomplete/vague answer |
| ERROR | Return error or malformed answer |

---

## 14. Enum Values

### User Status

```text
ACTIVE
DISABLED
```

### User Role Code

```text
ADMIN
QC_LEAD
QC_MEMBER
VIEWER
```

### Auth Provider

```text
LOCAL
GOOGLE
```

### Project Status

```text
ACTIVE
ARCHIVED
```

### Requirement Status

```text
ACTIVE
ARCHIVED
```

### Target Connector Auth Type

```text
NONE
BEARER
API_KEY
BASIC
```

### Target Connector Body Type

```text
NONE
RAW_JSON
RAW_TEXT
FORM_DATA
X_WWW_FORM_URLENCODED
```

### Target Connector Response Format

```text
JSON
TEXT
SSE
```

### Target Connector Streaming Type

```text
SSE
CHUNKED
```

### Dataset Source Type

```text
MANUAL
IMPORTED_EXCEL
SAMPLE_DEMO
GENERATED
```

### Dataset Status

```text
DRAFT
APPROVED
ARCHIVED
```

### Test Case Status

```text
ACTIVE
INACTIVE
```

### Rubric Version Status

```text
DRAFT
PUBLISHED
ARCHIVED
```

### Job Type

```text
DATASET_GENERATION
EVALUATION_RUN
EXPORT_EXCEL
EXPORT_JSON
CONNECTOR_TEST
```

### Job Status

```text
PENDING
RUNNING
COMPLETED
FAILED
CANCELLED
```

### Evaluation Run Status

```text
PENDING
RUNNING
COMPLETED
FAILED
CANCELLED
```

### Judge Status

```text
PASS
FAIL
WARNING
ERROR
```

### QC Status

```text
NOT_REVIEWED
PASS
FAIL
NEED_FIX
IGNORED
```

### Export File Type

```text
EXCEL
JSON
```

### Export File Status

```text
PENDING
READY
FAILED
EXPIRED
```

---

## 15. API Contract Definition of Done

This contract is ready for MVP coding when:

```text
[ ] Frontend can mock all P0 API responses from this file
[ ] Backend can implement P0 endpoints from this file
[ ] Enum values match the DB schema
[ ] API URLs use public IDs, not internal BIGINT IDs
[ ] API response bodies use publicId, not id
[ ] Auth APIs use email, not username
[ ] Connector APIs use target-api-connectors, not api-connectors
[ ] Connector secret values are write-only and never returned raw
[ ] Error format is standardized
[ ] Evaluation run and export return 202 for async jobs
[ ] Result dashboard response contains enough fields for QC review
```
