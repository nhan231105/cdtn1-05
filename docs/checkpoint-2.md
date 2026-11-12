# Checkpoint 2 – Nghiệm thu cuối và bàn giao artifacts

Ngày 03/11/2026; branch `feat/bt2-setup`. Giữ thay đổi trong workspace; chưa commit, push, merge hoặc tạo tag. Checkpoint0/1 giữ lịch sử baseline và các lượt chạy trước.

## Công việc hoàn tất

- Đủ UC01–09 và 9 endpoint theo API Contract, gồm MUST/SHOULD/COULD; UC04 là tác vụ nền. UC01 polling chỉ đọc phiếu DA_DONG, chống tạo/gửi trùng.
- UC02 SMS/ZALO giả lập, trạng thái/lỗi/phone, retry tối đa 3 lần giãn 5 phút; UC04 dùng cùng token, sau 72 giờ, một reminder thành công, lỗi retry qua metadata V5 riêng.
- UC03 kiểm DA_GUI/token/expiry/rate-limit/validation/sanitize, transaction response+status, rollback DB, bất biến và concurrency. Fallback `Thiết bị #ID` cho ticket L2 mới chưa có device record ở SurveyResponseRepository, có regression assertion trong final verify46.
- UC05–09 và auth: aggregate/filter/page/scope/mask/XLSX, JWT/BCrypt/bootstrap opt-in; SQL/HTTP tách khỏi services. Bootstrap không đè user đang có.
- React 3 màn wireframe, login/điều hướng vai trò, màn trung tâm/tỷ lệ, API client riêng và loading/empty/error/retry/success. Không số liệu hardcode hoặc service key trong trình duyệt.
- README/env/design/AI/architecture/submission, ma trận baseline/current; báo cáo Markdown, PDF mới 8 trang và 7 ảnh thực. Giữ PDF/DOCX gốc.

## Lệnh chạy và kết quả thực tế

| Kiểm tra | Kết quả cuối |
|---|---|
| `backend/mvnw.cmd verify` | Lượt cuối 07:35:37: 46 tests, 0 failures, 0 errors, 0 skipped; package SUCCESS; gồm regression device ID mới |
| `powershell -File scripts/test.ps1` tại root | 46 PASS trên MySQL tạm mới V1–V5, datasource CLI ép server/schema sở hữu, không chịu TEST_DB_* kế thừa |
| Cùng lệnh ở source copy sạch thư mục mới | 46 PASS trên MySQL8.0.46 riêng/seed fresh; shutdown thành công, cổng3307 trống. Cùng máy/có dependency cache, chưa kiểm máy khác hoặc Git clone tag |
| `npm ci`, `npm test`, `npm run build` tại frontend và bản sao | 17 PASS (client8/UI9), build PASS, audit0 tại thời điểm chạy |
| `npm run test:e2e` qua jar8081/MySQL3307 mới V1–V5 | 1 journey PASS, 6,5 giây: submit360px, duplicate409, manager stats/list/rate, Marketing Excel, director center/403 |
| Playwright 7 ảnh thực | 4 màn data fresh: 1 sent/1 response/CSAT5; empty kỳ01/2000 từ DB; loading API trì hoãn2giây chỉ kiểm UI; auth error401/JWTinvalid và khôi phục phiên |
| `node scripts/render-report.mjs --preview` | PDF1.4, 8 trang/7 ảnh, khoảng257KB; xem preview bìa/matrix/states/rubric, không tràn bảng; link Markdown tồn tại |
| V1/V2 SHA256 | Giữ nguyên V1: `e630fabf95df8fe5e5c487783c3b68a94e7e9f11771083da5a326a5ebc8a8766`; V2: `53ba98a0379cbce2e9f9a6f698435032ba9ecdb92518b565796c19f02e07b2ab` |
| Kết thúc runtime | Server của phiên làm việc đã dừng,8081/3307 trống; MySQL gốc3306 PID14368 giữ nguyên |

Không bỏ/skip test lỗi. Surefire12classes/46tests gồm Flow12 và ReportingIntegration6. Mock UI không bị trình bày thành DB integration; ảnh loading chủ động trì hoãn không là bằng chứng p95.

## Lỗi đã gặp và cách xử lý

- Lock query dùng sai trong read-only transaction: tách query lock riêng cho luồng ghi rồi chạy lại tests.
- Fixture phone vượt độ dài cột: sửa dữ liệu mô phỏng đúng tình huống kiểm, không bỏ test.
- Jar Windows bị JVM E2E giữ: dừng đúng JVM của phiên làm việc, chạy lại toàn verify46 và package SUCCESS.
- MySQL Windows monitor tạo child: xác định actual listener PID/datadir sở hữu và dùng mysqladmin shutdown. Script không dừng hoặc dùng server có trước ở3307.
- TEST_DB_* kế thừa có thể trỏ nơi khác: runner ép datasource CLI vào endpoint/schema tạm nó tạo; đã chạy lại46PASS fresh.
- UC03 trước cho CHO_GUI: sửa theo precondition DA_GUI của SRS và regression test; ảnh cuối dùng DB mới nhất quán.
- Tài liệu checkpoint bị mất dấu do PowerShell5.1 pipe Unicode vào Python: viết lại UTF8, kiểm trực tiếp file và xuất lại PDF. Không ảnh hưởng mã nghiệp vụ hoặc dữ liệu DB.

