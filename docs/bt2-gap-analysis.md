# Đối chiếu BT2 Smart CRM – Mekong Mobile L8

Ngày audit: 03/11/2026. Baseline: branch `feat/bt2-setup`, HEAD `1f62008`; trạng thái trước thực hiện có sửa `SurveyInvitation.java` chưa commit, `../AGENTS.md`, `backend.zip` và báo cáo DOCX chưa tracked. Không ghi đè các thay đổi đó. Bảng baseline bên dưới không được hiểu là kết quả nghiệm thu sau triển khai.

Quy ước: **DONE** = có mã và kết quả kiểm chứng tương ứng trong lần thực hiện; **PARTIAL** = mới có một phần; **NOT_IMPLEMENTED** = chưa có mã chức năng; **NOT_VERIFIED** = có tài liệu/mã nhưng chưa đủ bằng chứng chạy. Không dùng ảnh/báo cáo cũ để suy ra tests hiện tại pass.

## Nguồn và độ đầy đủ của hồ sơ

Đã đối chiếu `../AGENTS.md`, `srs.md` (đủ US1–US8, UC01–UC09, FR01–12/NFR01–08), `api-contract.md` (9 endpoint), `usecase-diagram.drawio` (7 actor, 9 UC, include/extend), `wireframe-l8.png` (đã xem ảnh), văn bản báo cáo BT1 DOCX và báo cáo tiến độ BT2 DOCX/PDF hiện có. PDF BT1 và sơ đồ PDF là bản trình bày đi kèm; nội dung nghiệp vụ được đối chiếu từ DOCX/Markdown/Drawio gốc, chưa kiểm tra từng trang PDF bằng mắt.

Thư mục `docs/requirements/` và `Codex_SmartCRM_L8_BT2_HoanThien.md` không có trong repository; tìm theo tên dưới `D:/cdtn1` chưa thấy file hướng dẫn này. Không thể xác nhận đã thực hiện chỉ dẫn riêng trong file chưa được cung cấp. Đã tìm thấy đề BT2 và học liệu buổi 7–12 ở thư mục cha `D:/cdtn1/bt2`; bản Markdown tại `D:/cdtn1/bt2/CDTN1_10_PDF_sang_Markdown/CDTN1_10_files_markdown/`. Đây là nguồn rubric thực tế, không suy đoán từ prompt. Các bản chuyển PDF → Markdown chứa ảnh trang để đối chiếu, chưa đưa tài liệu bên ngoài này vào repository.

| Tài liệu ngoài repo | Nội dung áp dụng cho track SE |
|---|---|
| `CDTN1_De bai Bai tap 2 - Hien thuc hoa module cot loi(1).md`, mục 2–4, trang 3–9 | 5 thành phần; test đúng/biên/sai; C1–C7/S1–S4; thay đổi thiết kế; khai báo AI; clone-and-run |
| Buổi 07 Xây dựng module cốt lõi; tự học Module cốt lõi theo chuyên ngành | Controller–Service–Repository; validation 3 tầng; service không HTTP; migration/seed; Git |
| Buổi 08 Workshop module cốt lõi Sprint 1 | Chạy luồng chính; kiểm chứng quy tắc; kiểm thử; chạy lại và minh chứng |
| Buổi 09 Giao tiếp giữa các thành phần; tự học Trình bày kết quả | Hợp đồng dữ liệu, client API riêng, đăng nhập cơ bản; loading/empty/error; retry, mốc so sánh |
| Buổi 10 Workshop module mở rộng và Giao diện Sprint 2 | 3 màn hình API thật; ghép toàn luồng; tắt backend kiểm lỗi; thử README ở thư mục mới |
| Buổi 11 Tích hợp và Xử lý lỗi; tự học Đóng gói và cấu hình | Cấu hình môi trường, timeout/retry hữu hạn, không nuốt lỗi, không log secret, tự kiểm dựng lại |
| Buổi 12 Workshop tích hợp và Nộp BT2 | Kiểm C1/C2/C4/C5 trước, PDF/link/tag, không coi mục thiếu minh chứng là đạt |

## Baseline toàn bộ phạm vi L8

