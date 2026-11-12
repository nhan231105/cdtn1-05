# Kịch bản nghiệm thu website Smart CRM L8

Ngày 03/11/2026. Nguồn: [SRS](../srs.md) §3.2–3.5 và [API Contract](../api-contract.md); đối chiếu controller/service/repository, App.jsx và tests hiện tại. SRS liệt kê UC01–UC09, chỉ UC03 có đặc tả từng bước đầy đủ; không bịa các đặc tả chi tiết còn thiếu. [Hướng dẫn chạy](web-run-guide.md) cung cấp lệnh khởi động và chuẩn bị fixture.

PASS dưới đây chỉ cho phần tự động đã ghi trong “Thực tế”, không chứng nhận các bước thủ công bổ sung đã được người dùng thao tác. BLOCKED là chưa đủ điều kiện/minh chứng, không tự đổi expected status. Evidence tương đối thuộc [web-acceptance](evidence/web-acceptance/). Các tên class TXT là báo cáo Surefire cùng thư mục, có prefix package đầy đủ.

## Ánh xạ use case

| UC | Chức năng | Màn hình | Endpoint/tác vụ | Test data | Kỳ vọng |
|---|---|---|---|---|---|
| UC01 | Tạo lời mời phiếu đóng | Không có UI, L2/L4/scheduler | POST /api/survey-invitations | 401 đóng/404 mở/ID thiếu | 201/422/404; trùng409 |
| UC02 | Gửi lời mời | Không có UI gửi nội bộ | POST /api/survey-invitations/{id}/send | SMS/ZALO giả lập | DA_GUI hoặc GUI_LOI, retry hữu hạn |
| UC03 | Khách trả lời | /khao-sat/{token} | GET /api/surveys/{token}; POST .../responses | Token mới, score1–5 | 201, một phản hồi bất biến |
| UC04 | Nhắc72h | Không có màn/endpoint public | SurveyScheduler/DeliveryService | sent_at73h trước, còn hạn | Một nhắc thành công, cùng link |
| UC05 | Tra cứu | /phan-hoi | GET /api/survey-responses | Manager101/Marketing, filter | Scope, page, phone che |
| UC06 | CSAT kỹ thuật viên | /thong-ke/ky-thuat-vien | GET /api/survey-stats/by-technician | Manager101, đúng kỳ | AVG/rate/null đúng |
| UC07 | CSAT trung tâm | /thong-ke/trung-tam | GET /api/survey-stats/by-center | Director, month | Phân bố/tổng hợp/xu hướng |
| UC08 | Excel | Nút Xuất Excel trong /phan-hoi | GET /api/survey-responses/export | Marketing, filter | XLSX7cột, cùng bộ lọc |
| UC09 | Tỷ lệ phản hồi | /thong-ke/ty-le | GET /api/survey-stats/response-rate | Manager101, cohort tháng gửi | responses/sent, null nếu0 |

## Quy tắc thực thi

Dùng MySQL3307/smartcrm_l8_test mới, bootstrap true, scheduler false cho thao tác API có thứ tự. Không thay V1/V2, không chỉnh response đã gửi, không reset3306. ID A/R/E2E được sinh mỗi lượt; dùng ID trong mysql-after.txt cho đối chiếu thay vì hardcode ID lượt trước. Token/key/password chỉ trong biến process, không dán vào tài liệu/ảnh.

Các API nội bộ dùng Service Key, không thêm nút tạo/gửi cho nhân viên. “Gửi tin thành công” trong ca thử nghĩa gateway mô phỏng chấp nhận. Lịch sử đầy đủ từng lần gửi không có bảng riêng; invitation chỉ có trạng thái/counter/timestamps. Tất cả bước ghi phải chạy DB test. Nếu cần phục hồi, dựng datadir test mới; giữ datadir cũ làm bằng chứng, không xóa response.

## TC01 — Đăng nhập hợp lệ

