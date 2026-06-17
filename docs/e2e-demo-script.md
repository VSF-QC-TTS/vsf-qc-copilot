# Kịch bản Demo End-to-End (E2E) - VSF QC Copilot

Tài liệu này hướng dẫn chi tiết các bước để bạn demo toàn bộ luồng hệ thống VSF QC Copilot cho Mentor, bắt đầu từ lúc khởi chạy hệ thống bằng Docker Compose cho đến khi ra được kết quả đánh giá (Evaluation) cuối cùng.

> [!TIP]
> **Chuẩn bị trước khi demo:**
> - Bật sẵn Docker Desktop.
> - Chuẩn bị sẵn API Key của Google Gemini (`gemini-2.5-flash`).
> - Chuẩn bị một Endpoint API (có thể dùng public mock API hoặc API nội bộ đơn giản) để test. Ví dụ: API trả về thông tin thời tiết hoặc API chatbot giả lập.

---

## Bước 1: Khởi chạy hệ thống

Mở terminal tại thư mục gốc của dự án và chạy lệnh:
```bash
docker compose up --build -d
```
> [!NOTE]
> - Chờ khoảng 1-2 phút để các service (Frontend, Backend, Database) khởi động hoàn tất.
> - Có thể dùng `docker compose logs -f` để theo dõi log.

## Bước 2: Truy cập và Đăng nhập
1. Mở trình duyệt, truy cập vào Frontend: `http://localhost:3000`
2. Đăng nhập vào hệ thống bằng tài khoản QC có quyền thao tác (ví dụ: `qc.demo@example.com` hoặc tài khoản mà bạn đã config default trong DB).
3. Sau khi đăng nhập, hệ thống sẽ đưa bạn về màn hình **Dashboard** (nơi có widget "Recent Evaluations" mà ta vừa làm).

## Bước 3: Tạo Project mới
1. Chuyển sang tab **Projects** trên thanh Navigation.
2. Nhấn nút **Create Project** (Tạo dự án).
3. Điền thông tin:
   - **Tên dự án:** `Demo Chatbot AI`
   - **Mô tả:** `Dự án demo luồng E2E cho mentor đánh giá tự động.`
4. Nhấn lưu và truy cập vào chi tiết Project vừa tạo.

## Bước 4: Tạo Dataset (Tạo bằng AI)
1. Trong màn chi tiết Project, chuyển sang tab **Datasets**.
2. Nhấn **Create Dataset** -> Chọn phương thức **Generate with AI** (Tạo bằng AI).
3. Hệ thống sẽ yêu cầu bạn nhập Prompt/Context để AI tự sinh Test Case. 
   - *Ví dụ nhập vào:* `"Sinh cho tôi 5 câu hỏi thường gặp của khách hàng khi hỏi về chính sách đổi trả hàng của cửa hàng điện thoại."`
4. Hệ thống sẽ tự động generate ra danh sách Test Case. Bạn hãy review lại danh sách này.
5. Nhấn **Approve** (Phê duyệt) để chốt Dataset này (Dataset phải ở trạng thái Approved thì mới được mang đi test).

## Bước 5: Tạo Tiêu chí đánh giá (Rubric)
1. Chuyển sang tab **Rubrics**.
2. Nhấn **Create Rubric**.
3. Khai báo các tiêu chí đánh giá (Criteria). *Ví dụ:*
   - **Tiêu chí 1:** *Độ chính xác* - "Câu trả lời có đúng với chính sách đổi trả là 30 ngày không?" (Thang điểm: Pass/Fail hoặc 1-10).
   - **Tiêu chí 2:** *Thái độ* - "Câu trả lời có lịch sự và thân thiện không?"
4. Nhấn **Publish** để chốt version của Rubric.

## Bước 6: Cấu hình Target API (Hệ thống cần test)
1. Chuyển sang tab **Target Connectors** (hoặc API Connectors).
2. Nhấn **Create Connector**.
3. Điền thông tin API mà bạn muốn test. *Ví dụ (nếu dùng mock API):*
   - **Tên:** `Mock Chatbot API`
   - **URL:** `https://jsonplaceholder.typicode.com/posts` (Hoặc URL API chatbot thực tế của bạn).
   - **Method:** `POST`
   - **Headers/Body:** Cấu hình mapping tham số sao cho input từ Test Case (câu hỏi) được truyền đúng vào Payload của API.

## Bước 7: Cấu hình AI Judge (Giám khảo AI)
1. Trong cấu hình dự án, tìm đến phần **AI Judge Settings** (Cấu hình Giám khảo).
2. Chọn Provider: **Google Gemini**.
3. Nhập Model: `gemini-2.5-flash`
4. Dán **API Key** bạn đã chuẩn bị vào ô cấu hình. Lời khuyên là thao tác copy/paste thật nhanh gọn ở bước này để demo tính năng.

## Bước 8: Khởi chạy Evaluation Run
1. Đi tới tab **Evaluation Runs** (hoặc có nút Run ngay tại Dashboard/Project).
2. Nhấn **New Evaluation Run** (Chạy đánh giá mới).
3. Chọn các thành phần đã tạo ở trên:
   - **Dataset:** `Dataset vừa generate`
   - **Rubric:** `Rubric vừa publish`
   - **Connector:** `Mock Chatbot API`
   - **Judge:** `Gemini 2.5 Flash`
4. Nhấn **Start Evaluation** (Chạy).

## Bước 9: Xem kết quả (Kết thúc luồng E2E)
1. Trở về màn hình **Dashboard** (thanh Nav top).
2. Bạn sẽ thấy bản ghi Run vừa khởi tạo xuất hiện trong widget **"Recent Evaluations"**. 
3. Trạng thái sẽ nhảy từ `PENDING` -> `RUNNING` -> `COMPLETED`. Bạn có thể nhấn vào để vào xem chi tiết:
   - Hệ thống hiển thị bao nhiêu case PASS, bao nhiêu case FAIL.
   - Click vào một Test Case cụ thể để xem chi tiết: *Câu hỏi (Input)* -> *Hệ thống mục tiêu trả lời gì (Output)* -> *AI Judge chấm điểm ra sao, lý do (Reasoning) là gì*.

> [!IMPORTANT]
> Đây là bước then chốt nhất để "wow" mentor. Hãy giải thích rõ ràng cách AI sinh ra lý do (reasoning) tự động để chấm điểm cho Target API, qua đó thể hiện giá trị cốt lõi của **VSF QC Copilot**.
