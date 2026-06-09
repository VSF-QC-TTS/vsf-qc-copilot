# 05. Review & Change List — Sau feedback mentor

## 1. Kết luận nhanh

Sau feedback mentor, bộ docs cũ không sai, nhưng cần chỉnh trọng tâm.

Điểm quan trọng nhất:

```text
Không design platform cố định cho một chatbot API.
Phải chuyển sang Dynamic API Connector để sau này chỉ cần paste cURL/config là dùng được.
```

Ngoài ra cần thêm:

```text
- Mock/demo chatbot API vì hiện chưa chắc có API nội bộ
- Run size demo <100 cases
- Simple login username/password
- Gemini/OpenAI cho dataset generation và judge
- Excel export flexible mapping, thiếu field thì bỏ qua
- Streaming để optional/P2
```

## 2. Mentor feedback → Plan change

| Mentor feedback | Plan change |
|---|---|
| API config dynamic, truyền cURL là dùng được | Đổi `chatbot_api_configs` thành `api_connectors`, thêm paste cURL/manual config |
| Không cố định cho chatbot nào | Đổi product model từ `1 project = 1 chatbot` sang `1 project = 1 evaluation scope` |
| Excel theo format hiện tại, thiếu trường bỏ qua | Export dùng flexible mapper, không fail vì thiếu field |
| LLM suggest Gemini/OpenAI | Provider config ưu tiên Gemini/OpenAI cho dataset generation/judge |
| Simple login | Bỏ SSO khỏi MVP |
| Run size <100 | Demo 30–80 cases, validation dưới 100 cases |
| Chưa có API nội bộ | Thêm mock/demo chatbot API hoặc public/free API connector |

## 3. File nào đã sửa gì?

### `00-ai-context.updated.md`

Đã bổ sung:

```text
- Mentor feedback summary
- Dynamic API Connector concept
- Demo strategy khi chưa có internal API
- Updated MVP scope P0/P1/P2
- Updated team context Long/Trường
```

### `01-prd.updated.md`

Đã bổ sung/sửa:

```text
- Product goal sau mentor feedback
- Dynamic API Connector requirements
- cURL import / connector draft
- Mock chatbot API requirement
- Gemini/OpenAI provider requirement
- Run size <100
- Excel flexible export
- P0/P1/P2 scope updated
```

### `02-architecture.updated.md`

Đã bổ sung/sửa:

```text
- Architecture diagram có Dynamic API Connector
- Container diagram có Mock Chatbot API + Gemini/OpenAI
- Data model đổi chatbot_api_configs → api_connectors
- REST API thêm /api-connector-drafts và /api-connectors
- Evaluation flow qua connector
- SSE + polling fallback
```

### `03-delivery-plan.updated.md`

Đã bổ sung/sửa:

```text
- Week 3–4 plan thực tế hơn
- P0/P1/P2 rõ ràng
- Day-by-day plan theo 2 tuần
- Mock API không để bị block
- Run size dưới 100
- Definition of Done
- Demo script updated
```

### `04-adr.updated.md`

Đã thêm ADR mới:

```text
- Use Dynamic API Connector
- Include Mock/Demo Chatbot API
- Use Gemini/OpenAI
- Simple Login
- Limit demo run size <100
- Flexible Excel mapping
- Non-streaming first
- SSE with polling fallback
- Red-team as P2
```

### `06-work-assignment-long-truong.updated.md`

Đã cập nhật phân công:

```text
Long: backend, API connector, mock chatbot, job queue, promptfoo runner, export, deployment/report
Trường: frontend, connector UI, dataset/rubric UI, dashboard, review/export validation, demo checklist
```

### `07-mentor-report-template.updated.md`

Đã cập nhật:

```text
- Tin nhắn reply mentor
- Daily report
- Bản nói miệng
- Checklist gửi kèm
```

## 4. Nội dung nên báo cáo lại mentor

Nên nói ngắn gọn:

```text
Sau feedback của anh, em đã chỉnh lại scope Week 3–4 theo hướng Dynamic API Connector. Tức là hệ thống không phụ thuộc vào một chatbot API cố định nữa. MVP sẽ cho phép tạo connector bằng manual form/paste cURL, dùng mock chatbot API để không bị block khi chưa có API nội bộ, và demo run size dưới 100 cases. Khi anh lấy được API nội bộ, mình chỉ cần thay connector/cURL là có thể chạy flow evaluation.
```

## 5. Các quyết định mới nên nhấn mạnh

```text
1. Dynamic API Connector là core change.
2. Mock chatbot API là fallback để không chờ API nội bộ.
3. Promptfoo vẫn là engine, nhưng không deep fork ngay.
4. Gemini/OpenAI dùng cho generation/judge theo key công ty có.
5. Demo dưới 100 cases để đảm bảo hoàn thành Week 3–4.
6. Excel export flexible, không bắt buộc đủ 100% field.
```

## 6. Risk còn lại

| Risk | Status | Mitigation |
|---|---|---|
| cURL parser khó cover đủ case | Medium | Manual form trước, parser P1 |
| Không có internal API | High | Mock chatbot API / public API target |
| Judge quality không ổn | Medium | Rubric rõ + QC override |
| Dataset generation chưa tốt | Medium | Import/manual/sample dataset fallback |
| Export template chưa chắc | Medium | Flexible mapping, missing field skip |
| UI làm không kịp | Medium | Ưu tiên dashboard table, chưa cần đẹp |
| Red-team bị kỳ vọng quá sớm | Medium | Nói rõ P2/roadmap |

## 7. File nên gửi mentor

Nên gửi gọn:

```text
- 03-delivery-plan.updated.md
- 06-work-assignment-long-truong.updated.md
- 07-mentor-report-template.updated.md nếu cần copy message
```

Nếu mentor muốn review sâu thì gửi cả bộ:

```text
00-ai-context.updated.md
01-prd.updated.md
02-architecture.updated.md
03-delivery-plan.updated.md
04-adr.updated.md
05-review-and-change-list.updated.md
06-work-assignment-long-truong.updated.md
07-mentor-report-template.updated.md
```
