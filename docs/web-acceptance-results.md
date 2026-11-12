# Kết quả nghiệm thu thực tế — Smart CRM Mekong Mobile L8

> Báo cáo dưới đây là hồ sơ lịch sử ngày 03/11/2026. Kết quả mới ngày 12/11 sau CRUD/V6 và chạy lại toàn bộ suite được ghi tại [verification-2026-11-12.md](verification-2026-11-12.md) cùng [evidence hiện hành](evidence/web-acceptance/README.md). Không dùng các số liệu cũ trong phần dưới làm kết quả mới.

Ngày 03/11/2026, múi giờ Asia/Ho_Chi_Minh. Kiểm tra trên source đang làm việc ở feat/bt2-setup, không phải bản release/tag. Working tree đã có nhiều thay đổi của người dùng trước lượt kiểm tra. Không commit/push/merge/tag; không sửa source ứng dụng hoặc migration trong lượt này.

**Kết luận:** có đủ bằng chứng để trình diễn luồng L8 trên môi trường test riêng: gửi lời mời mô phỏng → khách phản hồi bất biến → MySQL → tra cứu/thống kê theo quyền → tải XLSX. Chưa đủ để kết luận đạt toàn bộ rubric/NFR hoặc hoàn tất mọi bước nghiệm thu thủ công. Không thay nghiệp vụ BT1 để làm S1 full CRUD.

## Môi trường và cấu hình

- Windows/PowerShell5.1; Temurin21.0.12.1, Maven Wrapper3.10.0, Spring Boot4.1.2.
- Node24.19.0, npm11.17.0, React19.3.1, Vite8.3.5, Vitest5.1.0, Playwright1.64.1/Chromium.
- MySQL8.0.46 test mới tại127.0.0.1:3307/smartcrm_l8_test; backend8080; frontend127.0.0.1:5173.
- MySQL80 đang chạy3306. Không có credential DB thật được cung cấp, không đọc/ghi smartcrm_l8; trạng thái Flyway của DB thật còn BLOCKED.
- Secret được sinh riêng trong process test, bootstrap chỉ trên schema test mới; scheduler tắt khi demo browser. Test scheduler/reminder chạy trong integration suite với fixture và gateway mock.
- DB test browser cuối được giữ tại đường dẫn trong [test-environment.json](evidence/web-acceptance/test-environment.json), server đã shutdown. Các datadir test không bị xóa.
- Sau kiểm tra, tiến trình backend/Vite/MySQL test đã dừng; MySQL80 giữ nguyên hoạt động.

## Kết quả tự động

| Bộ chạy | Tổng | PASS | FAIL/error | SKIPPED | Build |
|---|---:|---:|---:|---:|---|
| scripts/test.ps1, đầy đủ backend | 46 | 46 | 0 | 0 | SUCCESS |
| npm test, Vitest 2files | 17 | 17 | 0 | 0 | Không áp dụng |
| npm run build | Không áp dụng | Không áp dụng | 0 | Không áp dụng | PASS,17modules |
| npm run test:e2e, test có sẵn | 1 | 1 | 0 | 0 | PASS |
| browser-check.mjs bổ sung | Assertions, không phải suite test count | PASS | 0 | Không áp dụng | Không áp dụng |

Tổng suite có cấu hình repository: **64 tests PASS, 0FAIL/error, 0SKIPPED**. Không cộng assertions của runner thành tests. Lượt E2E cuối 1passed/4.0s; thời gian có thể thay đổi ở lượt sau.

Evidence: [backend-tests.txt](evidence/web-acceptance/backend-tests.txt), [frontend-tests.txt](evidence/web-acceptance/frontend-tests.txt), [frontend-build.txt](evidence/web-acceptance/frontend-build.txt), [e2e.log](evidence/web-acceptance/e2e.log), [browser-run.log](evidence/web-acceptance/browser-run.log). Báo cáo từng class Surefire TXT cùng thư mục cung cấp count và thời gian. Frontend unit tests mock HTTP; chúng không được tính là MySQL evidence.

## Khởi động và tích hợp thật

[Backend startup](evidence/web-acceptance/backend-startup.log) chứng minh Flyway áp dụng V1–V5 vào schema rỗng, Hikari kết nối MySQL8.0.46, Tomcat8080 và Started BackendApplication. [Frontend startup](evidence/web-acceptance/frontend-startup.log) xác nhận Vite ready tại127.0.0.1:5173. Không coi GET /404 là lỗi.

Các API nghiệp vụ thật đã được gọi qua frontend Vite proxy; E2E tạo ticket/lời mời mới bằng fixture và API create/send, browser gửi response, các vai trò đọc báo cáo/danh sách và download. Không có uncaught pageerror trong runner bổ sung. Website tự quản lý History API; không có dependency React Router.