- **Mục tiêu:** Đăng nhập hợp lệ theo hợp đồng L8.
- **Điều kiện trước:** DB test đã bootstrap; biết password test do người chạy đặt.
- **Đầu vào:** manager101, marketing, director; bổ sung manager102 khi nghiệm thu thủ công.
- **Thao tác website/công cụ:** Mở /; nhập username/password; bấm Đăng nhập; đối chiếu menu; Đăng xuất rồi thử vai trò kế tiếp.
- **API liên quan:** POST /api/auth/login; GET /api/auth/me
- **Kết quả mong đợi:** 200, điều hướng theo role, không thấy menu ngoài quyền.
- **Kết quả thực tế / trạng thái:** PASS — Chromium đăng nhập 3 account, hiển thị đúng bảng/menu; manager102 chưa thao tác browser.
- **Minh chứng:** e2e.log; 03-technician-stats.png; 04-center-stats.png
- **SQL xác minh:** `SELECT username,role,center_id,enabled FROM app_user;`

## TC02 — Đăng nhập sai

- **Mục tiêu:** Đăng nhập sai theo hợp đồng L8.
- **Điều kiện trước:** Không có session đang đăng nhập.
- **Đầu vào:** marketing / password sai tổng hợp.
- **Thao tác website/công cụ:** Mở /; nhập marketing và password sai; bấm Đăng nhập; kiểm thông báo, vẫn ở form, không thấy dữ liệu nội bộ.
- **API liên quan:** POST /api/auth/login
- **Kết quả mong đợi:** 401 UNAUTHENTICATED, không cấp JWT.
- **Kết quả thực tế / trạng thái:** PASS mức API — HTTP401 trong browser-checks.json; bước nhập sai trên giao diện cần người dùng thực hiện thêm.
- **Minh chứng:** browser-checks.json (login401); ReportingIntegrationTest TXT
- **SQL xác minh:** `Không cần SQL; không log password.`

## TC03 — Tạo lời mời cho phiếu đóng

- **Mục tiêu:** Tạo lời mời cho phiếu đóng theo hợp đồng L8.
- **Điều kiện trước:** Scheduler false; 401 DA_DONG, chưa có invitation.
- **Đầu vào:** ticket_id=401; Service Key backend test hợp lệ.
- **Thao tác website/công cụ:** Không có nút tạo trên website. Terminal C chạy create API theo web-run-guide mục7; Workbench kiểm invitation; sau send mở link để khách thấy form.
- **API liên quan:** POST /api/survey-invitations
- **Kết quả mong đợi:** 201 CHO_GUI, đúng một invitation, token43 ký tự, TTL7 ngày.
- **Kết quả thực tế / trạng thái:** PASS — HTTP201 trên test DB; ảnh khách mở lời mời thật sau send.
- **Minh chứng:** browser-checks.json; mysql-before.txt; mysql-after.txt
- **SQL xác minh:** `SELECT invitation_id,ticket_id,status,created_at,expires_at FROM survey_invitation WHERE ticket_id=401;`

## TC04 — Phiếu không tồn tại

- **Mục tiêu:** Phiếu không tồn tại theo hợp đồng L8.
- **Điều kiện trước:** ID không có trong DB test, Service Key hợp lệ.
- **Đầu vào:** ticket_id=999999999.
- **Thao tác website/công cụ:** Không có màn hình tạo. Gọi API bằng Terminal C, kiểm404; mở Workbench xác nhận không có invitation tương ứng.
- **API liên quan:** POST /api/survey-invitations
- **Kết quả mong đợi:** 404 TICKET_NOT_FOUND, không thêm dữ liệu.
- **Kết quả thực tế / trạng thái:** PASS — HTTP404 đúng code.
- **Minh chứng:** browser-checks.json; SurveyInvitationServiceTest TXT
- **SQL xác minh:** `SELECT COUNT(*) FROM survey_invitation WHERE ticket_id=999999999;`

## TC05 — Phiếu chưa đóng

