# 01. Product Requirement Document — VSF QC Copilot / Updated after Mentor Feedback

## 1. Overview

**VSF QC Copilot** là platform nội bộ hỗ trợ QC đánh giá chatbot/LLM application hiệu quả hơn.

Hệ thống không thay thế QC. QC vẫn là người quyết định cuối cùng. Platform đóng vai trò như copilot giúp QC:

```text
Generate / import dataset
→ manage rubric
→ configure API dynamically
→ run evaluation
→ review suspicious cases
→ export report
```

Promptfoo được dùng như evaluation engine (bộ máy đánh giá) ở bên dưới. VSF QC Copilot cung cấp workflow dễ dùng hơn cho QC, gồm UI, project management, dynamic API connector, dataset review, rubric versioning, dashboard và export.

## 2. Background

Flow hiện tại của QC đang gồm nhiều bước thủ công:

```text
Business Requirement
→ QC đọc nghiệp vụ
→ QC tạo/query data
→ QC dùng ChatGPT/Gemini sinh test cases
→ QC sinh ground truth
→ QC kiểm lại ground truth
→ QC copy vào Excel
→ Tool gọi chatbot API
→ LLM judge
→ Export Excel
→ QC review từng dòng
```

Các vấn đề chính:

- Dataset creation còn thủ công.
- Rubric chưa được tách thành criteria rõ ràng.
- Ground truth do LLM sinh nhưng QC vẫn phải verify.
- Tool hiện tại phụ thuộc Excel và khó track trend.
- Khi muốn test chatbot/API khác thì schema API có thể khác nhau.
- Hiện tại có rủi ro chưa có API chatbot nội bộ ổn định để demo ngay.

## 3. Mentor Feedback Incorporated

Mentor đã chốt một số điểm ảnh hưởng trực tiếp tới PRD:

| Topic | Decision |
|---|---|
| API integration | Không cố định cho một chatbot. Sau này chỉ cần paste/truyền cURL là có thể sử dụng |
| Streaming API | Chưa cần chốt ngay. MVP ưu tiên non-streaming, streaming để optional |
| Excel export | Theo format hiện tại, field nào thiếu thì bỏ qua |
| LLM provider | Ưu tiên Gemini và OpenAI vì công ty đang có key |
| Authentication | Simple username/password là đủ cho MVP |
| Run size demo | `<100` test cases là hợp lý |
| Internal API availability | Có thể chưa có API nội bộ ngay, cần mock/demo chatbot hoặc free chatbot API để test trước |

## 4. Target Users

Primary user:

```text
QC team
```

Secondary users later:

```text
BA / PM / Dev / Tech Lead
```

MVP chỉ tập trung cho QC trước.

## 5. Product Goals

### 5.1 Main Goal

Giảm các phần việc lặp lại của QC trong quá trình test chatbot, nhưng vẫn giữ QC là final reviewer.

### 5.2 Specific Goals

- QC có thể tạo project cho một evaluation scope.
- QC có thể cấu hình target API bằng form hoặc paste cURL.
- QC có thể dùng mock/demo chatbot API khi chưa có API nội bộ.
- QC có thể nhập business requirement bằng tiếng Việt.
- Hệ thống có thể generate/import/manual dataset.
- QC có thể review/edit/approve dataset trước khi run.
- QC có thể tạo rubric dynamic criteria.
- Hệ thống generate promptfoo config từ dataset + rubric + connector.
- Hệ thống chạy evaluation async qua worker.
- Hệ thống dùng Gemini/OpenAI cho dataset generation và judge nếu được cấu hình.
- Dashboard hiển thị pass/fail/warning/error.
- QC có thể override final status.
- Export Excel/JSON theo format QC-friendly.

## 6. Non-goals for MVP

MVP Week 3–4 **không** tập trung vào:

- Replacing QC final decision.
- Deep customization promptfoo source/UI.
- Auto SQL generation.
- Auto mock DB generation.
- Complex permission model.
- Company SSO.
- Multi-tenant organization.
- Full red-team UI.
- Advanced analytics.
- Stress test 500+ cases.

## 7. Core Concepts

### 7.1 Project

Project đại diện cho một phạm vi đánh giá.

Cách hiểu cũ:

```text
1 project = 1 chatbot
```

Cách hiểu mới sau feedback mentor:

```text
1 project = 1 evaluation scope
Project có thể gắn một hoặc nhiều API connector trong tương lai
MVP dùng một active connector cho mỗi evaluation run
```

### 7.2 Dynamic API Connector

Dynamic API Connector là config giúp hệ thống gọi được nhiều loại chatbot/API khác nhau.

Input có thể là:

```text
- Paste cURL
- Manual form: method, url, headers, body template, response selector
```

Connector cần lưu:

```text
method
url
headers
body_template
query_params
auth_type
response_selector
is_streaming
streaming_type
retry/timeout
```

### 7.3 Business Requirement

Business Requirement là mô tả nghiệp vụ QC muốn test. Đây là input cho dataset generator hoặc để QC tự tạo/import dataset.

### 7.4 Dataset

Dataset gồm test cases.

Mỗi test case tối thiểu có:

```text
question
precondition/context
ground_truth
metadata
```

Dataset có thể đến từ:

```text
GENERATED
IMPORTED_EXCEL
MANUAL
SAMPLE_DEMO
```

### 7.5 Rubric

Rubric là bộ tiêu chí chấm câu trả lời chatbot.

Rubric phải dynamic, không hard-code.

Một criterion nên có:

```text
name
description
weight
pass_condition
fail_condition
judge_instruction
is_critical
```

### 7.6 Evaluation Run

