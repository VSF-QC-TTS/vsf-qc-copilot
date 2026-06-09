# 03. Delivery Plan — VSF QC Copilot / Week 3–4 Updated Plan

## 1. Current Status

Hiện tại project đang ở cuối Week 2 / bắt đầu Week 3.

Đã có:

```text
- Promptfoo PoC chạy được bằng npx
- Đã test với file Excel thật của QC
- Đã hiểu flow QC hiện tại
- Đã có plan architecture ban đầu
- Mentor đã feedback 6 điểm quan trọng về API, Excel, LLM, auth, run size
```

Điểm thay đổi lớn nhất sau feedback mentor:

```text
Không design cứng cho một chatbot API cụ thể.
Phải chuyển sang Dynamic API Connector, tốt nhất sau này chỉ cần paste cURL là dùng được.
```

Ngoài ra hiện tại có rủi ro là công ty đang cắt giảm nhiều chatbot nội bộ, nên chưa chắc có API nội bộ để test ngay. Vì vậy MVP phải có mock/demo chatbot API hoặc một free/public API để không bị block.

## 2. Delivery Principle

Không cố làm full platform quá rộng trong 2 tuần.

Ưu tiên:

```text
Một end-to-end flow nhỏ nhưng chạy thật
→ không phụ thuộc API nội bộ
→ có demo dưới 100 test cases
→ có dashboard để QC review
→ có export để báo cáo
```

Không ưu tiên ở Week 3–4:

```text
- Deep custom promptfoo source
- Full red-team UI
- Auto SQL/mock DB generation
- Complex role/permission
- Company SSO
- Advanced analytics
- 500+ test cases optimization
```

## 3. MVP Scope Updated

### P0 — Must Complete

| Scope | Expected output |
|---|---|
| Simple auth | Login username/password |
| Project management | Tạo/xem/sửa/archive project |
| Dynamic API Connector manual | Nhập method/url/headers/body/response selector |
| Mock/demo chatbot API | Có target API để test khi chưa có API nội bộ |
| Business requirement | Nhập/lưu requirement tiếng Việt |
| Dataset | Import/manual/sample dataset, target `<100` cases |
| Dataset review | QC edit/approve test cases |
| Rubric builder | Tạo criteria động, version cơ bản |
| Evaluation run | Tạo run/job async |
| Promptfoo runner | Worker gọi promptfoo eval và parse output |
| Result dashboard | Xem expected/actual/status/reason |
| QC override | QC set final status, note, PIC bug |
| Export | Excel + JSON, field thiếu thì bỏ qua |

### P1 — Should Complete

| Scope | Expected output |
|---|---|
| Paste cURL parser | Parse cURL thành connector draft |
| Response selector | Lấy answer từ JSON bằng selector |
| Dataset generation | Generate dataset bằng Gemini/OpenAI |
| SSE progress | Progress bar/live status |
| Result filters | PASS/FAIL/WARNING/ERROR |
| Run history | Xem các run của project |
| Basic trend | So sánh pass rate giữa 2 run |

### P2 — Only If Time Allows

| Scope | Expected output |
|---|---|
| Streaming API support | Parser cho một format cụ thể nếu có API thật |
| Red-team minimal | Backend/CLI demo hoặc roadmap |
| Rubric templates | Một vài template tiêu chí |
| Advanced report | Summary đẹp hơn trong export |

## 4. Week 3 Plan — Backend Core + Connector + Runner

### Day 1 — Project Setup + Skeleton

Tasks:

```text
- Setup repo/project structure
- Setup Spring Boot backend
- Setup React frontend
- Setup PostgreSQL + Redis docker-compose
- Setup Flyway migration
- Setup package convention: controller/service/repository/entity/mapper/request/response/enums
- Setup simple health check
```

Deliverables:

```text
- Backend chạy được
- Frontend chạy được
- DB connect được
- Redis connect được
- Docker compose local chạy được
```

### Day 2 — Auth + Project + API Connector v1

Tasks:

```text
- Simple login username/password
- Project CRUD
- API Connector entity/table/API
- Manual connector form backend: method/url/headers/bodyTemplate/responseSelector
- Secret masking basic
```

Deliverables:

```text
- Login được
- Tạo project được
- Tạo API connector manual được
- Lưu được connector vào DB
```

### Day 3 — Mock Chatbot API + Dataset v1

Tasks:

```text
- Build mock/demo chatbot API
- Dataset entity/API
- Test case entity/API
- Import/sample/manual dataset path
- Dataset approve flow
```

Deliverables:

```text
- Có mock chatbot endpoint để target
- Tạo dataset dưới 100 cases được
- QC có thể edit/approve dataset
```

### Day 4 — Rubric Builder + Judge Config

Tasks:

```text
- Rubric/rubric version/rubric criteria tables
- Rubric CRUD API
- Dynamic criteria
- Generate judge instruction from criteria
- Configure Gemini/OpenAI provider for judge if needed
```

