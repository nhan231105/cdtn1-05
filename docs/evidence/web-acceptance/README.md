# Evidence nghiệm thu website 12/11/2026

Lượt hiện tại: MySQL 8.0.46 test mới, Flyway V1–V6, E2E 1/1 PASS (5,8 giây), browser/API checks PASS. Backend 54/54; frontend 18/18 và production build PASS. NFR result/limits ở [verification report](../../verification-2026-11-12.md). Bản 03/11 lưu tại `../web-acceptance-2026-11-03/`.

Đọc [kết quả](../../web-acceptance-results.md), [test cases](../../web-acceptance-test-cases.md), [run guide](../../web-run-guide.md).

- backend-tests.txt + com.*.txt: suite backend 54/54 từ 12/11 và báo cáo class; Maven XML gốc nằm trong target/surefire-reports (bị ignore).
- npm-ci.txt, frontend-tests.txt, frontend-build.txt: clean clone cài lockfile/test/build frontend.
- e2e.log: suite Playwright 1/1 PASS, API/MySQL thật; e2e-attempt1.log là lượt runner dừng bởi warning, không phải PASS.
- backend-startup.log / frontend-startup.log: startup8080/5173; log bị Git ignore, hiện vẫn tồn tại trên máy, không tự add/commit.
- browser-checks.json / browser-run.log: asserts bổ sung, HTTPstatus/latency; surveytoken được thay [redacted].
- mysql-before.txt / mysql-after.txt / score-check.txt / foreign-key.txt:0→6responses,8invitations, migration/constraint/FK.
- api-response-list.json / responses.xlsx / xlsx-inspection.txt: snapshot2responses điểm5 lúc tải, trước khi thêm1–4.
- survey-*.png, survey-success.png: form và success thật.
- manager101-*.png / marketing-*.png / director-*.png: route/role/viewport thật;4ảnh01–04 từ E2E repo360/1440px.
- ui-loading-delayed-real-api.png: API thật bị delay1.8s.
- ui-empty-mocked.png, ui-error-mocked-503.png: HTTPmock được điều khiển bởi Playwright; không chứng minh MySQL trống/hỏng.
- verify.sql: truy vấn chỉ đọc tái thực hiện bằng Workbench kết nối test3307.
- test-environment.json: vị trí datadir của lượt browser, server đã dừng.

run-checks.ps1 + browser-check.mjs là harness nghiệm thu, không đổi source sản phẩm. Root repository → powershell -NoProfile -ExecutionPolicy Bypass -File docs/evidence/web-acceptance/run-checks.ps1. Phải rảnh3307/8080/5173 và đã npmci/cài Chromium. Mỗi lượt DB mới, tự sinh secretprocess, giữ datadir; chờ60s giữa nhóm publicrequests để tôn trọng limiter; finally dừng tiến trình sở hữu. Harness ghi đè evidence cùng tên; lưu bản sao nếu cần giữ lượt trước.

Không công bố trace hoặc sessionStorage/JWT/key/.env. Không có credential mặc định để login lại lượt tự động; hãy dựng demo riêng theo run-guide và tự đặt password bootstrap.

