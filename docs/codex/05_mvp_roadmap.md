# MVP Roadmap

## Dự án: Nền tảng nội bộ QC chatbot AI

### Phiên bản tài liệu

| Thuộc tính | Nội dung |
|---|---|
| Tên tài liệu | MVP Roadmap |
| Timeline mục tiêu | 1 tuần |
| Mục tiêu chính | Chạy được luồng end-to-end từ dataset đến dashboard/review/export |
| Tech stack | Spring Boot 4.1.0, Java 21, PostgreSQL, Redis, React, Vite, Tailwind CSS v4, Promptfoo Runner |

---

## 1. Định nghĩa MVP

MVP không phải là platform hoàn chỉnh. MVP là bản chứng minh được luồng nghiệp vụ cốt lõi:

```text
QC import dataset
→ cấu hình chatbot target
→ cấu hình judge model
→ nhập rubric
→ chạy evaluation
→ xem kết quả
→ review/chốt status
→ export report
```

MVP cần ưu tiên end-to-end hơn giao diện đẹp hoặc tính năng nâng cao.

---

## 2. Scope bắt buộc

| Hạng mục | Kết quả cần đạt |
|---|---|
| Auth local | User đăng ký/đăng nhập được. |
| Project | Tạo và xem project được. |
| Chatbot target config | Cấu hình URL/method/header/body/response mapping. |
| Sample response mapping | Paste sample response và preview field extracted. |
| Judge model config | Nhập provider/model/API key hoặc secret ref. |
| Rubric editor | Nhập prompt/rubric và lưu version. |
| Dataset import | Import Excel hiện tại, parse testcase và `_PRECONDITIONS`. |
| Evaluation runner | Generate config, chạy Promptfoo, lấy result. |
| Result table | Xem danh sách testcase sau khi chạy. |
| Review detail | Expected vs actual, scores/reason, final status/comment/PIC. |
| Dashboard | Total, Passed, Failed, Pending, pass rate, failed by section. |
| Export | Export CSV hoặc XLSX kết quả. |

---

## 3. Scope nên cắt khỏi MVP

| Hạng mục | Lý do cắt |
|---|---|
| SSO Google/GitHub hoàn chỉnh | Local auth đủ để demo end-to-end. Có thể thêm sau. |
| Red team UI | Promptfoo red team quan trọng nhưng không phải luồng Excel hiện tại. |
| Auto-generate testcases từ business requirement | Phức tạp, cần input từ QC và prompt riêng. |
| Assignment workload | Chưa đủ dữ liệu thực tế về cách phân công QC. |
| Multi-turn conversation đầy đủ | MVP có thể xử lý mỗi testcase là một session, hỗ trợ `$RESET` trước. |
| RBAC chi tiết | MVP chỉ cần role đơn giản. |
| CI/CD production | MVP deploy bằng Docker Compose là đủ. |
| Alerting/Grafana | Prometheus metrics trước, dashboard vận hành sau. |

---

## 4. Kế hoạch 7 ngày

### Ngày 1: Foundation

#### Backend

- Init Spring Boot project.
- Cấu hình PostgreSQL, Redis, Flyway/Liquibase.
- Tạo schema ban đầu: users, refresh_tokens, projects.
- Implement register/login/refresh/logout.
- Implement project CRUD tối thiểu.

#### Frontend

- Init React + Vite + Tailwind CSS v4.
- Setup routing, auth layout, protected routes.
- Trang login/register.
- Trang project list/detail cơ bản.

#### Deliverable cuối ngày

```text
User đăng ký/đăng nhập được và tạo project đầu tiên được.
```

---

### Ngày 2: Dataset import

#### Backend

- Implement upload Excel.
- Parse sheet list.
- Normalize headers.
- Parse `_PRECONDITIONS`.
- Parse test cases từ các sheet dataset.
- Lưu datasets, preconditions, test_cases.
- Trả import summary và warning.

#### Frontend

- Trang upload dataset.
- Preview import summary.
- Hiển thị một số dòng testcase đầu tiên.
- Hiển thị validation errors/warnings.

#### Deliverable cuối ngày

```text
Import được file Excel hiện tại và xem preview test cases trên UI.
```

---

### Ngày 3: Target, Judge, Rubric

#### Backend

- CRUD chatbot target.
- Implement response mapping JSONPath.
- API validate mapping bằng sample response.
- CRUD judge model.
- CRUD rubric và rubric version.