Deliverables:

```text
- Tạo rubric được
- Thêm/sửa/xóa criteria được
- Có rubric version dùng cho run
```

### Day 5 — Evaluation Job + Promptfoo Runner v1

Tasks:

```text
- Evaluation run table/API
- Job table/API
- Redis queue producer/consumer
- Worker skeleton
- Generate promptfoo config/test file
- Execute promptfoo CLI
- Parse JSON output
- Save evaluation result
```

Deliverables:

```text
Project + Connector + Dataset + Rubric
→ Run Evaluation
→ Worker executes promptfoo
→ Result saved to DB
```

## 5. Week 4 Plan — UI + Dashboard + Export + Demo

### Day 1 — UI Foundation

Tasks:

```text
- Login page
- Project list/detail
- API connector form
- Mock connector preset
- Basic layout/navigation
```

Deliverables:

```text
- QC login được
- QC tạo project được
- QC configure connector được
```

### Day 2 — Dataset + Rubric UI

Tasks:

```text
- Dataset page
- Test case table
- Add/edit/delete test case
- Approve dataset
- Rubric builder UI
- Criteria editor
```

Deliverables:

```text
- QC nhập/import/sample dataset được
- QC approve dataset được
- QC tạo rubric được
```

### Day 3 — Evaluation Run + Dashboard

Tasks:

```text
- Run evaluation button
- Job status polling/SSE
- Result table
- Show expected/actual/judge reason
- Filter PASS/FAIL/WARNING/ERROR
```

Deliverables:

```text
- QC chạy evaluation từ UI được
- QC xem kết quả được
- QC lọc failed/warning cases được
```

### Day 4 — QC Review + Export

Tasks:

```text
- QC final status field
- QC note
- PIC bug
- Export Excel
- Export JSON
- Validate missing-field behavior
```

Deliverables:

```text
- QC override được
- Export Excel/JSON được
- Field thiếu thì bỏ qua/để trống
```

### Day 5 — Stabilization + Demo Prep

Tasks:

```text
- Test demo 30–80 cases
- Fix UI edge cases
- Fix failed job handling
- Prepare demo script
- Prepare report
- Deploy MVP to VPS if ready
```

Deliverables:

```text
- MVP demo được end-to-end
- Có demo script
- Có report cho mentor
```

## 6. Team Split

Team có 2 người:

```text
Long + Trường
```

### Long — Backend / Architecture / Integration

Long ownership:

```text
- Backend setup
- DB schema/Flyway
- REST API
- Simple auth
- Dynamic API Connector backend
- Mock chatbot API
- Redis job queue
- Worker / Promptfoo Runner
- Gemini/OpenAI judge config
- Result persistence
- Export backend
- Deployment/report
```

### Trường — Frontend / QC Workflow / Validation

Trường ownership:

```text
- React setup
- Login/project UI
- API connector UI
- Dataset table/edit UI
- Rubric builder UI
- Evaluation run UI
- Result dashboard/filter
- QC review/override UI
- Export button/check output
- Test checklist/demo script support
```

### Shared

```text
- Confirm API connector UX
- Validate Excel export
- Prepare Week 4 demo
- Write known issues and fallback
```

## 7. Fallback Plan

| Risk | Fallback |
|---|---|
| Không có API nội bộ | Dùng mock chatbot API hoặc Gemini/OpenAI-compatible target |
| cURL parser chưa ổn | Manual connector form trước |
| Streaming khó | Non-streaming trước, streaming để P2 |
| Dataset generation chưa tốt | Import/sample/manual dataset trước |
| SSE chưa kịp | Polling job status 2–5s |
| Promptfoo CLI lỗi | Lưu raw output/error, demo với smaller sample |
| Excel thiếu field | Fill field map được, thiếu thì bỏ qua |
| Run quá chậm | Demo 30–80 cases, maxConcurrency thấp |

## 8. Definition of Done

MVP Week 4 được coi là done nếu demo được flow:

```text
Login
→ Create Project
→ Create Dynamic API Connector / use Mock Connector
→ Input Requirement
→ Create/Import Dataset under 100 cases
→ Approve Dataset
→ Create Rubric
→ Run Evaluation
→ View Dashboard
→ Filter FAIL/WARNING
→ QC Override
→ Export Excel/JSON
```

## 9. Demo Script

```text
1. Open VSF QC Copilot
2. Login as demo QC user
3. Create project: AI Health Chatbot Demo
4. Create connector by manual form or paste cURL
5. Use Mock Chatbot API as target
6. Add business requirement
7. Import/sample 30–80 test cases
8. Approve dataset
9. Create rubric with 3–5 criteria
10. Run evaluation
11. Watch progress/status
12. Open dashboard
13. Filter FAIL/WARNING cases
14. Override one case as QC
15. Export Excel
16. Export JSON
17. Explain that when internal API is available, only connector/cURL needs to be replaced
```