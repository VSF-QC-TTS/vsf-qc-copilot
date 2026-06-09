# 06. Phân công Week 3–4 — Long / Trường

## 1. Context

Team mình hiện có 2 người:

```text
Long + Trường
```

Mục tiêu Week 3–4 là chuyển từ PoC promptfoo CLI sang một MVP platform nội bộ chạy được end-to-end.

Sau feedback mentor, scope đã đổi một chút:

```text
Không làm platform cố định cho một chatbot API cụ thể.
Phải làm Dynamic API Connector để sau này chỉ cần paste cURL/config là dùng được.
```

Ngoài ra vì hiện tại có thể chưa có API chatbot nội bộ để test, team cần dựng mock/demo chatbot API trước để không bị block.

## 2. Mục tiêu chung đến cuối Week 4

Demo được flow:

```text
Login
→ Create Project
→ Configure Dynamic API Connector / use Mock Connector
→ Input Business Requirement
→ Create/Import Dataset under 100 cases
→ QC Review/Approve Dataset
→ Create Rubric
→ Run Evaluation via Promptfoo
→ View Dashboard
→ QC Override Final Status
→ Export Excel/JSON
```

## 3. Nguyên tắc chia việc

Mình nghĩ chia theo ownership, không chia kiểu mỗi người làm một chút cho mọi thứ.

```text
Long chịu trách nhiệm core backend + integration để flow chạy được.
Trường chịu trách nhiệm UI + QC workflow để người dùng thao tác được.
```

Nếu backend chưa xong, frontend có thể mock API response trước.
Nếu frontend chưa xong, backend vẫn phải test được qua Postman/cURL.

## 4. Phân công tổng quan

| Người | Ownership chính | Output cuối Week 4 |
|---|---|---|
| Long | Backend, architecture, DB, REST API, Dynamic API Connector, mock chatbot API, Redis job, worker, promptfoo runner, export backend, deployment/report | Backend chạy được end-to-end từ connector + dataset + rubric → promptfoo eval → DB result → export |
| Trường | Frontend, QC workflow UI, connector UI, dataset/rubric UI, dashboard, review/export UI, test checklist | UI thao tác được end-to-end, QC có thể nhìn dashboard, review case và export |

## 5. Long phụ trách

### 5.1 Backend foundation

```text
- Setup Spring Boot project
- Setup PostgreSQL + Redis docker-compose
- Setup Flyway migration
- Setup package structure
- Setup health check
```

Package style nên dùng:

```text
controller
service
service/impl
repository
entity
mapper
request
response
enums
exception
config
```

### 5.2 Auth + Project

```text
- Simple username/password login
- User entity
- Project entity/API
- Basic JWT/session nếu cần
```

Expected API:

```http
POST /api/v1/sessions
GET  /api/v1/users/me
POST /api/v1/projects
GET  /api/v1/projects
GET  /api/v1/projects/{projectId}
PATCH /api/v1/projects/{projectId}
```

### 5.3 Dynamic API Connector backend

Long làm phần quan trọng nhất sau feedback mentor:

```text
- api_connectors table
- Manual connector create/update API
- raw_curl field
- method/url/headers/bodyTemplate/responseSelector
- Secret masking basic
- Connector test-run nếu kịp
- cURL parser draft nếu còn thời gian
```

Expected API:

```http
POST /api/v1/api-connector-drafts
POST /api/v1/projects/{projectId}/api-connectors
GET  /api/v1/projects/{projectId}/api-connectors
GET  /api/v1/api-connectors/{connectorId}
PATCH /api/v1/api-connectors/{connectorId}
```

### 5.4 Mock chatbot API

Vì chưa chắc có API nội bộ, Long dựng mock target:

```http
POST /mock-chatbot/chat
```

Response tối thiểu:

```json
{
  "answer": "..."
}
```

Mock nên có một số logic đơn giản để tạo pass/fail/warning demo được.

### 5.5 Dataset backend

```text
- datasets table
- test_cases table
- dataset source_type: MANUAL / IMPORTED_EXCEL / SAMPLE_DEMO / GENERATED
- CRUD test cases
- approve dataset
```

Dataset generation bằng Gemini/OpenAI là P1. Nếu chưa kịp thì dùng sample/import/manual trước.

### 5.6 Rubric backend

```text
- rubrics
- rubric_versions
- rubric_criteria
- Dynamic criteria
- Version/publish status
- Generate judge instruction from criteria
```

### 5.7 Evaluation job + Promptfoo runner

```text
- evaluation_runs
- evaluation_results
- jobs
- job_events
- Redis queue
- Worker consumes job
- Generate promptfoo config/test files
- Execute promptfoo CLI
- Parse JSON output
- Save normalized results
```

### 5.8 Export backend

