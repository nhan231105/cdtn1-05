# Minh chứng trình duyệt thật – cập nhật 12/11/2026

Ảnh và log trong thư mục `web-acceptance/` được cập nhật bằng lượt 12/11 qua React/Vite → Spring Boot → MySQL 8 test riêng cổng3307, schema mới V1–V6. Playwright 1/1 PASS (7,9 giây), browser/API checks PASS. Đã xem ảnh; không thấy token, mật khẩu hay số điện thoại đầy đủ. Dữ liệu fixture tổng hợp, tài khoản demo và nhãn #ID không phải dữ liệu vận hành Mekong Mobile. Bản minh chứng ngày 03/11 được lưu riêng tại [web-acceptance-2026-11-03](web-acceptance-2026-11-03/README.md). Chi tiết suite/NFR: [xác minh 12/11](../verification-2026-11-12.md).

| Ảnh | Nội dung kiểm chứng |
|---|---|
| [01-survey-mobile.png](01-survey-mobile.png) | Form token thật, viewport360px, mã phiếu/trung tâm/thiết bị/ngày từ API |
| [02-response-list.png](02-response-list.png) | Manager101, phản hồi E2E thực, các bộ lọc/phân trang, phone đã che |
| [03-technician-stats.png](03-technician-stats.png) | Bảng KTV từ dữ liệu MySQL; CSAT/tỷ lệ tính ở backend |
| [04-center-stats.png](04-center-stats.png) | Director, tổng hợp trung tâm/tháng, distribution/null không phản hồi |
| [05-empty-responses.png](05-empty-responses.png) | Chọn kỳ01/2000: backend/MySQL thật không có phản hồi, UI hiện trạng thái rỗng |
| [06-loading-responses.png](06-loading-responses.png) | Chủ động trì hoãn requestAPI thật2giây bằng Playwright interception; chỉ chứng minh loading UI, không là đo latency |
| [07-auth-error.png](07-auth-error.png) | JWTinvalid chủ động đặt trong sessionStorage bị backend trả401; UI báo lỗi/thử lại. Đã khôi phục phiên hợp lệ và kiểm bảng hồi phục |

Journey E2E còn xác nhận submit thành công → mở lại409, manager response-rate, Marketing tải XLSX thật, director không được vào danh sách phản hồi403. Ba ảnh trạng thái là kiểm có điều kiện chủ động và nguồn điều kiện được ghi ở bảng; không giả là lỗi ngẫu nhiên hoặc bằng chứng hiệu năng. Server/backend/MySQL tạm đã dừng khi hoàn tất; database gốc3306 giữ nguyên.
