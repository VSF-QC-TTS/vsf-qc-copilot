# Bộ tài liệu dự án QC Chatbot Evaluation Platform

Bộ tài liệu này được tạo để hỗ trợ thảo luận với mentor, QC/tester và developer trước khi triển khai MVP.

## Danh sách tài liệu

| File | Nội dung |
|---|---|
| `01_product_requirement.md` | Yêu cầu sản phẩm, phạm vi MVP, user stories, success metrics, rủi ro. |
| `02_technical_design.md` | Thiết kế kỹ thuật, module, Promptfoo integration, response mapping, API, security. |
| `03_data_model.md` | Entity, ERD, bảng dữ liệu, mapping từ Excel sang database. |
| `04_architecture.md` | Kiến trúc hệ thống, deployment, runtime flow, security boundary, scaling path. |
| `05_mvp_roadmap.md` | Kế hoạch 1 tuần, milestone, definition of done, backlog sau MVP. |

## Ghi chú trọng tâm

Các tài liệu này đi theo hướng:

```text
Spring Boot platform quản lý workflow QC
+ React UI tiếng Việt
+ PostgreSQL lưu dataset/result/review
+ Redis điều phối job
+ Promptfoo làm eval/red-team engine phía sau
```

Không đề xuất fork sâu UI Promptfoo cho MVP. Nền tảng nên tự build UI theo nghiệp vụ QC, còn Promptfoo dùng làm engine chạy evaluation.