| UC / US / FR / mức | API / tác vụ theo hợp đồng | Backend baseline / database | Frontend baseline | Test case hiện có / còn thiếu | Trạng thái baseline | Rubric liên quan |
|---|---|---|---|---|---|---|
| UC01 / US2 / FR01–02 / MUST | EP1 POST `/api/survey-invitations` | Có controller/service/repository/entity/DTO; kiểm DA_DONG, chống trùng, SecureRandom 32 byte; V1 UNIQUE ticket/token. Chưa có tự kích hoạt sự kiện L2/L4 hoặc gửi nền; TTL 7 ngày ghi cố định | Không áp dụng UI tạo lời mời; chỉ hệ thống gọi | `SurveyInvitationServiceTest` luồng đúng, phiếu sai, trùng, không tồn tại, ID=0; thiếu HTTP/concurrency MySQL | PARTIAL | C2,C3,C4,C5,C7,S2,S3 |
| UC02 / US2 / FR03 / MUST | EP2 POST `/api/survey-invitations/{id}/send` | Có gateway interface/giả lập, phone validator, request/response DTO; chưa có endpoint/service gửi, khóa tranh chấp, retry 5 phút. Entity có markSent/markSendFailed chưa commit từ người dùng | Không cần UI gửi thủ công hoặc lộ service key | Có gateway/phone/DTO tests; thiếu gửi thành công SMS/ZALO, lỗi phone, gateway, trạng thái, retry/giới hạn/đồng thời | PARTIAL | C2,C3,C4,C7,S2,S3 |
| UC03 / US1 / FR04–05 / MUST | EP3 GET `/api/surveys/{token}`; EP4 POST `.../responses` | V1 có survey_response UNIQUE/CHECK, chưa entity/repository/service/controller; thiếu TX chung, duplicate 409, expiry, rate limit, sanitize | Chưa có React/form/trang cảm ơn | Thiếu happy path, score 1/5, invalid score/comment, token 404/409/410/429, rollback 503/concurrency | NOT_IMPLEMENTED | C1,C2,C3,C4,C5,C7,S2,S3,S4 |
| UC04 / US8 (SRS), US2 mở rộng (BT1) / FR11 / COULD | Scheduler, không có API công khai | V1 có reminded_at; chưa scheduler/logic nhắc sau 72h một lần, cùng token; không gửi khi hết hạn/đã trả lời | Không áp dụng | Thiếu cutoff 72h/expiry/once-only/restart test | NOT_IMPLEMENTED | C3,C4,C5,S2,S3 |
| UC05 / US6 / FR08,FR12 / SHOULD | EP7 GET `/api/survey-responses` | Chưa có API lọc theo center/technician/date/score, phân trang, che phone; V1 có join/index cần thiết cơ bản | Chưa có màn hình danh sách, lọc, phân trang, states | Thiếu filter combination, 1-based page/size, masking, scope 403, empty/error | NOT_IMPLEMENTED | C1,C2,C3,C4,C7,S2,S4 |
| UC06 / US3 / FR06,FR12 / MUST | EP5 GET `/api/survey-stats/by-technician` | Chưa có aggregate. V1 technician chưa tên; cần dữ liệu tham chiếu tên thật/demo trong DB. Không có JWT/scope | Chưa có màn hình bảng kỹ thuật viên, null → “—” | Thiếu AVG/rate/rounding, KTV rỗng, sắp xếp, date boundary, manager scope | NOT_IMPLEMENTED | C1,C2,C3,C4,C7,S2,S4 |
| UC07 / US4 / FR07,FR12 / SHOULD | EP6 GET `/api/survey-stats/by-center` | Chưa aggregate theo trung tâm/tháng/distribution 1–5 và executive role | Chưa có tổng hợp/xu hướng từ API thật | Thiếu tháng/trung tâm, rounding, empty, executive 401/403 | NOT_IMPLEMENTED | C2,C3,C4,C7,S2,S4 |
| UC08 / US5 / FR09 / SHOULD | EP8 GET `/api/survey-responses/export` | Chưa có XLSX, cùng bộ lọc EP7, giới hạn 10.000, mask phone/marketing role | Chưa có xuất Excel theo bộ lọc; chỉ Marketing thấy | Thiếu parse file XLSX/header/filter/mask/limit 422/role 403 | NOT_IMPLEMENTED | C2,C3,C4,C7,S2,S4 |
| UC09 / US7 / FR10 / COULD | EP9 GET `/api/survey-stats/response-rate` | Chưa aggregate denominator DA_GUI/DA_PHAN_HOI, zero/null, theo tháng và scope | Chưa màn phụ tỷ lệ phản hồi của manager | Thiếu denominator đúng không tính GUI_LOI, date/month/scope/empty | NOT_IMPLEMENTED | C2,C3,C4,C7,S2,S4 |
| Toàn phạm vi / FR12 / MUST | EP1–2 service key; EP3–4 token; EP5–9 JWT | `ServiceKeyFilter` có so key constant-time, từ chối duplicate header; chưa JWT/đăng nhập, role/scope nội bộ | Chưa đăng nhập/điều hướng theo role | `ServiceKeyFilterTest`; thiếu token giả/hết hạn, 401/403 mọi endpoint, sửa center_id | PARTIAL | C2,C3,C4,C7,S2,S4 |

