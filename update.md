# Phase 2 Progress Update

1. **Target Connectors (Phase 2):**
   - Đã hoàn thiện tính năng **Update** & **Delete** (cả UI và API).
   - Ở trang chi tiết Connector, người dùng có thể chọn Edit hoặc Delete.

2. **Judge Models (Phase 2):**
   - Đã thêm API `DELETE /api/v1/projects/{projectId}/judge-models/{id}` và `PATCH /api/v1/judge-models/{id}` (Update).
   - Đã thiết kế lại `JudgeModelDialog` trên giao diện để có thể tái sử dụng cho tính năng **Edit**.
   - Đã thêm nút Edit và Delete ở cuối mỗi hàng trên bảng Judge Models.

3. **Projects (Phase 2):**
   - Đã hoàn thiện tính năng **Update Project** (tên, description). Refactor lại `CreateProjectDialog` thành `ProjectDialog` dùng chung cho cả Create và Edit.
   - Thêm nút Edit trên giao diện chi tiết Project.

4. **Datasets (Phase 2):**
   - Đã thêm tính năng **Update Dataset** (tên, mô tả) và **Delete Dataset** (xóa cứng) trên cả giao diện danh sách Datasets và trang chi tiết Dataset.
   - Bổ sung Confirm Dialog trước khi Delete để tránh xóa nhầm.

**Tình trạng:** Phase 2 (CRUD Project Settings) coi như đã hoàn tất các hạng mục cốt lõi. Chúng ta có thể chuyển sang **Phase 3: Evaluation Wizard**!