[MySQL trước](evidence/web-acceptance/mysql-before.txt):0invitation,0response. [MySQL sau](evidence/web-acceptance/mysql-after.txt):8invitation,6response (hai điểm5, mỗi điểm1–4 có1); một invitation hết hạn chưa response, một CHO_GUI từ race create. [Score check](evidence/web-acceptance/score-check.txt) xác nhận từng mức và0response mồ côi thử nghiệm. Survey được gửi qua UI có status DA_PHAN_HOI, send_attempts1, score/comment khớp. Truy vấn duplicate không trả dòng.

FK thử chèn ticket_id không tồn tại bị MySQL1452/23000, không có row mới: [foreign-key.txt](evidence/web-acceptance/foreign-key.txt). Kiểm PK/FK/UNIQUE/CHECK từ information_schema có trong mysql-after. device_id không có FK device; không tuyên bố mọi quan hệ reference đều được FK bảo vệ.

Ảnh/báo cáo và XLSX được chụp/tải trước khi bổ sung các điểm1–4: snapshot lúc đó chỉ có2responses điểm5. [Danh sách API snapshot](evidence/web-acceptance/api-response-list.json), [XLSX](evidence/web-acceptance/responses.xlsx) và [nội dung đã parse](evidence/web-acceptance/xlsx-inspection.txt) khớp2rows này. Không dùng snapshot cuối6responses để kết luận file export2rows sai. File có7header đúng hợp đồng và phone090****001. ZIP/XML mở/parse thành công; chưa mở Excel desktop.

## TC01–TC20 và mức chứng minh

Chi tiết input, từng bước, expected, actual, evidence và SQL ở [test cases](web-acceptance-test-cases.md).

| TC | Đã thực thi / kết quả | Phần chưa thực thi đầy đủ |
|---|---|---|
| 01 | PASS browser login manager101/marketing/director, role navigation | manager102 browser |
| 02 | PASS API sai password401 | BLOCKED thao tác nhập sai trên UI/ảnh riêng |
| 03 | PASS create201, MySQL, form thật | Không có UI tạo, đúng ranh giới L2/L4 |
| 04 | PASS404 TICKET_NOT_FOUND | Không áp dụng UI tạo |
| 05 | PASS422 TICKET_NOT_CLOSED | Không áp dụng UI tạo |
| 06 | PASS409trùng; race ticket mới201/409, mộtrow | Không phải100CCU |
| 07 | PASS runtime SMS/ZALO giả lập, service idempotency | Không gửi SMS/Zalo thật; không có bảng audit từng tin |
| 08 | PASS integration retry3/5phút, metadata commit, phone sai | BLOCKED chờ thực5phút/HTTP502 trực tiếp; gateway được mock trong suite |
| 09 | PASS integration nhắc72h dùng fixture73h, once-only/retry; scheduler tick2 | BLOCKED quan sát scheduler chạy đủ72h ngoài đời |
| 10 | PASS form public thật,360px và4viewport yêu cầu | — |
| 11 | PASS UI5, required; API1–4/invalid400; MySQL đủ1–5 | BLOCKED gửi từng1–4 bằng clickUI |
| 12 | PASS reload/POST409, không overwrite; integration2submit/rollback | Chưa100requests |
| 13 | PASS API404/410, integration expiry | BLOCKED ảnhUI riêng cho404/410 |
| 14 | PASS bảng browser thật; integration kiểmAVG/rate/distribution/null | Snapshot cuối thêm4rows chưa được chụp lại dashboard |
| 15 | PASS integration lọc/page/date và UI unit tests | BLOCKED pagination browser thật với>20rows |
| 16 | PASS download,ZIP/XML/7header/2rows snapshot/phoneche | BLOCKED mở Excel desktop, dữ liệu10.001rows |
| 17 | PASS crosscenter403, director403, full role matrix backend | Chưa pentest ngoài phạm vi |
| 18 | PASS directURL/F5 các route và session hiện hành | BLOCKED Back/Forward/đóngtab/chờTTL ngoài đời |
| 19 | PASS HTTP401/403; integration expired/tampered/disabledJWT; public11→429 | Chưa kiểm nhiều instance/proxy |
| 20 | PASS loading API thật delay; mockedempty/error, retry API thật; success thật | BLOCKED ảnh tắt hẳn backend; mock không chứng minh DB empty/failure |

Không chuyển các phần BLOCKED này thành PASS toàn diện. Chưa có lỗi ứng dụng tái hiện trong các phép thử đã chạy. Không có source fix hoặc regression test mới cho lỗi sản phẩm.

## Giao diện

32ảnh trong thư mục evidence. Form khảo sát kiểm1920×1080,1366×768,390×844,375×667; E2E repo thêm360×800. Manager:3routes; Marketing:responses; Director:center — mỗi trường hợp kiểm4viewport. 20kết quả layout nội bộ trong [browser-checks.json](evidence/web-acceptance/browser-checks.json), không overflow toàn trang; form cũng kiểm4viewport. Bảng mobile cuộn trong vùng table-scroll, không ép toàn bảng vào375px.