## Rubric BT2 – baseline

| Mã | Tiêu chí đúng đề BT2 §4.2 | Trạng thái baseline | Bằng chứng và khoảng trống |
|---|---|---|---|
| C1 (cốt lõi) | Chạy từ trạng thái đầu theo README, không can thiệp ngoài hướng dẫn | NOT_IMPLEMENTED | README chỉ 2 dòng; thiếu môi trường, cấu hình, MySQL/database, frontend, lệnh chạy/kiểm tra |
| C2 (cốt lõi) | Ba lớp tách nơi và đúng chiều | DONE | Kiểm tra source baseline: controller → service → repositories; HTTP tại controller/filter/advice, repository không gọi ngược. DONE ở mức kiểm tra cấu trúc, không khẳng định luồng chưa có chạy được |
| C3 | Ít nhất 2 quy tắc nghiệp vụ thật | PARTIAL | UC01 kiểm trạng thái đóng và mỗi phiếu một lời mời, tests tương ứng có source; kết quả chạy hiện tại chờ xác nhận |
| C4 (cốt lõi) | ≥3 test thuộc đúng/biên/sai, PASS bằng một lệnh | NOT_VERIFIED | Có 8 test classes, nhưng context/repository integration dùng DB_USER/DB_PASSWORD máy cá nhân và seed cố định. Cần test DB độc lập và chạy nguyên suite, không skip lỗi |
| C5 (cốt lõi) | Lần hai không lỗi, không nhân đôi | NOT_VERIFIED | Flyway history thiết kế tránh replay V1/V2; chưa tự kiểm 2 lần ở DB độc lập và chưa concurrency business |
| C6 | Lockfile đã commit, env.example đủ biến, không .env thật | PARTIAL | POM pin Boot 4.1.2/Java21, Maven wrapper; gitignore chặn .env, chưa env.example/frontend lockfile và chưa audit secrets lịch sử |
| C7 | Input sai bị từ chối rõ ràng, không lỗi thô | PARTIAL | DTO/advice UC01 có validation JSON/error fields; chưa phủ endpoint khác; error DATABASE_ERROR chưa đúng API code chung |
| S1 | ≥2 entity đủ CRUD | NOT_IMPLEMENTED | Mâu thuẫn nghiệp vụ đã nộp: survey_response bất biến, invitation không xóa, reference L2/L4 chỉ đọc. Không thêm CRUD để lấy điểm; cần GV chấp nhận điều chỉnh tiêu chí bằng văn bản |
| S2 | Service không req/res/status HTTP | DONE | SurveyInvitationService và SurveyPhoneValidator không import HTTP, exception chỉ mã nghiệp vụ; handler ánh xạ status |
| S3 | Migration từ CSDL trống + seed | NOT_VERIFIED | V1 schema 6 bảng, V2 2 centers/2 customers/2 technicians/4 tickets, chưa có survey demo. Chưa dựng mới trong lần audit này |
| S4 | ≥3 màn hình API thật, loading/empty/error | NOT_IMPLEMENTED | Không có frontend ở baseline; wireframe chỉ ảnh thiết kế |