- **Mục tiêu:** Phiếu chưa đóng theo hợp đồng L8.
- **Điều kiện trước:** Ticket404 từ V2 có DANG_XU_LY.
- **Đầu vào:** ticket_id=404.
- **Thao tác website/công cụ:** Terminal C gọi create; Workbench kiểm trạng thái ticket và không có invitation. Website không có thao tác đổi trạng thái ticket.
- **API liên quan:** POST /api/survey-invitations
- **Kết quả mong đợi:** 422 TICKET_NOT_CLOSED, không ghi invitation.
- **Kết quả thực tế / trạng thái:** PASS — HTTP422 đúng hợp đồng.
- **Minh chứng:** browser-checks.json; mysql-after.txt
- **SQL xác minh:** `SELECT status FROM ticket WHERE ticket_id=404; SELECT COUNT(*) FROM survey_invitation WHERE ticket_id=404;`

## TC06 — Tạo trùng lời mời

- **Mục tiêu:** Tạo trùng lời mời theo hợp đồng L8.
- **Điều kiện trước:** 401 đã có invitation; chuẩn bị thêm ticket tổng hợp mới cho concurrency.
- **Đầu vào:** 401 lần hai; hai create đồng thời trên ticket R... mới.
- **Thao tác website/công cụ:** Gọi lại create, đối chiếu409; ở Workbench kiểm count1. Không xóa bản ghi để chạy lại.
- **API liên quan:** POST /api/survey-invitations
- **Kết quả mong đợi:** 409 INVITATION_ALREADY_EXISTS; hai request vào ticket mới trả201/409, một row.
- **Kết quả thực tế / trạng thái:** PASS — sequential409 và race201/409 trên API thật.
- **Minh chứng:** browser-checks.json; mysql-after.txt
- **SQL xác minh:** `SELECT ticket_id,COUNT(*) FROM survey_invitation GROUP BY ticket_id HAVING COUNT(*)>1;`

## TC07 — Gửi SMS/ZALO giả lập

- **Mục tiêu:** Gửi SMS/ZALO giả lập theo hợp đồng L8.
- **Điều kiện trước:** Invitation CHO_GUI, phone demo hợp lệ.
- **Đầu vào:** E2E gửi SMS; 401 gửi ZALO.
- **Thao tác website/công cụ:** Không có UI gửi nội bộ. Terminal C gọi /send; kiểm log gateway giả lập và MySQL; mở link trong trình duyệt; gọi send lại trước phản hồi để thử idempotency.
- **API liên quan:** POST /api/survey-invitations/{id}/send
- **Kết quả mong đợi:** 200 DA_GUI, channel đúng, sent_at có giá trị, send_attempts1; resend không gửi thành công thêm lần nữa.
- **Kết quả thực tế / trạng thái:** PASS — gateway runtime mô phỏng SMS/ZALO; service integration chứng minh idempotency. Không có bảng lịch sử từng tin; chỉ metadata invitation.
- **Minh chứng:** backend-startup.log; browser-checks.json; SurveyFlowIntegrationTest TXT
- **SQL xác minh:** `SELECT channel,status,send_attempts,sent_at,last_attempt_at FROM survey_invitation;`

## TC08 — Gateway lỗi và retry

- **Mục tiêu:** Gateway lỗi và retry theo hợp đồng L8.
- **Điều kiện trước:** Suite backend DB tạm; gateway MockitoBean thất bại. Demo thủ công có thể dùng GATEWAY_SIMULATE_FAILURE=true rồi restart backend test.
- **Đầu vào:** 3 lượt, giãn≥5 phút; lỗi phone thử riêng.
- **Thao tác website/công cụ:** Website không có UI retry nội bộ. Gọi send, kiểm502/metadata; gọi ngay bị409; sau5 phút gọi lại; tối đa3. Dùng tests tự động để không chờ, không chỉnh thời gian DB thật.
- **API liên quan:** POST /api/survey-invitations/{id}/send
- **Kết quả mong đợi:** 502 SMS_GATEWAY_ERROR, GUI_LOI được commit; retry không sớm hơn5 phút, không quá3; số phone sai422 INVALID_PHONE.
- **Kết quả thực tế / trạng thái:** PASS mức integration service — retry3 lần và commit metadata, không gọi gateway khi phone sai. Chưa đo thời gian chờ thực hoặc HTTP502 trực tiếp trong runner browser.
- **Minh chứng:** com.mekongmobile.smartcrm.survey.service.SurveyFlowIntegrationTest.txt
- **SQL xác minh:** `SELECT status,send_attempts,last_attempt_at FROM survey_invitation WHERE invitation_id=<ID_TEST>;`