#### Frontend

- Form chatbot target config.
- Form sample response mapping preview.
- Form judge model config.
- Rubric editor.

#### Deliverable cuối ngày

```text
Cấu hình được chatbot target, preview được actual response extraction, lưu được judge model và rubric.
```

---

### Ngày 4: Runner và Promptfoo integration

#### Backend

- API tạo evaluation run.
- Tạo run status `QUEUED`.
- Enqueue job vào Redis hoặc DB queue.

#### Runner

- Worker nhận `run_id`.
- Load dataset/target/judge/rubric từ DB.
- Generate promptfooconfig.yaml và tests file.
- Chạy Promptfoo CLI với mock/small dataset.
- Capture stdout/stderr.

#### Deliverable cuối ngày

```text
Bấm Run Evaluation và runner thực sự chạy được một evaluation nhỏ.
```

---

### Ngày 5: Result ingestion và Result UI

#### Backend

- Parse results JSON/JSONL.
- Lưu evaluation_run_items.
- Lưu evaluation_scores.
- Lưu raw result JSON.
- Update run counters.
- API list run items, filter status/section.

#### Frontend

- Trang run detail.
- Result table.
- Filter Passed/Failed/Pending.
- Detail view một testcase.

#### Deliverable cuối ngày

```text
Sau khi chạy xong, UI hiển thị được kết quả từng testcase và filter được failed cases.
```

---

### Ngày 6: Dashboard, manual review, export

#### Backend

- API run summary/dashboard.
- Manual review API.
- Update effective_status sau khi QC review.
- Export CSV hoặc XLSX.

#### Frontend

- Dashboard cards: total, passed, failed, pending, pass rate.
- Failed by section table/chart đơn giản.
- Review form: final status, comment, PIC.
- Export button.

#### Deliverable cuối ngày

```text
QC review được từng testcase, dashboard cập nhật, export được report.
```

---

### Ngày 7: Hardening và demo

#### Backend/Runner

- Mask secret trong log.
- Error handling cho chatbot timeout, judge timeout, parser error.
- Retry cơ bản nếu phù hợp.
- Health check và Prometheus metrics.
- Docker Compose deploy trên server.

#### Frontend

- Polish UX tối thiểu.
- Loading/error states.
- Empty states.
- Kiểm tra flow end-to-end bằng dataset thật.

#### Deliverable cuối ngày

```text
Demo end-to-end: import dataset → run eval → dashboard → manual review → export report.
```

---

## 5. Milestone checklist

### Milestone 1: Basic platform

- [ ] User đăng ký/đăng nhập được.
- [ ] Project tạo được.
- [ ] Frontend gọi API có auth được.

### Milestone 2: Dataset

- [ ] Upload Excel được.
- [ ] Parse sheet dataset được.
- [ ] Parse `_PRECONDITIONS` được.
- [ ] Preview test cases được.
- [ ] Validation warning rõ ràng.

### Milestone 3: Config

- [ ] Chatbot target lưu được.
- [ ] Sample response mapping preview được.
- [ ] Judge model lưu được.
- [ ] Rubric version lưu được.

### Milestone 4: Evaluation

- [ ] Tạo run được.
- [ ] Runner nhận job được.
- [ ] Promptfoo chạy được.
- [ ] Result được parse vào DB.

### Milestone 5: Review/report

- [ ] Result table hiển thị được.
- [ ] Detail expected vs actual hiển thị được.
- [ ] QC final status lưu được.
- [ ] Dashboard tổng quan có số đúng.
- [ ] Export CSV/XLSX được.

---

## 6. Definition of Done cho MVP

MVP được coi là đạt khi hoàn thành các điều kiện sau:

1. Import được một file Excel thực tế của QC.
2. Cấu hình được ít nhất một chatbot target bằng API thật hoặc API mock có cấu trúc tương tự thật.
3. Cấu hình được một judge LLM và một rubric version.
4. Chạy được ít nhất một evaluation run từ UI.
5. Kết quả từng testcase được lưu vào PostgreSQL.
6. QC xem được expected, actual, judge reason, scores.
7. QC chốt được Passed/Failed/Pending thủ công.
8. Dashboard hiển thị đúng tổng quan run.
9. Export được report.
10. Secret không xuất hiện trong log thông thường.

---