## Giới hạn và việc tiếp theo

- S1 NOT_IMPLEMENTED: full CRUD hai entity trái QT10/QT13/KS06 và L2/L4 chỉ đọc; cần GV chấp nhận tiêu chí thay thế. Không tự thay nghiệp vụ.
- C6 PARTIAL: lockfile/env/config đã có nhưng chưa commit thay đổi mới. Chưa push/merge/tag v0.2/nộp E-learning; chỉ thực hiện sau chủ repository duyệt.
- NFR01/02/03 p95/tải100/50/10.000 phản hồi, NFR05 đủ100requests→1/99, NFR06 SLO99%/500phiếu, NFR07 thử5người/30giây/3chạm chưa đủ bằng chứng. Hiện concurrency2 đã pass; rate-limit một instance có test, chưa phân tán/pentest.
- Chưa tìm thấy file HoanThien; requirements chưa có trong repo, học liệu tìm ngoài repo. Không khẳng định tuân chỉ dẫn của file thiếu.
- C1 đã kiểm source copy sạch cùng máy, chưa máy khác/cache trống/Git clone tag. Event/tên tham chiếu L2/L4 thật và SMS/Zalo thật ngoài phạm vi thiết kế.
- Chủ repository cần đọc/hiểu source và AI report, xác nhận S1 với GV, duyệt PDF/link/commit/tag trước nộp. Báo cáo8trang ghi giới hạn, không nhận đạt toàn bộ rubric.

## Danh sách file thay đổi thực tế

Từ `git status --short --untracked-files=all`, loại ba mục đã có trước: backend.zip, backend/AGENTS.md và báo cáo DOCX gốc. SurveyInvitation mở rộng trên sửa đổi người dùng đã có, giữ markSent/markSendFailed. Không kê target/dist/node_modules vì build files không đưa vào Git.

```text
.env.example
README.md
backend/pom.xml
backend/src/main/java/com/mekongmobile/smartcrm/BackendApplication.java
backend/src/main/java/com/mekongmobile/smartcrm/auth/AuthController.java
backend/src/main/java/com/mekongmobile/smartcrm/auth/AuthService.java
backend/src/main/java/com/mekongmobile/smartcrm/auth/DemoAccountBootstrap.java
backend/src/main/java/com/mekongmobile/smartcrm/auth/UserIdentity.java
backend/src/main/java/com/mekongmobile/smartcrm/auth/UserRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/reference/repository/TicketRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ApiException.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ApiExceptionHandler.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ReportFilter.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ReportingController.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ReportingRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ReportingService.java
backend/src/main/java/com/mekongmobile/smartcrm/reporting/ResponseWorkbook.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/controller/SurveyDeliveryController.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/controller/SurveyResponseController.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/entity/SurveyInvitation.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/exception/SurveyExceptionHandler.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/repository/SurveyInvitationRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/repository/SurveyJobRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/repository/SurveyResponseRepository.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/security/SurveyRateLimitFilter.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/service/SurveyDeliveryService.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/service/SurveyInvitationService.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/service/SurveyResponseService.java
backend/src/main/java/com/mekongmobile/smartcrm/survey/service/SurveyScheduler.java
backend/src/main/resources/application.properties
backend/src/main/resources/db/migration/V3__reporting_reference_and_users.sql
backend/src/main/resources/db/migration/V4__invitation_retry_time.sql
backend/src/main/resources/db/migration/V5__reminder_retry_metadata.sql
backend/src/test/java/com/mekongmobile/smartcrm/reporting/ReportFilterTest.java
backend/src/test/java/com/mekongmobile/smartcrm/reporting/ReportingIntegrationTest.java
backend/src/test/java/com/mekongmobile/smartcrm/reporting/ResponseWorkbookTest.java
backend/src/test/java/com/mekongmobile/smartcrm/survey/service/SurveyFlowIntegrationTest.java
backend/src/test/java/com/mekongmobile/smartcrm/survey/service/SurveyInvitationServiceTest.java
backend/src/test/resources/application.properties
docs/ai-disclosure.md
docs/architecture-thuc-te.md
docs/bt2-gap-analysis.md
docs/checkpoint-0.md
docs/checkpoint-1.md
docs/checkpoint-2.md
docs/design-changes.md
docs/evidence/01-survey-mobile.png
docs/evidence/02-response-list.png
docs/evidence/03-technician-stats.png
docs/evidence/04-center-stats.png
docs/evidence/05-empty-responses.png
docs/evidence/06-loading-responses.png
docs/evidence/07-auth-error.png
docs/evidence/README.md
docs/reports/BT2_L8_implementation_2026-11-03.pdf
docs/reports/bt2-implementation-report.md
docs/submission-guide.md
docs/thay-doi-thiet-ke.md
frontend/.env.example
frontend/.gitignore
frontend/README.md
frontend/e2e/l8.spec.js
frontend/e2e/prepare-fixture.ps1
frontend/index.html
frontend/package-lock.json
frontend/package.json
frontend/playwright.config.js
frontend/src/App.jsx
frontend/src/App.test.jsx
frontend/src/api.js
frontend/src/api.test.js
frontend/src/main.jsx
frontend/src/styles.css
frontend/src/test-setup.js
frontend/vite.config.js
scripts/render-report.mjs
scripts/start-test-db.ps1
scripts/test.ps1
```