## TC09 — Scheduler và nhắc72h

- **Mục tiêu:** Scheduler và nhắc72h theo hợp đồng L8.
- **Điều kiện trước:** Suite backend tạo fixture; thời điểm sent_at của fixture được đặt73h trước, còn hạn, chưa phản hồi.
- **Đầu vào:** Invitation mới và đủ72h; chạy tick/remind lặp lại.
- **Thao tác website/công cụ:** Không có nút nhắc hay endpoint public. Dùng test backend; demo quan sát metadata qua Workbench. Không chờ72h trong trình duyệt và không nói đã chờ thực.
- **API liên quan:** Tác vụ SurveyScheduler.tick / SurveyDeliveryService.remind
- **Kết quả mong đợi:** Tạo/gửi thiếu; nhắc một lần thành công, cùng token; giữ sent_at; không nhắc sớm; retry reminder≤3 cách5 phút.
- **Kết quả thực tế / trạng thái:** PASS mức integration với gateway mock và thời gian fixture; scheduler tick2 lần không duplicate.
- **Minh chứng:** SurveyFlowIntegrationTest TXT
- **SQL xác minh:** `SELECT status,sent_at,reminded_at,reminder_attempts,last_reminder_attempt_at FROM survey_invitation;`

## TC10 — Mở khảo sát hợp lệ

- **Mục tiêu:** Mở khảo sát hợp lệ theo hợp đồng L8.
- **Điều kiện trước:** Invitation DA_GUI, chưa response, chưa hết hạn.
- **Đầu vào:** Link từ API create/send.
- **Thao tác website/công cụ:** Mở link /khao-sat/{token}; kiểm mã phiếu, trung tâm, thiết bị, ngày đóng, expiry, điểm1–5 và nhận xét.
- **API liên quan:** GET /api/surveys/{token}
- **Kết quả mong đợi:** 200, dữ liệu từ MySQL; không cần login.
- **Kết quả thực tế / trạng thái:** PASS — Chromium form thật ở360 và4 viewport yêu cầu.
- **Minh chứng:** 01-survey-mobile.png; survey-390.png
- **SQL xác minh:** `SELECT t.ticket_code,t.center_id,t.device_id,i.status,i.expires_at FROM ticket t JOIN survey_invitation i USING(ticket_id) WHERE t.ticket_id=401;`

## TC11 — Điểm1–5 và validation

- **Mục tiêu:** Điểm1–5 và validation theo hợp đồng L8.
- **Điều kiện trước:** Mỗi điểm dùng một ticket/invitation mới; đừng tái dùng response bất biến.
- **Đầu vào:** Score1,2,3,4 qua HTTP;5 qua UI;0 và comment1001 ký tự bị từ chối.
- **Thao tác website/công cụ:** Chưa chọn điểm bấm GỬI PHẢN HỒI; kiểm validation; chọn5, nhập nhận xét tổng hợp, gửi; xem trang cảm ơn. Lặp lại bằng link mới cho1–4 nếu muốn nghiệm thu hoàn toàn bằng UI.
- **API liên quan:** POST /api/surveys/{token}/responses
- **Kết quả mong đợi:** 201, điểm nguyên1–5 lưu đúng; invalid400 VALIDATION_FAILED; không mất nhận xét khi lỗi tạm.
- **Kết quả thực tế / trạng thái:** PASS — UI5 và validation required; API thật1–4/invalid400; MySQL có đủ5 mức điểm. Chưa click gửi từng mức1–4 trên UI.
- **Minh chứng:** survey-success.png; score-check.txt; browser-checks.json; App.test.jsx suite
- **SQL xác minh:** `SELECT score,COUNT(*) FROM survey_response GROUP BY score ORDER BY score;`