Đã xem ảnh form390 và danh sách375: chữ Việt render đúng, form/menu/nút nhìn được; bảng có vùng cuộn riêng. Chưa đo WCAG contrast hoặc kiểm mọi nút tại mọi viewport. Không tuyên bố đạt usability5người.

- [Loading](evidence/web-acceptance/ui-loading-delayed-real-api.png): delay1.8s rồi tiếp tục API thật.
- [Empty](evidence/web-acceptance/ui-empty-mocked.png): route mock200/items[].
- [Error](evidence/web-acceptance/ui-error-mocked-503.png): route mock503, sau bỏ mock bấm Thử lại tải dữ liệu thật.
- [Success](evidence/web-acceptance/survey-success.png): response thật được lưu.

Không có ảnh màn tạo/gửi lời mời vì project không có màn hình đó; evidence tương ứng là API/log/SQL.

## Bảo mật, toàn vẹn và hiệu năng

[Log API đã che token](evidence/web-acceptance/browser-checks.json): missing/wrong Service Key401, Bearer sai/thiếu401, manager101 center102→403; lỗi nghiệp vụ404/409/410/422; inputscore0/comment1001→400. Full report role matrix5endpoint, JWT chữ ký sai/hết hạn/accountdisabled có integration tests. Public rate limit gọi11trong1phút trả429 được suite HTTP kiểm tra. Survey token ngẫu nhiên32byte/base64url43kýtự theo source; rate limiter theo IP trong bộ nhớ instance, không suy rộng cho distributed deployment.

33 lời gọi API bổ sung ghi latency end-to-end đơn lẻ trên localhost **4–115ms** ở lượt cuối; GET response list22ms, submit score1–4 lần lượt11/12/11/12ms. Không có percentile/tải đồng thời, không điều kiện4G, không tập10.000responses: chưa đạt chứng minh NFR01/02/03. Thời gian click-to-render UI không được đo bằng các latency này.

NFR05 chỉ có2submit đồng thời tại service,2create API đồng thời; chưa100requests→1/99. Public limiter10/phút còn cần thiết kế phép thử nếu đo100submit đồng thời theo SRS mà không bị429. NFR06 chưa500phiếu/99%gửi≤5phút. NFR07 chưa5người/30s/3chạm. NFR08 đã thấy response vừa gửi xuất hiện ngay trong list/stats theo E2E, không suy ra SLO tải lớn.

Không lưu password/secret/JWT thực vào ảnh/log công bố. File artifact có dữ liệu tổng hợp; response-list/XLSX phone đã che. Trace E2E thô không được đưa vào evidence. Chưa thực hiện secret scan toàn lịch sử Git/pentest, không kết luận bảo mật tuyệt đối.

## Sự cố runner và hồi quy

Lượt đầu bị dừng do PowerShell5.1 ErrorActionPreference Stop chuyển stderr warning NO_COLOR/FORCE_COLOR thành NativeCommandError. [e2e-attempt1.log](evidence/web-acceptance/e2e-attempt1.log) giữ lượt chưa hoàn tất; không được tính thành PASS. Sửa cách thu native output bằng cmd /c “... 2>&1” trong script nghiệm thu. Không đổi dependency/expected status/mã sản phẩm để bỏ qua lỗi. Các lượt sau E2E và browser assertions PASS; backend/frontend suite và build đã chạy hồi quy đầy đủ. Warning Maven metadata401 từ feed bổ sung và open-in-view không ngăn suite thành công; không chứa bằng chứng lỗi API nghiệp vụ.

## Khoảng trống rubric và sẵn sàng nộp

- C1: cách chạy Windows và kịch bản test đã xác minh trên máy này; chưa freshclone/tag/máy khác của lượt nghiệm thu này.
- C2/C3/C4/C7: source phân lớp và tests đúng/biên/sai có bằng chứng;64testsPASS; không bỏ tests.
- C5/S3: Flyway V1–V5 freshDB thành công; context sau báo up-to-date, duplicate/scheduler có tests. Chưa walkthrough khởi động lại demo và đối chiếu thủ công toàn bộ dữ liệu.
- C6: có wrapper/lockfile/envexample, nhưng nhiều file hiện vẫn chưa commit từ trước; chưa đạt phần “đã commit”; không tự commit thay người dùng.
- S1: fullCRUD2entity còn khoảng trống, chờ giảng viên chấp nhận cách đánh giá phù hợp response bất biến/reference chỉ đọc. Không xây CRUD ngoài BT1.
- S2/S4: services và nhiều màn API thật có minh chứng runtime; UI states mô phỏng được ghi provenance rõ.
- NFR tải/usability, mở Excel desktop, tài liệu phê duyệt giả địnhBT1, Git release/tag và nộpE-learning còn cần hoàn thiện/duyệt.

**Sẵn sàng demo chức năng trên DB test; chưa xác nhận nghiệm thu BT2 toàn bộ.** Người dùng tiếp tục theo [web-run-guide](web-run-guide.md), thực hiện các bước BLOCKED trong bảng trên và ghi người/ngày/kết quả thật trước khi đưa kết luận cuối vào báo cáo.

