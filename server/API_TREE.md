# Server API Tree

Date: 2026-06-11

Purpose: ghi lại quan hệ API hiện tại dưới dạng cây để khi project phình to vẫn dễ nắm luồng.

Nguồn tham chiếu:
- `server/SERVER_CONTEXT.md`
- `server/API_TODO.md`
- Controller mapping hiện tại dưới `server/src/main/java/me/nghlong3004/vqc/api/**/controller`

## Legend

- `public`: API public, không cần JWT theo security intent hiện tại.
- `auth`: cần `Authorization: Bearer <access-token>`.
- `{...PublicId}`: UUID public expose ra API; không expose internal database ID.
- `DELETE archive`: soft archive.
- `DELETE hard`: xóa vật lý.

## High Level Product Flow

```text
Auth
`-- User session
    `-- Project
        |-- Target API Connector
        |   `-- Connector test run
        |-- Requirement
        |-- Dataset
        |   `-- Test Case
        |-- Rubric
        |   `-- Rubric Version
        |       `-- Rubric Criterion
        |-- Evaluation Run / Job
        |   |-- Evaluation Result
        |   `-- Job Event
        `-- Future
            |-- QC Review
            `-- Export
```

## API Resource Tree

```text
/
|-- /api/v1/auth [public]
|   |-- POST /register
|   |   `-- Tạo local user trạng thái pending và gửi link verify email
|   |-- POST /login
|   |   `-- Trả access token trong JSON, set refresh_token HttpOnly cookie
|   |-- POST /refresh-token
|   |   `-- Đọc refresh_token cookie, rotate cookie, trả access token mới
|   |-- POST /logout
|   |   `-- Clear refresh_token cookie
|   |-- POST /verify-email
|   |   `-- Active pending account bằng opaque email verification token
|   |-- POST /forgot-password
|   |   `-- Gửi reset link nếu account tồn tại, luôn trả 204
|   `-- POST /reset-password
|       `-- Đặt password mới bằng opaque reset token
|
|-- /api/v1/users [auth]
|   `-- GET /me
|       `-- Lấy user đang đăng nhập
|
|-- /api/v1/projects [auth]
|   |-- POST /
|   |   `-- Tạo project thuộc user đang đăng nhập
|   |-- GET /
|   |   `-- List project của user; filter: status; pageable/sortable
|   |-- GET /{projectPublicId}
|   |   `-- Chi tiết project
|   |-- PATCH /{projectPublicId}
|   |   `-- Update metadata/status của project
|   |-- DELETE /{projectPublicId} (archive)
|   |   `-- Set status ARCHIVED và archivedAt
|   |
|   |-- /{projectPublicId}/target-api-connectors [auth]
|   |   |-- POST /
|   |   |   `-- Tạo connector dưới project; secretValues chỉ được write
|   |   `-- GET /
|   |       `-- List connector của project; pageable/sortable
|   |
|   |-- /{projectPublicId}/requirements [auth]
|   |   |-- POST /
|   |   |   `-- Tạo requirement dưới project
|   |   `-- GET /
|   |       `-- List requirement của project; filter: status; pageable/sortable
|   |
|   |-- /{projectPublicId}/datasets [auth]
|   |   |-- POST /
|   |   |   `-- Tạo dataset dưới project, có thể gắn requirement
|   |   `-- GET /
|   |       `-- List dataset của project; filter: status; pageable/sortable
|   |
|   `-- /{projectPublicId}/rubrics [auth]
|       |-- POST /
|       |   `-- Tạo rubric dưới project
|       `-- GET /
|           `-- List rubric của project; filter: status; pageable/sortable
|
|   `-- /{projectPublicId}/evaluation-runs [auth]
|       |-- POST /
|       |   `-- Tạo evaluation run (202); validate dataset/rubric/connector
|       `-- GET /
|           `-- List evaluation runs của project; pageable/sortable
|
|-- /api/v1/target-api-connectors [auth]
|   `-- /{connectorPublicId}
|       |-- GET /
|       |   `-- Chi tiết connector; không trả raw secret
|       |-- PATCH /
|       |   `-- Update connector; secretValues thay raw values bằng placeholders
|       `-- POST /test-runs
|           `-- Chạy thử 1 request qua connector; hiện chỉ extract $.answer
|
|-- /api/v1/requirements [auth]
|   `-- /{requirementPublicId}
|       |-- GET /
|       |   `-- Chi tiết requirement
|       `-- PATCH /
|           `-- Update content/status; version chỉ tăng khi content đổi
|
|-- /api/v1/datasets [auth]
|   `-- /{datasetPublicId}
|       |-- GET /
|       |   `-- Chi tiết dataset
|       |-- PATCH /
|       |   `-- Update dataset; approve cần 1-100 active test cases
|       `-- /test-cases [auth]
|           |-- POST /
|           |   `-- Tạo test case dưới dataset; reject nếu dataset archived
|           `-- GET /
|               `-- List test case của dataset; filter: status; pageable/sortable
|
|-- /api/v1/test-cases [auth]
|   `-- /{testCasePublicId}
|       |-- PATCH /
|       |   `-- Update test case; reject nếu parent dataset archived
|       `-- DELETE / (hard)
|           `-- Xóa test case theo API contract hiện tại
|
|-- /api/v1/rubrics [auth]
|   `-- /{rubricPublicId}
|       |-- GET /
|       |   `-- Chi tiết rubric
|       |-- PATCH /
|       |   `-- Update rubric metadata; rubric archived sẽ reject mutation
|       |-- DELETE / (archive)
|       |   `-- Soft archive rubric
|       `-- /versions [auth]
|           |-- POST /
|           |   `-- Tạo draft version kế tiếp; version number do server quản
|           `-- GET /
|               `-- List rubric version; filter: status; pageable/sortable
|
|-- /api/v1/rubric-versions [auth]
|   `-- /{rubricVersionPublicId}
|       |-- GET /
|       |   `-- Chi tiết version kèm criteria
|       |-- PATCH /
|       |   `-- Publish/archive version; publish cần tổng criteria weight = 1.0000
|       `-- /criteria [auth]
|           |-- POST /
|           |   `-- Tạo criterion dưới draft version
|           `-- GET /
|               `-- List criteria theo sortOrder
|
|-- /api/v1/rubric-criteria [auth]
|   `-- /{criterionPublicId}
|       |-- PATCH /
|       |   `-- Update criterion dưới draft version
|       `-- DELETE /
|           `-- Xóa criterion dưới draft version
|
`-- /mock-chatbot [public]
    `-- POST /chat
        `-- Target API fallback public cho demo