## TC12 — Gửi lần hai, bất biến

- **Mục tiêu:** Gửi lần hai, bất biến theo hợp đồng L8.
- **Điều kiện trước:** Một survey đã có response5.
- **Đầu vào:** Gửi lại score1/comment overwrite; F5 link đã hoàn tất.
- **Thao tác website/công cụ:** F5 sau cảm ơn; form biến mất và báo đã gửi; gọi POST lại; Workbench so score/comment cũ.
- **API liên quan:** GET /api/surveys/{token}; POST /api/surveys/{token}/responses
- **Kết quả mong đợi:** 409 ALREADY_RESPONDED, vẫn1row, không overwrite; cạnh tranh2submit cũng1row.
- **Kết quả thực tế / trạng thái:** PASS — browser reload và API409; integration2submit + rollback đã pass.
- **Minh chứng:** e2e.log; browser-checks.json; mysql-after.txt; SurveyFlowIntegrationTest TXT
- **SQL xác minh:** `SELECT ticket_id,score,comment FROM survey_response WHERE ticket_id=401; SELECT ticket_id,COUNT(*) FROM survey_response GROUP BY ticket_id HAVING COUNT(*)>1;`

## TC13 — Token sai/hết hạn

- **Mục tiêu:** Token sai/hết hạn theo hợp đồng L8.
- **Điều kiện trước:** Test DB, tạo fixture riêng rồi đặt expires_at quá khứ chỉ cho fixture đó.
- **Đầu vào:** invalid; invitation test hết hạn.
- **Thao tác website/công cụ:** Mở /khao-sat/invalid; với link hết hạn mở link fixture; kiểm alert và không có form; đối chiếu response không tăng.
- **API liên quan:** GET /api/surveys/{token}; POST .../responses
- **Kết quả mong đợi:** 404 SURVEY_NOT_FOUND /410 SURVEY_EXPIRED; không lưu.
- **Kết quả thực tế / trạng thái:** PASS API thật404/410; integration từ chối expired; màn error token trên browser chưa chụp riêng.
- **Minh chứng:** browser-checks.json; mysql-after.txt; SurveyFlowIntegrationTest TXT
- **SQL xác minh:** `SELECT status,expires_at FROM survey_invitation;`

## TC14 — Thống kê CSAT/tỷ lệ

- **Mục tiêu:** Thống kê CSAT/tỷ lệ theo hợp đồng L8.
- **Điều kiện trước:** Đã có response; đúng tháng Việt Nam.
- **Đầu vào:** manager101; director; khoảng ngày hiện tại.
- **Thao tác website/công cụ:** Manager mở CSAT theo kỹ thuật viên → Làm mới số liệu; mở Tỉ lệ phản hồi. Director mở CSAT theo trung tâm, xem phân bố1→5; đối chiếu SQL với cùng kỳ.
- **API liên quan:** GET /api/survey-stats/by-technician; by-center; response-rate
- **Kết quả mong đợi:** AVG2chữ số, tỷ lệ0–1, null/— nếu0response; scope đúng; dữ liệu mới xuất hiện.
- **Kết quả thực tế / trạng thái:** PASS — browser bảng thật trước khi thêm điểm1–4; integration fixture kiểm AVG3.50, rate0.67, phân bố và null. Không dùng ảnh2response đối chiếu snapshot cuối6response.
- **Minh chứng:** 03-technician-stats.png; 04-center-stats.png; ReportingIntegrationTest TXT; verify.sql
- **SQL xác minh:** `Dùng hai truy vấn aggregate trong verify.sql, giữ cùng kỳ và center.`

## TC15 — Lọc và phân trang