## Mâu thuẫn và quyết định không làm sai thiết kế

1. **S1 CRUD và QT10/QT13/KS06:** không xây edit/delete phản hồi, không CRUD dữ liệu L2/L4. Giữ khoảng trống S1 cho đến khi GV xác nhận cách chấm phù hợp. Có thể đề xuất GV đánh giá create/read và lifecycle transaction của hai bảng L8 nhưng không tự coi là đạt full CRUD.
2. **SRS FR12/QT15 và API Contract §2:** SRS cho quản lý/giám đốc ngoại lệ số đầy đủ, API nói luôn che mọi response và không endpoint nào trả đầy đủ. Theo yêu cầu người dùng và API, trả che mọi vai trò, kể cả XLSX; ghi vào thay đổi thiết kế.
3. **US8:** SRS cũ có US8 riêng; báo cáo BT1 đã gộp vào US2 mở rộng vì giới hạn số story. UC04/FR11 không mất; theo dõi cả hai mã để không bỏ chức năng.
4. **A03–A06 còn chờ GV:** TTL 7 ngày, reminder 72h, Marketing toàn công ty, ngưỡng score≥4 là giá trị thiết kế hiện hành, không mô tả thành giả định đã được GV phê duyệt. Người dùng đã yêu cầu triển khai toàn bộ thiết kế L8; hiện thực các giá trị này nhưng giữ ghi nhận trạng thái hồ sơ.
5. **Schema rút gọn:** BT1 ERD chỉ 6 bảng; tên thiết bị/KTV thuộc L2/L4 chưa có ở V1. Bổ sung qua migration mới hoặc cung cấp dữ liệu tham chiếu tích hợp có định danh demo rõ. Không hiển thị mã ID thành tên thiết bị giả, không ghi số liệu mẫu trong React.
6. **Ranh giới L2/L4:** module đóng phiếu ngoài L8. EP1 là cổng nhận sự kiện; cần ghi rõ nguồn sự kiện khi demo, không thêm sửa trạng thái ticket trong L8. Quan hệ include UC01→UC02 cần tác vụ gửi tự động sau tạo, không buộc user gọi thủ công để coi hoàn tất.
7. **Bảo toàn dữ liệu:** giữ checksum V1/V2, migration mới chỉ mở rộng có chủ đích. Tests phải dùng MySQL test riêng hoặc môi trường tự dựng; không clean/truncate/delete DB hiện có để tạo bằng chứng.

## Tiêu chí nghiệm thu và thứ tự tiếp tục

1. Chạy suite baseline và ghi nguyên kết quả; sửa khả năng dựng tests độc lập trước khi kết luận C4. Ghi file/lệnh/kết quả theo checkpoint.
2. UC02: endpoint thật, key, khóa trạng thái, commit GUI_LOI khi lỗi, attempts≤3/retry≥5 phút, gateway test SMS/ZALO và failure. Giữ V1/V2 nguyên vẹn.
3. MUST UC03/UC06 + FR12: token/error/TX/unique/rate limit; scope; query từ MySQL, null đúng; integration và concurrency.
4. UC05/UC07/UC08/UC09/UC04: API thật, filters/role/boundary, scheduler 72h một lần; truy vết tests.
5. React 3 màn hình wireframe + login và màn phụ; client riêng; mỗi màn có loading/empty/error/retry, không dữ liệu hardcode.
6. README/env/lockfile/design changes/AI/architecture/report; thử dựng ở thư mục mới và chạy lại. Chỉ báo DONE cho hạng mục đủ bằng chứng. Tag/push/merge là thao tác cuối do người dùng phê duyệt, chưa thực hiện.

## Cập nhật sau audit