|-- /api/v1/evaluation-runs [auth]
|   `-- /{runPublicId}
|       |-- GET /
|       |   `-- Chi tiết evaluation run
|       |-- /results [auth]
|       |   `-- GET /
|       |       `-- List kết quả; filter: judgeStatus; pageable/sortable
|       `-- /events [auth]
|           `-- GET /
|               `-- List job events theo thứ tự thời gian
|
`-- /api/v1/jobs [auth]
    `-- /{jobPublicId}
        `-- GET /
            `-- Chi tiết job; resourcePublicId resolved từ internal ID
```

## Domain Relationship Tree

```text
User
`-- owns Project qua project.createdBy
    |-- has many TargetApiConnector
    |   |-- lưu config bằng secret placeholders
    |   `-- có thể chạy one-off connector test calls
    |
    |-- has many Requirement
    |   `-- content version bắt đầu từ 1 và tăng khi content đổi
    |
    |-- has many Dataset
    |   |-- status: DRAFT, APPROVED, ARCHIVED
    |   `-- has many TestCase
    |       `-- status: ACTIVE, INACTIVE
    |
    `-- has many Rubric
        |-- status có ARCHIVED
        |-- currentVersion là null cho tới khi publish
        `-- has many RubricVersion
            |-- version tự tăng từ latest version
            |-- status quyết định có được mutate không
            `-- has many RubricCriterion
                |-- metricKey unique trong từng version
                `-- tổng weight khi publish phải bằng 1.0000

User
`-- owns EvaluationRun qua evaluation_runs.createdBy
    |-- links Job (1:1, job tracks async processing)
    |   `-- has many JobEvent (status changes, progress)
    |-- has many EvaluationResult (1 per test case)
    |   `-- judgeStatus: PASS, FAIL, WARNING, ERROR
    |-- references Dataset (must be APPROVED)
    |-- references RubricVersion (must be PUBLISHED)
    `-- references TargetApiConnector (must be active)
```

## Main Workflow Tree

```text
1. POST /api/v1/auth/register
2. POST /api/v1/auth/verify-email
3. POST /api/v1/auth/login
4. GET  /api/v1/users/me
5. POST /api/v1/projects
6. POST /api/v1/projects/{projectPublicId}/target-api-connectors
7. POST /api/v1/target-api-connectors/{connectorPublicId}/test-runs
8. POST /api/v1/projects/{projectPublicId}/requirements
9. POST /api/v1/projects/{projectPublicId}/datasets
10. POST /api/v1/datasets/{datasetPublicId}/test-cases
11. PATCH /api/v1/datasets/{datasetPublicId}
    `-- approve dataset sau khi có active test cases
12. POST /api/v1/projects/{projectPublicId}/rubrics
13. POST /api/v1/rubrics/{rubricPublicId}/versions
14. POST /api/v1/rubric-versions/{rubricVersionPublicId}/criteria
15. PATCH /api/v1/rubric-versions/{rubricVersionPublicId}
    `-- publish khi có criteria và tổng weight = 1.0000
16. POST /api/v1/projects/{projectPublicId}/evaluation-runs
    `-- tạo evaluation run + job, trả 202
17. GET  /api/v1/jobs/{jobPublicId}
    `-- poll job progress
18. GET  /api/v1/evaluation-runs/{runPublicId}
    `-- xem kết quả tổng
19. GET  /api/v1/evaluation-runs/{runPublicId}/results
    `-- xem kết quả chi tiết từng test case
20. Worker consumes Redis queue `vqc:jobs:queue`
    `-- mock promptfoo executor writes evaluation results and job events
21. Future: QC review results
22. Future: export
```

## Cross-Cutting Rules

- Authenticated APIs owner-scoped qua username/email đang đăng nhập và project `createdBy`.
- Protected APIs dùng access JWT trong `Authorization` header.
- Refresh token flow chỉ dùng `refresh_token` HttpOnly cookie.
- Error response dùng Problem Details cộng thêm `code` và optional field-level `errors`.
- Connector `secretValues` chỉ write-only; response expose masked `secretRefs`, không expose raw secrets.
- Public API response nên expose `publicId`, không expose internal `id`.
- Docs target có thể nhắc `/api/v1/sessions`, nhưng implementation hiện tại dùng `/api/v1/auth`.

## Known Gaps To Attach Later

```text
Project
|-- Evaluation Run / Job
|   |-- Worker + Promptfoo mock executor [done]
|   |-- QC Review [future]
|   `-- Export [future]
|       `-- flexible mapping; optional missing fields thì để blank
`-- Connector secrets encryption [future]
```
