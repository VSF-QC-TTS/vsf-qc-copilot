# E2E Frontend Input Sheet

Mục tiêu: dùng file này để copy/paste dữ liệu vào UI sau khi chạy:

```bash
docker compose up --build
```

Mở frontend:

```text
http://localhost:3000/vi/login
```

## Lưu Ý Trước Khi Chạy

- Frontend tạo rubric hiện đi qua AI preview, nên `server/.env` cần có `GEMINI_API_KEY` thật nếu muốn tạo rubric hoàn toàn bằng UI.
- Nếu chỉ muốn smoke workflow nhanh, backend `dev` đang dùng `vqc.promptfoo.mode=mock`, evaluation không gọi Promptfoo thật.
- Sau khi register bằng UI, user mặc định còn `PENDING_EMAIL_VERIFICATION`. Chạy lệnh DB activation bên dưới rồi login.
- Trong form test case, tạm thời để trống `Precondition` và `Metadata` khi nhập bằng UI. Form hiện đang gửi string, trong khi backend contract là JSON object.

## 1. Register

Trang:

```text
http://localhost:3000/vi/register
```

Điền:

| Field | Value |
| --- | --- |
| Email | `e2e-qc@test.local` |
| Display name | `E2E QC User` |
| Password | `TestPass123!` |
| Confirm password | `TestPass123!` |

Sau khi register xong, activate user bằng terminal:

```bash
docker compose exec -T db psql -U vqc -d vqc_dev -c \
  "UPDATE users SET status = 'ACTIVE' WHERE username = 'e2e-qc@test.local';"
```

## 2. Login

Trang:

```text
http://localhost:3000/vi/login
```

Điền:

| Field | Value |
| --- | --- |
| Email | `e2e-qc@test.local` |
| Password | `TestPass123!` |

## 3. Project

Vào `Projects` rồi tạo project.

| Field | Value |
| --- | --- |
| Name | `E2E QC Project` |
| Description | `Project dùng để chạy manual end-to-end qua frontend.` |

Sau khi tạo xong, mở project vừa tạo.

## 4. Target API Connector

Trong project, vào `Connectors` -> `Create connector`.

| Field | Value |
| --- | --- |
| Name | `Local Mock Chatbot` |
| Description | `Connector gọi mock chatbot nội bộ để chạy E2E.` |
| Raw cURL | copy block bên dưới |
| Response selector | `$.answer` |
| Timeout seconds | `60` |
| Retry count | `1` |

Raw cURL:

```bash
curl -X POST 'http://localhost:8080/mock-chatbot/chat' \
  -H 'Content-Type: application/json' \
  -d '{"message":"{{question}}"}'
```

Sau khi tạo connector, mở detail connector và chạy test-run.

Connector test-run input:

| Field | Value |
| --- | --- |
| Question | `How many steps did I walk today?` |
| Precondition | leave blank |
| Metadata | leave blank |

Expected result:

```text
Mock answer: How many steps did I walk today?
```

## 5. Dataset

Trong project, vào `Datasets` -> `Create dataset`.

| Field | Value |
| --- | --- |
| Name | `E2E Steps Dataset` |
| Description | `Dataset nhỏ để kiểm tra evaluation từ frontend.` |

Mở dataset vừa tạo, thêm test cases.

### Test Case 1

| Field | Value |
| --- | --- |
| Question | `How many steps did I walk today?` |
| Ground truth | `Mock answer: How many steps did I walk today?` |
| Precondition | leave blank |
| Metadata | leave blank |

### Test Case 2

| Field | Value |
| --- | --- |
| Question | `What is the recommended daily water intake?` |
| Ground truth | `Mock answer: What is the recommended daily water intake?` |
| Precondition | leave blank |
| Metadata | leave blank |

### Test Case 3

| Field | Value |
| --- | --- |
| Question | `How many hours of sleep does an adult need?` |
| Ground truth | `Mock answer: How many hours of sleep does an adult need?` |
| Precondition | leave blank |
| Metadata | leave blank |

Sau khi có ít nhất 1 active test case, bấm `Approve dataset`.

Expected:

```text
Dataset status = APPROVED
```

## 6. Rubric

Vào sidebar `Rubrics` -> `Create rubric`.

### Generate Preview Form

| Field | Value |
| --- | --- |
| Rubric name | `E2E Semantic Answer Rubric` |
| Language | `vi` |
| Evaluation goal | copy block bên dưới |
| Domain context | copy block bên dưới |
| Sample question | `How many steps did I walk today?` |
| Sample expected answer | `Mock answer: How many steps did I walk today?` |
| Extra instructions | copy block bên dưới |

Evaluation goal:

```text
Đánh giá câu trả lời chatbot bằng cách so sánh ngữ nghĩa với expected answer của từng test case. Không yêu cầu khớp chuỗi tuyệt đối nếu ý nghĩa tương đương.
```

Domain context:

```text
Đây là luồng E2E cho QC chatbot. Target API là mock chatbot nội bộ, thường trả về câu trả lời dạng "Mock answer: {question}". Rubric cần ưu tiên kiểm tra actual answer có giữ đúng ý chính của ground truth hay không.
```

Extra instructions:

```text
Tạo 3 criteria độc lập cho Promptfoo llm-rubric. Metric key phải là lowercase snake_case. Weight là số nguyên 1-100. Criteria nên dùng question, expected answer, precondition, metadata và actual output làm context.
```