Baseline ở trên được giữ làm mốc. Bảng hiện tại đánh giá chức năng đã triển khai, không tuyên bố mọi NFR đã đạt. Kết quả cuối: `mvnw.cmd verify` **46 tests PASS,0fail/error/skip**, package thành công lúc07:35:37; `npm test` **17PASS**, buildPASS; Playwright E2E freshV5 **1PASS/6,5giây**,7ảnh thật. Source copy sạch cùng máy chạy npmci/17tests/build và backend one-command46PASS trên MySQL mới V1–V5. Runner ép datasourceCLI vào DB tạm, không chịu TEST_DB_* kế thừa. Runtime8081/3307đãdừng; MySQL gốc3306giữnguyên.

### Ma trận hiện tại – chức năng

| UC / US / FR | API / frontend / database hiện thực | Bằng chứng tương ứng | Trạng thái hiện tại / giới hạn |
|---|---|---|---|
| UC01 / US2 / FR01–02 | `SurveyInvitationController/Service`, repository lock/unique, EP1; scheduler đọc DA_DONG qua `SurveyJobRepository`; V1 giữ nguyên | `SurveyInvitationServiceTest`5; Flow test close-event concurrent và scheduler tick2lần không duplicate; E2E fixture tạo qua EP1 thật | DONE chức năng; nguồn event L2/L4 thật vẫn ngoài phạm vi |
| UC02 / US2 / FR03 | `SurveyDeliveryController/Service`, gateway, V4 timestamp retry; EP2; chỉ service-key, không UI nội bộ lộ key | Flow integration: gửi ZALO/idempotent, phone sai commit GUI_LOI, gateway3 lần/giãn5 phút, expired; E2E gọi send API thật | DONE cho luồng chức năng; SLO gửi5 phút trên500phiếu chưa đo |
| UC03 / US1 / FR04–05 | `SurveyResponseController/Service/Repository`, filter rate-limit; EP3/4; React `Survey`; V1 response unique, TX/lock | Flow integration sanitize/immutable/input/expiry/unsent; 2request concurrent→1response; rollback trigger; HTTP429; E2E submit360px rồi duplicate409 | DONE chức năng; NFR100concurrent và tải p95 chưa đo |
| UC04 / US8 hoặc US2 mở rộng / FR11 | `SurveyScheduler`, delivery.remind, V5 metadata retry nhắc; nền, không API/UI công khai | Integration reminder dùng lại token/sent_at, một lần thành công, retry gateway tối đa3/5phút PASS trong suite46 | DONE chức năng; thời gian được thiết lập fixture, không chờ72giờ ngoài đời |
| UC05 / US6 / FR08,12 | EP7 `ReportingController/Service/Repository`, filters/scoping/mask/page; React `Responses` | `ReportingIntegrationTest.listsPaginateFilterMaskAndRespectInclusiveDates`, scope/mismatch; FE filters/page/error/empty tests; E2E nhìn response mới/phone che/manager centerdisabled | DONE |
| UC06 / US3 / FR06,12 | EP5 aggregate repository, scope manager, React `TechnicianStats` | Integration KTV0response/null/AVG/rate/center isolation; E2E bảng từ MySQL mới gửi | DONE; tên tham chiếu L2/L4 thiếu thì dùng định danh #ID công khai rõ |
| UC07 / US4 / FR07,12 | EP6 by-center/month/distribution/trend, React `CenterStats` | Integration distribution/CSAT; E2E director bảng thực, center rỗng null và điều hướng role | DONE; p95/10k chưa đo |
| UC08 / US5 / FR09 | EP8 XLSX đúng filters/mask, giới hạn10.000; nút Marketing | `ResponseWorkbookTest` mở ZIP/XML format; reporting export từ DB; E2E Marketing download.xlsx thật | DONE chức năng; biên10.000/10.001 chưa có phép thử dữ liệu lớn riêng |
| UC09 / US7 / FR10 | EP9 cohort sent month, React `ResponseRate`, manager scope | Integration 2/3→0.67; E2E manager bảng tỷ lệ thật | DONE; giải thích cohort tại design changes, không chia hai tập độc lập |
| FR12 / xuyên suốt | `AuthService/UserRepository`, BCrypt/JWT/login/me/bootstrap; controller kiểm roles, service scope; API metadata và frontend role nav | Login/tampered/expired/disabled/wrong-role integration; E2E4vai trò gồm public, director list403; full HTTP role matrix5endpoint PASS trong suite46 | DONE cơ chế có kiểm chứng; chưa tuyên bố kiểm bảo mật phân tán hoặc pentest |

