# 04. Architecture Decision Records — VSF QC Copilot / Updated ADRs

## ADR-001 — Use VSF QC Copilot as Product Name

### Status

Accepted

### Context

Platform được build quanh promptfoo nhưng không nên lấy promptfoo làm product identity.

### Decision

Use name:

```text
VSF QC Copilot
```

### Consequences

Benefits:

```text
- Nhấn mạnh hệ thống hỗ trợ QC, không thay thế QC
- Không lock product identity vào promptfoo
- Sau này thay/extend engine vẫn hợp lý
```

Trade-off:

```text
- Cần giải thích rõ promptfoo vẫn là engine bên dưới
```

---

## ADR-002 — Use Promptfoo as Evaluation Engine

### Status

Accepted

### Context

Team đã PoC promptfoo bằng `npx`. Promptfoo có eval, providers, assertions, LLM-as-Judge, report/output.

### Decision

Use promptfoo as evaluation engine in MVP.

### Consequences

Benefits:

```text
- Nhanh hơn build engine từ đầu
- Có sẵn ecosystem
- Phù hợp PoC đã làm
```

Trade-offs:

```text
- Cần xử lý Node/promptfoo version
- Cần isolate promptfoo runner để không làm crash backend API
```

---

## ADR-003 — Build Platform Around Promptfoo Instead of Deep Fork First

### Status

Accepted

### Context

QC cần workflow:

```text
login
project
dataset
rubric
run dashboard
review
export
```

Các phần này nằm ngoài core UI/CLI của promptfoo.

### Decision

Build VSF QC Copilot as a wrapper/platform around promptfoo first. Không deep customize promptfoo source trong Week 3–4 trừ khi bắt buộc.

### Consequences

Benefits:

```text
- Giảm risk trong 2 tuần
- Dễ demo end-to-end
- Dễ upgrade promptfoo sau này
```

Trade-offs:

```text
- Một số UI/dashboard phải tự build
```

---

## ADR-004 — Use Dynamic API Connector Instead of Fixed Chatbot API Schema

### Status

Accepted

### Context

Mentor feedback:

```text
API phải config dynamic, sau này chỉ cần truyền/paste cURL là có thể sử dụng,
không cố định cho một chatbot nào cả.
```

Nếu hard-code request/response theo một chatbot cụ thể, platform sẽ rất khó reuse khi đổi chatbot hoặc API khác schema.

### Decision

Replace fixed `chatbot_api_config` concept with **Dynamic API Connector**.

Connector supports:

```text
raw_curl
method
url
headers
query_params
body_template
response_selector
is_streaming
streaming_type
timeout/retry
```

MVP supports manual config first. Paste cURL parser is P1 if time allows.

### Consequences

Benefits:

```text
- Không phụ thuộc một chatbot cụ thể
- Mentor/dev chỉ cần đưa cURL là có thể cấu hình
- Dễ dùng với mock API, internal API hoặc public API
```

Trade-offs:

```text
- UI/config phức tạp hơn
- Cần xử lý secret masking
- cURL parser có thể không cover mọi case ở MVP
```

---

## ADR-005 — Include Mock/Demo Chatbot API in MVP

### Status

Accepted

### Context

Hiện tại công ty đang cắt giảm nhiều chatbot nội bộ, nên có khả năng chưa có API nội bộ để test ngay. Nếu chờ API thật thì Week 3–4 có thể bị block.

### Decision

Include a mock/demo chatbot API in MVP.

Mock API is used as default target connector for demo if no internal API is available.

### Consequences

Benefits:

```text
- Không bị block bởi external/internal dependency
- Demo được full flow
- Có thể chủ động tạo PASS/FAIL/WARNING cases
```

Trade-offs:

```text
- Chưa chứng minh chất lượng với chatbot thật
- Cần nói rõ đây là fallback/demo target
```

---

## ADR-006 — Use Gemini and OpenAI as Preferred LLM Providers

### Status

Accepted

### Context

Mentor feedback: công ty đang có Gemini và OpenAI key, LLM nào cũng được nhưng suggest dùng hai provider này.

### Decision

Use Gemini/OpenAI for:

```text
dataset generation
LLM-as-Judge
```

Target chatbot vẫn đi qua Dynamic API Connector.

### Consequences

Benefits:

```text
- Dùng key công ty đang có
- Dễ triển khai dataset generation/judge
```

Trade-offs:

```text
- Cần tránh gửi dữ liệu nhạy cảm nếu chưa được approve
- Cần tách target model và judge model nếu có thể để giảm bias
```

---

## ADR-007 — Use Simple Username/Password Login for MVP

### Status

Accepted

### Context

Mentor confirm simple login username/password là đủ.

### Decision

MVP uses simple username/password authentication.

### Consequences

Benefits:

```text
- Nhanh triển khai
- Không bị block bởi SSO/company identity
```

Trade-offs:

```text
- Chưa có enterprise-grade auth
- Project-level permission có thể để later
```

---

## ADR-008 — Limit Demo Run Size to Under 100 Test Cases

### Status

Accepted

### Context

Mentor nói run size `<100` là được.

### Decision

MVP demo target:

```text
30–80 cases
Hard/soft validation: <100 cases
```

### Consequences

Benefits:

```text
- Giảm áp lực performance
- Dễ demo ổn định
- Phù hợp Week 3–4 timeline
```

Trade-offs:

```text
- Chưa chứng minh scale 500+ cases
- Scale test để Week 5/6 hoặc roadmap
```

---

## ADR-009 — Export Excel with Flexible Field Mapping

### Status

Accepted

### Context

Mentor feedback: tạm thời theo format hiện tại, trường nào thiếu thì bỏ qua.

### Decision

Excel export should use flexible mapper:

```text
Available field → fill
Missing/unmapped field → blank/skip
Do not fail export because optional fields are missing
```

### Consequences

Benefits:

```text
- Không bị block vì chưa đủ 100% template
- Dễ tương thích với format QC hiện tại
```

Trade-offs:

```text
- Export có thể chưa đầy đủ toàn bộ cột
- Cần document rõ cột nào fill được/cột nào blank
```

---

## ADR-010 — Start with Non-streaming API, Keep Streaming Optional

### Status

Accepted

### Context

Streaming format hiện tại chưa có API thật để confirm.

### Decision

MVP starts with non-streaming JSON response. Streaming support is optional/P2 and implemented when actual API format is available.

### Consequences

Benefits:

```text
- Giảm complexity
- Không bị block vì streaming format chưa rõ
```

Trade-offs:

```text
- Nếu API thật chỉ hỗ trợ streaming thì cần bổ sung parser nhanh sau
```

---

## ADR-011 — Use Redis-backed Job Queue for Async Runs

### Status

Accepted

### Context

Evaluation run có thể gồm nhiều test cases, dù demo dưới 100 cases vẫn không nên chạy trực tiếp trong HTTP request.

### Decision

Use Redis-backed Job Queue for MVP.

### Consequences

Benefits:

```text
- Không timeout HTTP
- Dễ show progress
- Có thể retry/fail job rõ hơn
```

Trade-offs:

```text
- Thêm Redis service vào deployment
```

---

## ADR-012 — Use SSE if Ready, Polling as Fallback

### Status

Accepted

### Context

SSE phù hợp để stream progress một chiều. Tuy nhiên Week 3–4 ngắn, không nên để SSE block MVP.

### Decision

Preferred: SSE.

Fallback: polling job/run status every 2–5 seconds.

### Consequences

Benefits:

```text
- Vẫn demo được nếu SSE chưa xong
- Dễ nâng cấp sau
```

Trade-offs:

```text
- Polling không real-time bằng SSE
```

---

## ADR-013 — Keep Red-team as P2 for Week 3–4

### Status

Accepted

### Context

Promptfoo có red-team, nhưng normal evaluation flow đã nhiều việc: connector, dataset, rubric, runner, dashboard, export.

### Decision

Red-team remains in architecture/roadmap but Week 3–4 focuses on normal evaluation MVP.

### Consequences

Benefits:

```text
- Giảm scope creep
- Tập trung vào flow QC cần dùng ngay
```

Trade-offs:

```text
- Red-team chưa phải demo chính Week 4
```