```text
- Export Excel
- Export JSON
- Flexible mapping: field nào có thì fill, thiếu thì bỏ qua
- export_files metadata
```

### 5.9 Long output checklist

Long cần đảm bảo cuối Week 4 có:

```text
[ ] Backend run local được
[ ] DB migration chạy được
[ ] Login API chạy được
[ ] Project API chạy được
[ ] API Connector lưu được
[ ] Mock chatbot API chạy được
[ ] Dataset/rubric API chạy được
[ ] Evaluation job chạy được
[ ] Promptfoo CLI được gọi từ worker
[ ] Result lưu DB
[ ] Export Excel/JSON được
[ ] Có demo script/report
```

## 6. Trường phụ trách

### 6.1 Frontend foundation

```text
- React project setup
- Routing/layout
- API client
- Auth state
- Basic UI components
```

### 6.2 Login + Project UI

```text
- Login page
- Project list
- Project detail
- Create/edit project form
```

### 6.3 API Connector UI

Đây là màn quan trọng vì mentor muốn paste cURL/config dynamic.

Trường làm UI:

```text
- Connector list
- Connector create/edit form
- Fields: method, url, headers, body template, response selector
- Paste cURL textarea nếu backend support draft
- Mock connector preset
```

UX nên làm đơn giản:

```text
Bước 1: Chọn mock connector hoặc nhập API
Bước 2: Nhập body template có {{question}}
Bước 3: Nhập response selector, ví dụ $.answer
Bước 4: Save connector
```

### 6.4 Dataset UI

```text
- Dataset list/detail
- Test case table
- Add/edit/delete test case
- Approve dataset button
- Show status DRAFT/APPROVED
```

MVP chưa cần Excel import UI quá đẹp nếu chưa kịp. Có thể dùng sample dataset button.

### 6.5 Rubric Builder UI

```text
- Rubric list/detail
- Criteria table
- Add/edit/delete criterion
- Weight/pass/fail/judge instruction fields
- Publish/use version
```

### 6.6 Evaluation Dashboard UI

```text
- Run evaluation button
- Job status/progress
- Result table
- Filter PASS/FAIL/WARNING/ERROR
- Show question, expected, actual, judge reason
```

Nếu SSE chưa ready, UI dùng polling:

```text
GET /api/v1/evaluation-runs/{runId}
GET /api/v1/evaluation-runs/{runId}/results
```

### 6.7 QC Review UI

```text
- QC final status dropdown
- QC note
- PIC bug
- Save review decision
```

### 6.8 Export UI

```text
- Export Excel button
- Export JSON button
- Download link/status
- Validate output có đúng các field chính
```

### 6.9 Trường output checklist

Trường cần đảm bảo cuối Week 4 có:

```text
[ ] Login UI dùng được
[ ] Project UI dùng được
[ ] Connector UI dùng được
[ ] Dataset UI dùng được
[ ] Rubric UI dùng được
[ ] Run evaluation từ UI được
[ ] Dashboard result table dùng được
[ ] Filter FAIL/WARNING được
[ ] QC override được
[ ] Export button dùng được
[ ] Có checklist test/demo
```

## 7. Daily Sync Suggestion

Mỗi ngày sync 10–15 phút là đủ:

```text
1. Hôm qua làm gì?
2. Hôm nay làm gì?
3. Blocker gì?
4. Có ảnh hưởng demo flow không?
```

Không nên sync quá lâu, vì Week 3–4 cần build nhanh.

## 8. Integration Milestones

### End Week 3

Backend phải demo được bằng Postman/cURL:

```text
Project + Connector + Dataset + Rubric
→ Create evaluation run
→ Worker runs promptfoo
→ DB has result
```

Frontend có thể chưa đầy đủ nhưng nên có skeleton.

### Mid Week 4

UI phải gọi được backend:

```text
Create project
→ Create connector
→ Dataset/rubric screen
→ Run evaluation
```

### End Week 4

Demo hoàn chỉnh:

```text
UI end-to-end
→ dashboard
→ QC override
→ export
```

## 9. Những phần không nên ôm quá sớm

```text
- Deep fork promptfoo source
- Full red-team UI
- Streaming nhiều format
- Perfect cURL parser
- Full Excel template 35 cột nếu chưa rõ mapping
- Beautiful dashboard chart
- Multi-user permission phức tạp
```

## 10. Giọng báo cáo khi nói với mentor

```text
Em chia team theo hướng Long giữ core backend/integration để đảm bảo flow chạy được, Trường giữ frontend/QC workflow để đảm bảo demo thao tác được. Vì thời gian chỉ có 2 tuần và API nội bộ chưa chắc có sẵn, em sẽ dựng mock chatbot API trước để không bị block. Khi có API thật thì mình chỉ cần thay Dynamic API Connector/cURL chứ không phải sửa lại core flow.
```