### Ma trận hiện tại – rubric

| Tiêu chí | Trạng thái hiện tại | Bằng chứng / việc còn lại |
|---|---|---|
| C1 | DONE | Source copy sạch cùng máy: backend one-command46PASS, frontend npmci/17tests/buildPASS; có dependency cache, chưa kiểm máy khác hoặc Gitclone tag |
| C2 | DONE | Controller→service→repository, SQL/HTTP tách khỏi services; scheduler dùng SurveyJobRepository. Static check, suite46 và E2E thật |
| C3 | DONE | Closed-only, unique invitation/response, immutable, retry/expiry và scope có unit/integration thực |
| C4 | DONE | Backend nguyên suite46PASS0fail/error/skip+17FE; đúng/biên/sai, không skip. Bản sao frontend17PASS/backend46PASS |
| C5 | DONE | Migration context up-to-date, duplicate2concurrent pass; scheduler tick2lần không tạo/gửi trùng; freshV1–V5 bản sao đã áp dụng/kiểm riêng |
| C6 | PARTIAL | Maven/POM pin, frontend package-lock, env.example đủ biến, env thật ignored; file mới chưa commit nên chưa đạt phần “đã commit” literal của rubric |
| C7 | DONE | Structured validation/controller advice, invalid JSON/score/filter/token/auth; FE giữ form khi503, retry; test sai có pass |
| S1 | NOT_IMPLEMENTED | Mâu thuẫn bất biến/chỉ đọc giữ nguyên; chờ GV xác nhận cách chấm, không tự tạo CRUD |
| S2 | DONE | Services nhận dữ liệu thuần, không org.springframework.http/servlet/status, SQL ở repositories; exception chỉ business code |
| S3 | DONE | V1–V5 fresh MySQL8.0.46 bản sao áp dụng thành công/seed có dữ liệu; checksumV1/V2giữnguyên |
| S4 | DONE | Ba màn wireframe API thật+login/màn phụ; E2E MySQL thật;17UItests;7ảnh data/loading/empty/error, provenance rõ |

### NFR và hồ sơ còn thiếu

| Mục | Trạng thái | Giới hạn chính xác |
|---|---|---|
| NFR01/02/03 | NOT_VERIFIED | Chưa đo p95 form100truycập/API50requests/10.000responses |
| NFR04 | PARTIAL | SecureRandom256bit/token43, expiry và HTTP11lần→429 đã test; chưa kiểm nhiều instance hoặc lưu rate-limit phân tán |
| NFR05 | PARTIAL | UNIQUE+lock, 2concurrent response và close-event duplicate có test; chưa100requests→1/99 |
| NFR06 | NOT_VERIFIED | Chưa chạy500phiếu chứng minh99% gửi≤5phút |
| NFR07 | PARTIAL | E2E360px không overflow; chưa5người thật/30giây/3chạm |
| NFR08 | DONE chức năng | E2E gửi phản hồi rồi xem ngay list/stats từ DB thành công, không cache/hardcode; không suy ra cam kết tải lớn |
| Hồ sơ PDF/ảnh/clone Git | PARTIAL | Đã có Markdown, PDF mới8trang và7ảnh data/states; PDF/DOCX cũ giữ nguyên. Chưa commit/clone theo tag/push/merge/tag/nộpE-learning |
| Hướng dẫn riêng HoanThien | NOT_VERIFIED | File không có trong phạm vi tìm; cần user cung cấp nguồn trước khi khẳng định đã tuân toàn bộ chỉ dẫn riêng |

Ảnh/provenance ở [evidence/README.md](evidence/README.md); file/lệnh/lỗi/kết quả cuối ở [checkpoint-2.md](checkpoint-2.md). Checkpoint1 giữ lịch sử. PDF8trang được xuất Chromium từ Markdown và ảnh thật, đã xem preview bố cục.

## Cập nhật nghiệm thu – 12/11/2026