- **Mục tiêu:** Lọc và phân trang theo hợp đồng L8.
- **Điều kiện trước:** Marketing hoặc manager; dữ liệu test đủ để có nhiều trang nếu kiểm UI pagination đầy đủ.
- **Đầu vào:** Center101, technician301, ngày hiện tại, điểm4–5; page1/2 size1 ở integration/API; UI size20/50/100.
- **Thao tác website/công cụ:** Mở /phan-hoi; nhập bộ lọc → Áp dụng; đổi Dòng/trang; bấm Sau/Trước khi đủ dòng; thử from>to/score_min>max; manager không đổi center.
- **API liên quan:** GET /api/survey-responses?..., /api/reference/*
- **Kết quả mong đợi:** total/items đúng bộ lọc, page từ1; UI resetpage khi lọc; ngày/điểm sai báo lỗi; input sai API400.
- **Kết quả thực tế / trạng thái:** PASS integration filter/page và UI unit mock; browser thật hiển thị2rows, chưa có dữ liệu>20 để bấm Sau.
- **Minh chứng:** ReportingIntegrationTest TXT; frontend-tests.txt; manager101-phan-hoi-1366.png
- **SQL xác minh:** `SELECT t.ticket_code,r.score FROM survey_response r JOIN ticket t USING(ticket_id) WHERE t.center_id=101 AND t.technician_id=301 AND r.score BETWEEN 4 AND 5;`

## TC16 — Xuất XLSX

- **Mục tiêu:** Xuất XLSX theo hợp đồng L8.
- **Điều kiện trước:** Marketing, bộ lọc đã Áp dụng; ít hơn10.000rows.
- **Đầu vào:** Bộ lọc hiện tại, snapshot2responses lúc download.
- **Thao tác website/công cụ:** Marketing /phan-hoi → Xuất Excel; kiểm thông báo tải; mở file trong Excel/LibreOffice; so dữ liệu/cột với cùng snapshot danh sách; manager không có nút.
- **API liên quan:** GET /api/survey-responses/export?...
- **Kết quả mong đợi:** 200 MIME XLSX,7cột đúng, số che, bộ lọc thống nhất; role khác403; >10.000 theo contract422 EXPORT_TOO_LARGE.
- **Kết quả thực tế / trạng thái:** PASS download/ZIP-XML và header/data snapshot; BLOCKED phần mở Excel desktop và biên10.001rows chưa thực thi.
- **Minh chứng:** responses.xlsx; xlsx-inspection.txt; e2e.log; ResponseWorkbookTest TXT
- **SQL xác minh:** `Đối chiếu api-response-list.json snapshot trước khi thêm1–4; không so file này với snapshot cuối6response.`

## TC17 — Sai role/scope

- **Mục tiêu:** Sai role/scope theo hợp đồng L8.
- **Điều kiện trước:** Manager101 và director đã login.
- **Đầu vào:** center_id102 với manager101; director gọi responses.
- **Thao tác website/công cụ:** Manager vào /phan-hoi thử URL/query ngoài quyền qua DevTools/API (không sửa secret); director gõ /phan-hoi; kiểm403 ở API và trang không quyền.
- **API liên quan:** Các report endpoints và reference endpoints
- **Kết quả mong đợi:** 403 FORBIDDEN_SCOPE; không trả dữ liệu vượt trung tâm; backend đọc lại role/center từ DB.
- **Kết quả thực tế / trạng thái:** PASS HTTP403 center102; E2E director403; integration full role matrix5endpoint.
- **Minh chứng:** browser-checks.json; e2e.log; ReportingIntegrationTest TXT
- **SQL xác minh:** `SELECT username,role,center_id FROM app_user;`

## TC18 — Refresh và URL trực tiếp

- **Mục tiêu:** Refresh và URL trực tiếp theo hợp đồng L8.
- **Điều kiện trước:** Session đang tồn tại trong tab; Vite phục vụ SPA.
- **Đầu vào:** Các URL đúng role và URL role khác.
- **Thao tác website/công cụ:** Login; F5 từng màn; dán URL trực tiếp; Back/Forward; logout rồi vào URL nội bộ; đóng/mở tab thử lại.
- **API liên quan:** GET /api/auth/me khi dùng; các data endpoints của màn
- **Kết quả mong đợi:** Trong tab còn JWT thì data load; chưa login hiện form; URL cấm hiện không quyền; logout xóa session.
- **Kết quả thực tế / trạng thái:** PASS browser reload/direct URL cho các màn; Back/Forward và vòng đời đóng tab cần kiểm thủ công bổ sung. App dùng History API, không React Router.
- **Minh chứng:** browser-checks.json; frontend-tests.txt; ảnh mỗi route/viewport
- **SQL xác minh:** `Không cần SQL.`

## TC19 — Thiếu/sai JWT/Service Key, rate limit

- **Mục tiêu:** Thiếu/sai JWT/Service Key, rate limit theo hợp đồng L8.
- **Điều kiện trước:** Server test; giữ10request/phút cho public API.
- **Đầu vào:** Thiếu key, sai key, Bearer incorrect; JWT expired/tampered trong integration;11calls /api/surveys/bad.
- **Thao tác website/công cụ:** Terminal C gọi API thiếu/sai credential, không in credential; public link gọi quá10 trong1phút; kiểm lỗi đúng. Website hết phiên thì logout/login lại.
- **API liên quan:** EP1–2, EP5–9; /api/surveys/*
- **Kết quả mong đợi:** 401 UNAUTHENTICATED; role403; public lần11→429 RATE_LIMITED, Retry-After. Không quy401 thành403 tùy ý.
- **Kết quả thực tế / trạng thái:** PASS HTTP401 thật; integration expired/tampered/matrix và HTTP429. Rate limiter public, không phải mọiAPI đều10/phút.
- **Minh chứng:** browser-checks.json; SurveyFlowIntegrationTest TXT; ReportingIntegrationTest TXT
- **SQL xác minh:** `Không cần SQL.`

## TC20 — Loading/empty/error/success

- **Mục tiêu:** Loading/empty/error/success theo hợp đồng L8.
- **Điều kiện trước:** Marketing đã login và có data; DevTools hoặc browser harness.
- **Đầu vào:** Delay1.8s API thật; empty200 và error503 mô phỏng; submit success thật.
- **Thao tác website/công cụ:** F12 Network throttle rồi reload để xem loading; lọc kỳ không có dữ liệu để empty thật; tắt backend riêng rồi reload để error, bật lại → Thử lại; khách gửi survey để success. Harness dùng route mock có ghi nhãn cho empty/error.
- **API liên quan:** GET /api/survey-responses; POST survey responses
- **Kết quả mong đợi:** Mỗi trạng thái hiển thị, không trắng trang; Thử lại phục hồi; submit success; dữ liệu form giữ khi lỗi tạm.
- **Kết quả thực tế / trạng thái:** PASS browser delayed real API, mockedempty/error và retry về API thật; success thật. Chưa chụp tình huống tắt hẳn backend.
- **Minh chứng:** ui-loading-delayed-real-api.png; ui-empty-mocked.png; ui-error-mocked-503.png; survey-success.png; frontend-tests.txt
- **SQL xác minh:** `Empty/error mock không chứng minh DB rỗng/lỗi; dùng verify.sql cho dữ liệu thật.`

## Giao diện và nghiệm thu thủ công bổ sung

Dùng DevTools Device Toolbar đặt1920×1080,1366×768,390×844,375×667; kiểm form khách và5trường hợp route/role trong browser-checks.json. Harness đã kiểm overflow toàn trang; bảng rộng cuộn trong vùng table-scroll là chủ ý, không đồng nghĩa tràn toàn trang. Ở mobile vuốt bảng ngang để xem đủ cột; kiểm nút filter/pagination, menu và tiếng Việt. Chưa có phép đo tương phản WCAG hay usability5người; không đánh PASS các chuẩn đó từ ảnh.

Người nghiệm thu ghi ngày/người thực hiện, kết quả từng bước còn chờ ở TC02/11/13/15/16/18/20 vào báo cáo; chụp ảnh sạch credential. Không dùng browser trace thô làm evidence công khai vì có thể chứa JWT/token. Tắt backend chỉ được thao tác tiến trình test của bạn. Không có tính năng thực tế thì ghi “không áp dụng”, không dựng ảnh.