Bấm `Generate preview`.

### Preview Values

Nếu AI sinh nội dung khác quá nhiều, chỉnh lại preview theo các giá trị dưới đây trước khi bấm `Create`.

Rubric content:

```text
Judge the chatbot answer against the user question, expected answer, preconditions, and metadata. Use semantic equivalence. Do not require exact wording unless a criterion explicitly requires exact wording.
```

Output schema:

```json
{"type":"object","required":["final_status","scores","reason"],"properties":{"final_status":{"enum":["PASS","FAIL","WARNING"]},"scores":{"type":"object"},"reason":{"type":"string"}}}
```

Criterion 1:

| Field | Value |
| --- | --- |
| Name | `Expected Answer Match` |
| Metric key | `expected_answer_match` |
| Weight | `60` |
| Critical | checked |
| Judge instruction | `Compare the actual output with the expected answer for the same question. Use semantic equivalence and consider testcase context.` |
| Pass condition | `The answer preserves the core meaning and facts of the expected answer.` |

Criterion 2:

| Field | Value |
| --- | --- |
| Name | `Clarity` |
| Metric key | `clarity` |
| Weight | `25` |
| Critical | unchecked |
| Judge instruction | `Assess whether the actual answer is understandable and directly responds to the question.` |
| Pass condition | `The answer is clear, readable, and not confusing.` |

Criterion 3:

| Field | Value |
| --- | --- |
| Name | `No Fabrication` |
| Metric key | `no_fabrication` |
| Weight | `15` |
| Critical | checked |
| Judge instruction | `Check whether the answer avoids inventing facts that are not supported by the expected answer or testcase context.` |
| Pass condition | `The answer does not add unsupported or contradictory facts.` |

Sau khi create, mở version detail và publish version.

Expected:

```text
Rubric version status = PUBLISHED
```

## 7. Judge Model

Quay lại project -> `Judge models`.

Nếu đang chạy mock mode, API key có thể là placeholder. Nếu chạy Promptfoo CLI thật, dùng key thật.

| Field | Value |
| --- | --- |
| Name | `E2E Gemini Judge` |
| Provider | `GEMINI` |
| Model name | `gemini-2.5-flash` |
| API key | `test-gemini-key-for-mock-mode` |
| Active | checked |
| Base URL | leave blank |
| Config JSON | leave blank |

Expected:

```text
Judge model active = true
```

## 8. Start Evaluation

Trong project, vào `Evaluations` -> `Start evaluation`.

Chọn:

| Field | Value |
| --- | --- |
| Dataset | `E2E Steps Dataset` |
| Rubric version | `E2E Semantic Answer Rubric v1` |
| Connector | `Local Mock Chatbot` |
| Judge model | `E2E Gemini Judge` |

Bấm `Start evaluation`.

Expected:

```text
Evaluation job starts, progress polling appears, then app navigates to run detail.
```

Nếu cả project chỉ có đúng 1 dataset approved, 1 published rubric version, 1 active connector, và 1 active judge model, có thể dùng `Quick evaluate`.

## 9. Review Results

Trong run detail, mở results.

Kiểm tra:

| Check | Expected |
| --- | --- |
| Total results | `3` |
| Each result has question | yes |
| Each result has actual answer | yes |
| Each result has judge status | `PASS`, `FAIL`, `WARNING`, or `ERROR` |
| QC status before review | `NOT_REVIEWED` |

Lưu ý ở mock mode:

```text
Judge status/score có thể random vì MockPromptfooExecutor đang random PASS/FAIL/WARNING.
Actual answer thường bằng ground truth.
```

## 10. QC Review

Mở một result rồi tạo review decision.

| Field | Value |
| --- | --- |
| QC status | `PASS` |
| QC note | `E2E manual review accepted.` |
| PIC bug | leave blank |

Expected:

```text
Result qcStatus = PASS
Review note được lưu và đọc lại sau reload.
```

## 11. Export

Trong run detail hoặc results page, tạo export.

| Field | Value |
| --- | --- |
| Format | `EXCEL` |

Expected:

```text
Export job completes.
Downloaded file is an .xlsx file.
```

## Checklist Pass/Fail

| Step | Pass? | Note |
| --- | --- | --- |
| Register + activate + login |  |  |
| Create project |  |  |
| Create connector from raw cURL |  |  |
| Connector test-run returns `$.answer` |  |  |
| Create dataset |  |  |
| Create 3 test cases |  |  |
| Approve dataset |  |  |
| Generate/create rubric |  |  |
| Publish rubric version |  |  |
| Create active judge model |  |  |
| Start evaluation |  |  |
| Job completes |  |  |
| Results visible |  |  |
| QC review saved |  |  |
| Export downloaded |  |  |

## Known UI Caveats To Watch

- `Precondition` and `Metadata` fields in test case forms should eventually send JSON objects, not strings. Leave them blank for this manual UI E2E.
- Rubric creation depends on AI preview. Without `GEMINI_API_KEY`, this step will not complete through frontend.
- Template clone currently clones rubric metadata only; do not use template clone as the main E2E rubric path until version/criteria cloning is confirmed.
- Docker backend currently runs in dev/mock Promptfoo mode unless config is changed. Use this input sheet for workflow smoke; use `docs/E2E_RUNBOOK.md` for API/CLI validation details.