Phần trên giữ nguyên baseline và kết quả ngày 03/11. Đối chiếu mới với source hiện tại và chạy lại trên MySQL test mới V1–V6:

| Hạng mục | Trạng thái | Bằng chứng mới |
|---|---|---|
| UC01–09 và EP1–9 | PASS chức năng | Mã backend/frontend hiện có; E2E thật, API/browser checks và DB V1–V6 ở `docs/evidence/web-acceptance/`. |
| CRUD S1 | Có mã/test; chờ xác nhận học thuật | Lời mời có list/detail/PATCH kênh trước gửi/DELETE vật lý khi chưa gửi; cancellation tombstone chặn tái phát hành. Marketing có CRUD mẫu khảo sát; nội dung được snapshot vào invitation. Không sửa/xóa response, không CRUD bảng L2/L4. `SurveyFlowIntegrationTest` kiểm tra role, validation, lifecycle và snapshot. |
| C1/C4/S3 | PASS cục bộ; clone theo tag còn chờ | `scripts/test.ps1`: 54 tests PASS, 0 fail/error/skip; V1–V6 trên MySQL 8.0.46 tạm mới cổng3307. npm test 18 PASS/build PASS. Sẽ thử bản clone sạch sau commit; hiện chưa là bằng chứng máy khác/cache trống. |
| C6 | PARTIAL trước commit | `.env.example`, Maven/npm lockfile có; `.env` bị ignore và không tracked. Phần “đã commit” chỉ được tính sau khi commit kiểm tra sạch. |
| NFR01 | Có số đo cục bộ; chưa đủ điều kiện SRS | MockMvc 100 client/IP riêng, p95 46 ms/max230 ms. Không mô phỏng mạng 4G và không qua TCP thật. |
| NFR02 | Có số đo cục bộ; chưa đủ điều kiện SRS | MockMvc 50 client/IP riêng, p95 362 ms/max364 ms; 50 token khác nhau, 50 phản hồi được lưu. Không mô phỏng mạng ngoài. |
| NFR03 | Có số đo local MySQL test | 10.000 response; 20 HTTP samples mỗi endpoint: KTV p95 75/max219 ms, trung tâm 41/45 ms, danh sách 71/72 ms. |
| NFR04 | PARTIAL | 11 request/phút/IP trả429 và token 256 bit đã test; limiter hiện giữ trạng thái trong một instance, chưa kiểm nhiều instance/proxy/pentest. |
| NFR05 | PASS trong test servlet/DB đơn instance | 100 POST đồng thời qua controller+filter và 100 IP test: 1×201, 99×409, DB đúng một response. Chưa thử nhiều instance. |
| NFR06 | PASS với gateway giả lập | Scheduler xử lý 500 phiếu: 500/500 gửi trong vòng 5 phút, 6 tick, 7.012 ms; không gọi SMS/Zalo nhà cung cấp thật. |
| NFR07 | Chưa kiểm chứng người dùng | Có [kế hoạch/phiếu ghi nhận](usability-test-plan.md); chưa có 5 người tham gia và không tạo dữ liệu giả. |
| NFR08 | PASS luồng chức năng | E2E gửi xong thấy dữ liệu ở list/statistics; không suy rộng thành tải lớn. |
| Tái lập | PASS trên source hiện tại; clone sạch còn chờ | E2E một journey PASS và browser/API checks PASS trên schema test mới; server test đã dừng, cổng3307/8080/5173 trống. |
| File hướng dẫn riêng | NOT_FOUND | `Codex_SmartCRM_L8_BT2_HoanThien.md` không thấy trong repository, Downloads hoặc cây `D:\cdtn1` tại lượt kiểm này. |

Thay đổi S1 cần giảng viên xác nhận về cách tính CRUD có điều kiện theo vòng đời. Không ghi nhận việc đã được duyệt. Xem [checkpoint3](checkpoint-3.md), [bảng xác minh](verification-2026-11-12.md), [ERD V6](erd-l8-extension.md) và [bằng chứng trình duyệt mới](evidence/web-acceptance/README.md). Báo cáo 03/11 được giữ trong `docs/evidence/web-acceptance-2026-11-03/`.