Evaluation Run là một lần chạy dataset qua target API connector và judge kết quả theo rubric.

### 7.7 QC Review Decision

Judge result chỉ là auto status.

Final status là do QC set:

```text
Auto status: PASS / FAIL / WARNING / ERROR
QC final status: PASS / FAIL / NEED_FIX / IGNORED / NOT_REVIEWED
```

## 8. Functional Requirements

### FR-01 — Simple Authentication

MVP dùng username/password.

Không cần company SSO ở Week 3–4.

### FR-02 — Project Management

QC có thể:

```text
create project
view project list
update project
archive project
```

### FR-03 — Dynamic API Connector

QC/dev có thể tạo connector bằng manual form.

Required fields:

```text
name
method
url
headers
body_template
response_selector
is_streaming
timeout_seconds
retry_count
```

### FR-04 — cURL Import / Connector Draft

Hệ thống nên cho paste cURL để tạo connector draft.

MVP có thể làm mức cơ bản:

```text
raw_curl
→ parse method/url/headers/body
→ user review/edit
→ save connector
```

Nếu parse cURL chưa đủ tốt thì fallback là manual form.

### FR-05 — Response Selector

Vì mỗi API có response format khác nhau, connector cần có response selector.

Ví dụ:

```text
$.answer
$.data.message
$.choices[0].message.content
```

MVP ưu tiên JSON response non-streaming.

### FR-06 — Mock/Demo Chatbot API

Vì hiện tại có thể chưa có API nội bộ, MVP cần mock/demo chatbot API.

Mock API giúp chứng minh end-to-end flow:

```text
Dataset
→ Connector
→ Evaluation
→ Judge
→ Dashboard
→ Export
```

### FR-07 — Business Requirement Input

QC có thể nhập/lưu requirement dạng free-text tiếng Việt.

### FR-08 — Dataset Generation / Import / Manual

Hệ thống support ít nhất một đường chính để tạo dataset.

Priority:

```text
1. Import/sample/manual dataset để không bị block
2. Dataset generation bằng Gemini/OpenAI nếu kịp
```

### FR-09 — Dataset Review/Edit/Approve

QC có thể review, sửa và approve dataset.

Chỉ approved test cases mới được dùng trong official evaluation run.

### FR-10 — Rubric Builder

QC có thể tạo rubric và criteria động.

### FR-11 — Rubric Versioning

Rubric cần version để sau này so sánh kết quả theo tiêu chí khác nhau.

### FR-12 — Promptfoo Config Generation

Backend/worker generate promptfoo config từ:

```text
project
dataset
rubric version
api connector
judge provider config
```

QC không cần viết YAML thủ công.

### FR-13 — Evaluation Execution

QC tạo evaluation run từ UI.

Run phải async:

```text
API creates run/job
→ queue job
→ worker executes
→ results saved to DB
```

### FR-14 — Result Dashboard

Dashboard tối thiểu hiển thị:

```text
test case id
question
expected answer
actual answer
auto status
judge score
judge reason
qc final status
qc note
pic bug
```

### FR-15 — QC Override

QC có thể override final status.

Ví dụ:

```text
Auto status: FAIL
QC final status: PASS
Reason: Judge hiểu sai rule nghiệp vụ
```

### FR-16 — Export Excel

Export theo format hiện tại của QC.

Rule:

```text
Có field nào thì fill field đó
Field nào thiếu hoặc chưa map được thì bỏ qua/để trống
Không block export vì thiếu một vài cột
```

### FR-17 — Export JSON

Export JSON phục vụ debug/kỹ thuật.

### FR-18 — LLM Provider Config

MVP ưu tiên Gemini và OpenAI cho:

```text
dataset generation
judge / LLM-as-Judge
```

### FR-19 — Run Size Limit

MVP demo target:

```text
<100 test cases
```

Recommended demo size:

```text
30–80 test cases
```

## 9. MVP Scope

### P0 — Must Have

```text
Simple login
Project CRUD
Dynamic API Connector manual form
Mock/demo chatbot API
Business requirement input
Dataset import/manual/sample
Dataset review/approve
Rubric builder v1
Evaluation run async
Promptfoo CLI runner
Result dashboard
QC final decision
Excel export
JSON export
```

### P1 — Should Have

```text
Paste cURL parser
Response selector JSONPath
Dataset generation by Gemini/OpenAI
SSE progress
Run history
Result filters
Basic trend
```

### P2 — Could Have

```text
Streaming API support
Red-team backend demo
Red-team UI
Rubric templates
Advanced report summary
```

## 10. Success Metrics

MVP được coi là thành công nếu:

```text
- Tạo được project
- Tạo được connector bằng manual form hoặc paste cURL draft
- Có mock/demo chatbot để chạy khi chưa có API nội bộ
- Có dataset dưới 100 cases
- QC approve dataset được
- Rubric tạo/edit được
- Run evaluation được
- Kết quả lưu DB và hiện dashboard
- QC override được
- Export Excel/JSON được
```

Business success:

```text
QC không còn phải chỉ nhìn Excel thô, mà có dashboard để tập trung review FAIL/WARNING/suspicious cases trước.
```

## 11. Open Questions

Sau mentor feedback, số câu hỏi còn lại ít hơn:

```text
1. Khi có API nội bộ thật thì response format cụ thể là gì?
2. Streaming có bắt buộc ở Week 4 demo không?
3. Excel template hiện tại có sheet/cột nào bắt buộc phải giữ?
4. Dataset generation có cần demo chính thức hay import/sample dataset là đủ cho Week 4?
5. Red-team có cần nhắc trong demo hay chỉ để roadmap?
```
