# Checkpoint 1 – Hiện thực L8 và kiểm thử tích hợp đầu tiên

Ngày 03/11/2026. Tiếp tục cùng branch `feat/bt2-setup`. Không commit/push/merge/tag; không truy cập reset database MySQL đang làm việc.

## Công việc và mã nguồn thực tế

| Nhóm file | Thay đổi và lý do |
|---|---|
| `survey/controller/SurveyDeliveryController.java`, `survey/service/SurveyDeliveryService.java` | EP2 SMS/ZALO, trạng thái gửi, số điện thoại, lỗi gateway; commit metadata GUI_LOI trước trả lỗi; idempotent gửi thành công; khóa invitation để chặn gửi đồng thời |
| `survey/entity/SurveyInvitation.java`, `survey/repository/SurveyInvitationRepository.java` | Giữ markSent/markSendFailed do người dùng đã thêm; bổ sung timestamp retry, responded/reminded và query khóa riêng |
| `survey/controller/SurveyResponseController.java`, `survey/service/SurveyResponseService.java`, `survey/security/SurveyRateLimitFilter.java` | EP3/4 token, validation score/comment, sanitize, transaction lưu response+invitation, chặn trùng/hết hạn và giới hạn 10 request/phút/IP |
| `survey/service/SurveyScheduler.java`, `SurveyInvitationService.java`, `BackendApplication.java` | Thăm dò phiếu đã đóng, tạo/gửi lời mời, retry và nhắc một lần; TTL cấu hình; bật scheduling |
| `auth/`, `reporting/` | Login BCrypt/JWT, role/scope, metadata; list/filter/page/mask, CSAT KTV/trung tâm, response-rate và XLSX |
| `db/migration/V3__reporting_reference_and_users.sql`, `V4__invitation_retry_time.sql` | Chỉ migration mới bổ sung dữ liệu tên tham chiếu/tài khoản/timestamp; giữ V1/V2 |
| `application.properties`, `pom.xml`, `src/test/resources/`, `scripts/test.ps1`, `scripts/start-test-db.ps1` | Cấu hình env, dependency JWT/BCrypt, fixture MySQL test riêng; không đổi Boot/Maven/Java/MySQL project |
| `src/test/.../SurveyFlowIntegrationTest.java`, `reporting/*Test.java`, tests UC01 hiện có | Chứng minh TX/rollback/duplicate/send/retry/reminder/auth/query/mask/Excel; giữ các tests baseline |
| `frontend/` | React API client riêng, 3 màn wireframe, login/role nav, màn center/rate, download; tests/build/E2E harness và lockfile |
| `README.md`, `.env.example`, `docs/` | Chạy lại, thiết kế/AI/kiến trúc/ma trận/báo cáo/hướng dẫn nộp; không khai báo kết quả chưa chạy |

Các đường dẫn Java trong bảng tương đối với `backend/src/main/java/com/mekongmobile/smartcrm/`. Danh sách thay đổi chính xác khi nghiệm thu lấy từ `git status --short`/`git diff`, bao gồm các file chưa tracked.

## Lệnh và kết quả đã chạy

| Lệnh/điều kiện | Kết quả checkpoint |
|---|---|
| Backend unit baseline rút gọn | 20 tests PASS; chỉ làm mốc, không thay nguyên suite |
| `powershell -File scripts/test.ps1` / `backend/mvnw.cmd test`, MySQL8.0.46 mới riêng cổng3307 | 42 tests, 0 failures, 0 errors, 0 skipped; xác nhận từ Surefire XML 12 test classes |
| Flyway trên MySQL test mới | V1–V4 áp dụng thành công; khởi tạo context lần sau schema up-to-date |
| `SurveyFlowIntegrationTest` | 9 tests PASS: gửi idempotent/ZALO; phone sai commit GUI_LOI; gateway retry 3 lần cách5 phút; response bất biến/sanitize; invalid/expiry/token; 2 concurrent →1 response; rollback qua trigger; nhắc một lần; HTTP401/429 |
| `ReportingIntegrationTest`, `ReportFilterTest`, `ResponseWorkbookTest` | MySQL aggregate/null, inclusive dates/filter/page/mask/scope, CSAT distribution/rate, JWT tamper/expiry/disabled, XLSX/filter validation pass trong suite42 |
| `cd frontend; npm test` | 17 tests PASS (mock HTTP để kiểm hành vi giao diện) |
| `cd frontend; npm run build` | PASS; build files không được đưa vào Git |
| `npm audit` tại frontend | 0 vulnerabilities ở thời điểm checkpoint, không thay kiểm bảo mật chức năng |
| Đối chiếu links Markdown và .gitignore | Các link local mới tồn tại; `.env` ignored, `.env.example` không ignored |

Hai lỗi thực thi ban đầu được sửa trước khi chạy lại: query khóa bị dùng trong read-only transaction và fixture phone vượt độ dài cột. Không xóa/skip test. Các kết quả trên là lần chạy checkpoint; sau tách SQL/repository hoặc thay đổi nghiệp vụ phải chạy lại suite phù hợp.

## Chưa nghiệm thu và checkpoint tiếp theo

- E2E trình duyệt với backend/MySQL thật đang chờ chạy, Chromium/harness đã chuẩn bị; mock frontend không tính là E2E database.
- Đang rà soát precondition UC03 lời mời phải đã gửi và ranh giới SQL/HTTP ở services; chưa dùng suite cũ để kết luận code chỉnh sau đó pass.
- Chưa đo NFR tải100 requests/p95/10.000 phản hồi, SLO gửi5 phút hoặc usability5người; chỉ concurrency2 trong suite hiện tại.
- Chưa kiểm scheduler tick tự động end-to-end hoặc nhắc đúng biên72h/expiry bằng thời gian thực; service reminder một lần đã có test.
- S1 full CRUD chờ giảng viên chấp nhận điều chỉnh cách chấm; không thêm sửa/xóa phản hồi trái BT1.
- Tiếp theo: rerun suite sau sửa cuối; chạy E2E thật; đối chiếu checksum V1/V2; cập nhật ma trận current theo bằng chứng; chuẩn bị ảnh/PDF và phép thử thư mục mới.
