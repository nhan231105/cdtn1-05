# Xác minh triển khai BT2 – 12/11/2026

Branch kiểm tra: `feat/bt2-setup`, commit `b5199d5`. Từ local Git bundle, mình tạo checkout mới không có `.env`/build artifacts rồi chạy `npm ci`, test, build, backend suite và E2E. Môi trường dùng Java 21.0.12.1, Spring Boot 4.1.2, Node 24.19.0, npm 11.17.0 và MySQL 8.0.46 tạm trên `127.0.0.1:3307`. Schema `smartcrm_l8_test` được tạo mới; Flyway áp dụng V1–V6 thành công. MySQL đang dùng ở cổng 3306 không bị truy cập hoặc thay đổi.

## Kết quả kiểm thử

| Lệnh | Kết quả | Bằng chứng |
|---|---|---|
| `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1` | 54 tests PASS, 0 failure/error/skip; BUILD SUCCESS. Kết thúc lúc 23:29:30 ICT trong checkout mới. | Maven Surefire reports; log chạy trong [`evidence/web-acceptance/backend-tests.txt`](evidence/web-acceptance/backend-tests.txt) |
| `npm ci` trong `frontend` | Cài từ lockfile; 0 vulnerabilities được báo cáo. | [`npm-ci.txt`](evidence/web-acceptance/npm-ci.txt) |
| `npm test` trong `frontend` | 18 tests PASS trên 2 files. | [`frontend-tests.txt`](evidence/web-acceptance/frontend-tests.txt) |
| `npm run build` trong `frontend` | Vite production build PASS. | [`frontend-build.txt`](evidence/web-acceptance/frontend-build.txt) |
| `docs/evidence/web-acceptance/run-checks.ps1` | Playwright 1/1 PASS trong 5,8 giây; browser/API checks PASS; schema và ràng buộc DB V1–V6 được kiểm tra. | [evidence hiện hành](evidence/web-acceptance/README.md) |

Maven suite chạy trên database tạm độc lập, giữ lại thư mục datadir test để xem xét và đã shutdown server. Cổng 3307, 8080 và 5173 rảnh sau lượt E2E. Các fixture là dữ liệu tổng hợp.

## NFR đã đo

| NFR | Phép đo mới | Kết quả và giới hạn |
|---|---|---|
| NFR01 | 100 lời gọi GET form đồng thời, 100 IP tổng hợp, MockMvc | p95 128 ms, max 180 ms. Đạt ngưỡng cục bộ 2.000 ms của test; không mô phỏng TCP/4G. |
| NFR02 | 50 lời gọi POST phản hồi đồng thời cho 50 token khác nhau, MockMvc | p95 381 ms, max 383 ms; 50 phản hồi lưu thành công. Đạt ngưỡng cục bộ 500 ms của test; không mô phỏng TCP/4G. |
| NFR03 | 10.000 phản hồi MySQL; 20 HTTP samples/endpoint, backend random port | KTV p95 80/max 222 ms; trung tâm 41/52 ms; danh sách 83/86 ms. Local loopback/MySQL test, không phải máy production. |
| NFR05 | 100 POST cùng token qua controller/filter, 100 IP test | 1×201, 99×409; DB có đúng một phản hồi. Đơn instance, chưa thử phân tán. |
| NFR06 | Scheduler và gateway giả lập, 500 phiếu | 500/500 gửi trong 5 phút, 6 tick, 6.192 ms. Không gọi SMS/Zalo thật. |

NFR04 có test rate limit trong một instance, nhưng chưa kiểm nhiều instance/proxy hoặc làm pentest. NFR07 chưa tổ chức usability test với 5 người thật; có [kế hoạch và biểu mẫu](usability-test-plan.md), không tạo kết quả giả. NFR08 được chứng minh ở mức luồng E2E xem dữ liệu ngay sau khi gửi, không suy rộng sang tải cao.

## Thay đổi và điểm còn chờ

- V1/V2 không sửa. V6 thêm mẫu khảo sát, snapshot nội dung trên lời mời và dấu hủy chống phát hành lại token cho ticket đã xóa lời mời chưa gửi.
- Lời mời có API đọc danh sách/chi tiết, đổi kênh trước lần gửi đầu và xóa vật lý chỉ khi chưa gửi/chưa có phản hồi. Mẫu khảo sát có CRUD cho Marketing; response vẫn bất biến; bảng L2/L4 chỉ đọc.
- S1 có mã và test cho hai phạm vi L8, nhưng cách chấm CRUD có điều kiện vẫn **chờ xác nhận học thuật**.
- SMS/Zalo là gateway giả lập. Chưa nối event đóng phiếu và nhãn tham chiếu thật từ L2/L4.
- Không tìm thấy `Codex_SmartCRM_L8_BT2_HoanThien.md` trong repository, Downloads hoặc cây `D:\cdtn1` trong lượt khảo sát.
- Commit `5a9c798` và merge an toàn commit remote PDF tại `b5199d5` đã được kiểm thử trong clone sạch. Trạng thái push/merge main/tag được ghi ở [checkpoint-3](checkpoint-3.md).