## 7. Kế hoạch demo

### Demo script đề xuất

1. Đăng nhập platform.
2. Tạo project `AI HEALTH - ScanAI Round 2`.
3. Import file Excel.
4. Xem import summary và preview testcase.
5. Tạo chatbot target:
   - Nhập endpoint.
   - Paste sample response.
   - Map `answer_path`, `trace_id_path`, `agent_steps_path`.
6. Tạo judge model.
7. Tạo rubric version.
8. Bấm Run Evaluation.
9. Mở dashboard run.
10. Filter Failed cases.
11. Mở một testcase failed.
12. So sánh expected vs actual.
13. QC override status/comment/PIC.
14. Export report.

### Dữ liệu demo tối thiểu

| Thành phần | Số lượng |
|---|---:|
| Project | 1 |
| Dataset | 1 file Excel |
| Testcase chạy demo | 10 đến 30 dòng nếu API tốn chi phí/thời gian |
| Chatbot target | 1 |
| Judge model | 1 |
| Rubric | 1 version |

---

## 8. Backlog sau MVP

### Phase 2: Nâng cấp QC workflow

| Feature | Mục tiêu |
|---|---|
| SSO Google/GitHub | Đăng nhập theo tài khoản công ty. |
| Role/permission chi tiết | Admin, QC lead, QC, viewer. |
| Assignment | Phân công failed cases cho QC/PIC. |
| Review history UI | Xem lịch sử thay đổi final status. |
| Batch review | Chốt nhiều case cùng lúc. |
| Comment threads | Trao đổi giữa QC và dev/chatbot owner. |

### Phase 3: Dataset generation

| Feature | Mục tiêu |
|---|---|
| Business requirement import | Upload BRD/spec để sinh testcase. |
| AI-assisted testcase generation | LLM sinh testcases/ground truth. |
| Human approval | QC duyệt testcase trước khi đưa vào dataset. |
| Dataset versioning | Theo dõi dataset thay đổi theo version. |

### Phase 4: Promptfoo red team

| Feature | Mục tiêu |
|---|---|
| Red team run type | Chạy red team theo target. |
| Vulnerability dashboard | Nhóm lỗi security/safety. |
| Policy/risk scoring | Chuẩn hóa mức độ rủi ro. |
| Regression suite | Chạy lại bộ test sau mỗi release chatbot. |

### Phase 5: Production hardening

| Feature | Mục tiêu |
|---|---|
| Multi-runner scale | Nhiều worker chạy song song. |
| Secret manager | Quản lý secret chuẩn production. |
| Object storage | Lưu artifacts/logs lớn. |
| Grafana alerts | Alert khi run fail hoặc latency cao. |
| CI/CD | Deploy tự động. |

---

## 9. Rủi ro theo timeline 1 tuần

| Rủi ro | Khả năng | Ảnh hưởng | Giảm thiểu |
|---|---:|---:|---|
| Chưa có API chatbot thật | Cao | Cao | Dùng mock server trước, chuẩn bị sample response thật càng sớm càng tốt. |
| Rubric chưa ổn định | Cao | Trung bình | Cho QC nhập prompt, bắt buộc JSON output, lưu version. |
| Promptfoo integration mất thời gian | Trung bình | Cao | Chạy POC nhỏ từ ngày 4, lưu raw output để debug. |
| Import Excel có nhiều ngoại lệ | Cao | Trung bình | Normalize header, import warning, bỏ qua sheet không cần. |
| UI scope quá lớn | Trung bình | Cao | Chỉ làm form và bảng cần thiết, chưa tối ưu UX nâng cao. |
| Secret/log chưa an toàn | Trung bình | Cao | Mask log từ đầu, không ghi API key vào config nếu tránh được. |

---

## 10. Ưu tiên triển khai nếu thiếu thời gian

Nếu thời gian không đủ, thứ tự ưu tiên nên là:

1. Import dataset.
2. Chatbot target response mapping.
3. Judge model + rubric.
4. Runner chạy được.
5. Result table.
6. Manual review.
7. Dashboard đơn giản.
8. Export.
9. Auth nâng cao/SSO.
10. UI polish.

Mục tiêu quan trọng nhất là end-to-end flow. Một giao diện chưa đẹp nhưng chạy đúng luồng sẽ có giá trị hơn một UI tốt nhưng chưa chạy được evaluation thật.
