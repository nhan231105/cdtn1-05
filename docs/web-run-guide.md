# Hướng dẫn chạy và trải nghiệm Smart CRM L8 trên Windows 11

Ngày xác minh: 03/11/2026. Repository D:\cdtn1\bt2\cdtn1-05-bt2, branch feat/bt2-setup. Đọc cùng [test cases](web-acceptance-test-cases.md), [kết quả thực thi](web-acceptance-results.md). Chỉ dùng dữ liệu tổng hợp trong môi trường test cho các bước ghi dữ liệu dưới đây.

## 1. Cấu hình đã xác minh từ source

| Thành phần | Giá trị / nguồn |
|---|---|
| Backend | backend/pom.xml: Java 21, Spring Boot 4.1.2; backend/.mvn/wrapper: Maven 3.10.0 |
| Runtime máy đã kiểm tra | Temurin 21.0.12.1; Node 24.19.0; npm 11.17.0; MySQL 8.0.46 |
| Cổng backend | application.properties: server.port=8080 |
| JDBC mặc định | jdbc:mysql://127.0.0.1:3306/smartcrm_l8?connectionTimeZone=Asia/Ho_Chi_Minh |
| Bắt buộc | DB_USER, DB_PASSWORD, SERVICE_API_KEY (≥32 ký tự), JWT_SECRET (≥32 byte) |
| Tùy chọn | DB_URL; BOOTSTRAP_ENABLED=false; BOOTSTRAP_PASSWORD trống; SURVEY_PUBLIC_BASE_URL=http://localhost:5173/khao-sat; GATEWAY_SIMULATE_FAILURE=false; SURVEY_EXPIRY_DAYS=7; SURVEY_SCHEDULER_ENABLED=true |
| Bootstrap | Khi bật: password ít nhất 12 ký tự và tối đa 72 byte; chỉ tạo account còn thiếu |
| Frontend | frontend/src/api.js gọi đường dẫn tương đối /api; không có API base URL chứa secret |
| Proxy | vite.config.js: VITE_API_TARGET từ process.env hoặc file env Vite, mặc định http://127.0.0.1:8080 |
| Vite | npm run dev bind 127.0.0.1:5173, strictPort=true; cổng bận thì báo lỗi |
| Database | MySQL ≥8.0.16 để CHECK được thực thi; schema thật smartcrm_l8, schema test smartcrm_l8_test ở 3307 |
| Dependency hỗ trợ | Không cần Docker/Redis/message broker. Scripts cần bộ MySQL server/client cài trên máy |
| Gateway | SMS/Zalo giả lập; log “Simulated message accepted”, không chứng minh gửi tin thật |
| Routing | App.jsx dùng History API/popstate tự quản lý, không cài React Router |

Spring Boot không tự nạp .env ở root. Không đưa DB password, JWT secret hoặc service key vào VITE_*.

## 2. Kiểm tra trước khi chạy

Mở IntelliJ IDEA → Open thư mục repository → Terminal, chọn PowerShell:

~~~powershell
Set-Location D:\cdtn1\bt2\cdtn1-05-bt2
git branch --show-current
git status --short
java -version
node --version
npm --version
Get-Service MySQL80
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
  Where-Object LocalPort -in 3306,3307,8080,5173 |
  Select-Object LocalAddress,LocalPort,OwningProcess
Test-Path backend\mvnw.cmd
Test-Path 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
~~~

Kỳ vọng branch feat/bt2-setup, JDK21, Node ≥22.12 (máy đã dùng Node24). Nếu thiếu JDK: cài JDK21 và đặt JAVA_HOME, IntelliJ File → Project Structure → Project SDK chọn 21; Settings → Build Tools → Maven → Runner chọn JDK21. Nếu thiếu Node/MySQL: cài Node24 LTS và MySQL8 server/client theo bộ cài của nhà cung cấp; mở terminal mới sau khi cập nhật PATH. Không cần Maven hệ thống.

MySQL80 đang Running ở 3306 khi kiểm tra. Nếu Stopped, Services (services.msc) → MySQL80 → Start hoặc PowerShell quản trị Start-Service MySQL80. Nếu cổng bị chiếm, xác định PID bằng Get-Process -Id; dừng đúng tiến trình bạn sở hữu, không kill server người khác. Bộ backend tests từ scripts/test.ps1 sẽ từ chối nếu 3307 đã có server; hãy dừng server test của bạn trước khi chạy suite này.

## 3. MySQL Workbench: kiểm tra dữ liệu hiện có, chỉ đọc

Mở Workbench → dấu + cạnh MySQL Connections → Standard TCP/IP; hostname 127.0.0.1, port 3306, user của bạn → Test Connection, nhập password tại hộp thoại. Không gửi password cho người khác.

Mở SQL tab:

~~~sql
SHOW DATABASES LIKE 'smartcrm_l8';
USE smartcrm_l8;
SELECT DATABASE(), VERSION();
SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank;
SHOW TABLES;
SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA=DATABASE();
SELECT ticket_id,ticket_code,status,center_id FROM ticket ORDER BY ticket_id;
SELECT invitation_id,ticket_id,status,send_attempts,sent_at,expires_at FROM survey_invitation;
SELECT response_id,ticket_id,score,responded_at FROM survey_response;
SELECT username,role,center_id,enabled FROM app_user;
~~~

Không SELECT password_hash/token khi chụp minh chứng. Mở từng bảng → Table Inspector để xem PK/FK/index. V1 tạo bảng và ràng buộc, V2 seed demo, V3 reference label/app_user, V4 last_attempt_at, V5 metadata retry reminder. Cả 5 đã chạy thành công trên database test trong lượt này; database thật 3306 chưa được truy vấn vì không có credential được cung cấp. Nếu bảng history chưa có, chưa được coi là migrate thành công. Không dùng repair/clean để che mismatch checksum.

Các bảng thực tế: service_center, customer, technician, ticket, survey_invitation, survey_response, device, app_user và flyway_schema_history. device_id của ticket chưa có FK sang device; reference thiếu được hiển thị bằng định danh “Thiết bị #...”. Không giả định có ticket_status_log trong schema rút gọn này.

## 4. Khởi động môi trường demo an toàn, database test riêng

Trước hết dừng lượt kiểm thử tự động nếu đang chạy. Trong terminal A ở root:

~~~powershell
$demoDatabase = & .\scripts\start-test-db.ps1
$demoDatabase | Select-Object ProcessId,Port,DataDirectory
~~~

Script tạo datadir mới trong TEMP, bind loopback 3307, schema smartcrm_l8_test, root không password chỉ trong server test tạm. Không dùng cấu hình này cho server thật. Không xóa datadir; giữ lại để kiểm tra sau demo.

Tạo file local .env nếu chưa có:

~~~powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
~~~

Trong editor đặt DB_URL trỏ jdbc:mysql://127.0.0.1:3307/smartcrm_l8_test?connectionTimeZone=Asia/Ho_Chi_Minh, DB_USER=root, DB_PASSWORD để trống cho server test này. Đặt BOOTSTRAP_ENABLED=true, BOOTSTRAP_PASSWORD là password test bạn tự chọn (12–72 byte), SURVEY_SCHEDULER_ENABLED=false, SURVEY_PUBLIC_BASE_URL=http://127.0.0.1:5173/khao-sat. Điền SERVICE_API_KEY và JWT_SECRET độc lập, đủ độ dài. Có thể sinh giá trị và gán vào clipboard bằng RNG rồi dán trong editor (không in ra terminal):

~~~powershell
$secretBytes = New-Object byte[] 48
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
$generator.GetBytes($secretBytes)
Set-Clipboard ([Convert]::ToBase64String($secretBytes))
$generator.Dispose()
~~~

Chạy lại để tạo secret thứ hai; không dùng chung hai secret. Không lưu ảnh editor hay commit .env. Nạp trong terminal A:

~~~powershell
Get-Content .env | ForEach-Object {
    $line = $_.Trim()
    if ($line -and !$line.StartsWith('#')) {
        $pair = $line -split '=',2
        if ($pair.Length -eq 2) {
            Set-Item -LiteralPath ('Env:'+$pair[0].Trim()) -Value $pair[1]
        }
    }
}
Set-Location backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="
~~~

Tham số password rỗng chỉ dành cho MySQL tạm initialize-insecure. Với smartcrm_l8 thật, dùng DB_USER/DB_PASSWORD hợp lệ, DB_URL 3306 và chỉ .\mvnw.cmd spring-boot:run; không áp dụng password rỗng. Chạy backend trên DB thật có thể tự áp dụng migration còn thiếu, scheduler mặc định tạo/gửi lời mời; vì thế walkthrough này sử dụng DB test.

Kỳ vọng log HikariPool Start completed, Flyway Successfully applied 5 migrations / schema version 5, Tomcat started on port 8080, Started BackendApplication; không có exception cấu hình secret. GET http://localhost:8080/ có thể 404 và vẫn bình thường. GET /api/auth/me chưa login phải 401.

IntelliJ chạy trực tiếp: mở backend/pom.xml → Load Maven; Run → Edit Configurations → Spring Boot hoặc Application → BackendApplication; JRE21, main class com.mekongmobile.smartcrm.BackendApplication, Environment variables nhập cùng cấu hình local, Program arguments --spring.datasource.password= chỉ cho DB test tạm. Dừng backend terminal trước để tránh tranh cổng. Đừng chụp hộp thoại chứa secret.

## 5. Frontend: terminal B

~~~powershell
Set-Location D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npm ci
npm run dev
~~~

Mở đúng URL Vite hiển thị: http://127.0.0.1:5173/. F12 → Console không có uncaught JavaScript error; Network có JS/CSS 200, login API đúng /api. Dùng 127.0.0.1 thống nhất để tránh IPv6 localhost trên một số máy. Nếu backend ở cổng khác: đặt VITE_API_TARGET trước npm run dev. Bản dist khi host production cần fallback các URL SPA về index.html và reverse proxy /api; Vite preview không thay cấu hình proxy production.

## 6. Chuẩn bị tài khoản và trải nghiệm từng vai trò

Bootstrap trên DB mới tạo các tài khoản dưới đây với password bạn vừa đặt; không có password mặc định và không có đăng ký public. Account đã tồn tại không được đổi password bằng bootstrap. Tắt BOOTSTRAP_ENABLED sau lần đầu (file .env và cấu hình lần chạy sau).

| Username demo từ V2 | Role | Màn hình được dùng |
|---|---|---|
| manager101 | QL_TRUNG_TAM / center101 | /thong-ke/ky-thuat-vien, /phan-hoi, /thong-ke/ty-le |
| manager102 | QL_TRUNG_TAM / center102 | Cùng chức năng, chỉ center102 |
| marketing | MARKETING | /phan-hoi, lọc toàn công ty, xuất Excel |
| director | BAN_GIAM_DOC | /thong-ke/trung-tam |
| Không đăng nhập | Khách hàng | /khao-sat/{token}, gửi phản hồi một lần |

Không có role ADMIN hay NHAN_VIEN trong app_user CHECK. Nhân viên không được dùng JWT để gọi tạo/gửi lời mời; EP1/EP2 thuộc hệ thống L2/L4 và scheduler. Không có màn hình tạo/gửi khảo sát nội bộ để bấm. Không thêm CRUD cho các bảng reference/phản hồi.

Tại /, nhập manager101 và password tự cấu hình → Đăng nhập → “Chất lượng từ đội ngũ của bạn”. Chọn Từ ngày/Đến ngày thuộc tháng hiện tại, bấm Áp dụng. Ban đầu phản hồi trống, CSAT hiển thị “—”. Bấm Danh sách phản hồi rồi Tỉ lệ phản hồi. Trung tâm bị khóa đúng center101; không có nút Xuất Excel. Đăng xuất.

Đăng nhập marketing → “Lắng nghe từng trải nghiệm”; có Trung tâm/Kỹ thuật viên/Điểm từ/Điểm đến, Dòng/trang, Trước/Sau và Xuất Excel. Đăng xuất, đăng nhập director → “Bức tranh hài lòng toàn công ty”, bảng theo tháng/phân bố điểm và xu hướng. Director không có link danh sách phản hồi.

JWT lưu sessionStorage trong tab, TTL một giờ; F5 giữ session trong tab, đóng tab có thể mất session; logout xóa session. Truy cập URL bị cấm hiển thị “Không có quyền truy cập”, backend vẫn kiểm role/scope.

## 7. Tạo link và chứng minh frontend → API → MySQL

Terminal C ở root: nạp .env bằng cùng đoạn loader của mục 4. Backend đã chạy trên DB test, scheduler false; các ticket V2: 401/402/403 DA_DONG, 404 DANG_XU_LY. Dùng ticket chưa có lời mời:

~~~powershell
$headers = @{ 'X-Service-Key' = $env:SERVICE_API_KEY }
$invite = Invoke-RestMethod -Method Post -Uri http://127.0.0.1:8080/api/survey-invitations -Headers $headers -ContentType application/json -Body '{"ticket_id":401}'
$null = Invoke-RestMethod -Method Post -Uri ("http://127.0.0.1:8080/api/survey-invitations/"+$invite.invitation_id+"/send") -Headers $headers -ContentType application/json -Body '{"channel":"SMS"}'
Start-Process $invite.survey_url
~~~

Đây là gateway mô phỏng. Không in $headers, JWT hoặc URL chứa token vào minh chứng. Nếu 409 vì phiếu đã có lời mời, dùng fixture mới, không xóa lời mời hay phản hồi. Có thể dùng frontend/e2e/prepare-fixture.ps1 (DB test suffix _test, port khác3306):

~~~powershell
$env:E2E_SERVICE_KEY = $env:SERVICE_API_KEY
$fixture = & .\frontend\e2e\prepare-fixture.ps1
Start-Process ("http://127.0.0.1:5173/khao-sat/"+$fixture.Token)
~~~

Trình duyệt: kiểm mã phiếu/trung tâm/định danh thiết bị → chưa chọn điểm bấm GỬI PHẢN HỒI phải có validation → chọn 5 điểm → nhập nhận xét tổng hợp → GỬI PHẢN HỒI. Phải hiện “Đánh giá đã được ghi nhận”; F5 báo đã gửi, không có form gửi lại.

Workbench tạo connection thứ hai 127.0.0.1:3307/root, password trống cho server test này, schema smartcrm_l8_test. Chạy SQL trong evidence/web-acceptance/verify.sql trước và sau thao tác. Phiếu có đúng một survey_response, score/comment khớp, invitation DA_PHAN_HOI. Marketing đăng nhập /phan-hoi, lọc ngày hiện tại thấy dòng mới và số che; manager101 thấy còn manager102 không thấy. Director xem phân bố/CSAT. Đối chiếu CSAT = AVG(score), satisfied_rate = số score≥4 / số response; response_rate dùng cohort tháng gửi thành công, không chia hai tập theo ngày độc lập.

Marketing → Xuất Excel → mở file trong Excel/LibreOffice; kiểm 7 cột và dữ liệu khớp bộ lọc, SĐT đã che. File phản hồi không có chức năng sửa/xóa từ website.

## 8. Danh mục API thật

| Method | URL | Xác thực |
|---|---|---|
| POST | /api/auth/login | username/password |
| GET | /api/auth/me | JWT |
| POST | /api/survey-invitations | X-Service-Key |
| POST | /api/survey-invitations/{invitation_id}/send | X-Service-Key |
| GET | /api/surveys/{token} | Public token |
| POST | /api/surveys/{token}/responses | Public token |
| GET | /api/survey-stats/by-technician | QL_TRUNG_TAM |
| GET | /api/survey-stats/by-center | BAN_GIAM_DOC |
| GET | /api/survey-stats/response-rate | QL_TRUNG_TAM |
| GET | /api/survey-responses | QL_TRUNG_TAM hoặc MARKETING |
| GET | /api/survey-responses/export | MARKETING |
| GET | /api/reference/centers | JWT, manager chỉ center của mình |
| GET | /api/reference/technicians | JWT và scope |

Report query: center_id, technician_id, from, to, score_min, score_max; page từ1, size20 mặc định, max100; by-center group_by=month. Đối chiếu api-contract.md cho từng endpoint; không phải mọi tham số đều có ý nghĩa cho mọi báo cáo. Scheduler nhắc sau72h không có endpoint public.

## 9. Chạy kiểm thử lại

Dừng backend/Vite/demo MySQL3307 trước khi chạy bộ tự động riêng. Tại root:

~~~powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
Set-Location frontend
npm ci
npm test
npm run build
~~~

Backend script tạo MySQL mới, chạy toàn suite và shutdown server riêng trong finally, giữ datadir. Không bỏ qua integration tests. Không chạy tests vào smartcrm_l8.

E2E theo cấu hình repo: dựng server demo test mới như trên, bootstrap true, scheduler false; terminal C đã nạp env:

~~~powershell
Set-Location D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npx playwright install chromium
$env:E2E_SERVICE_KEY = $env:SERVICE_API_KEY
$fixture = & .\e2e\prepare-fixture.ps1
$env:E2E_SURVEY_TOKEN = $fixture.Token
$env:E2E_TICKET_CODE = $fixture.TicketCode
$env:E2E_PASSWORD = $env:BOOTSTRAP_PASSWORD
npm run test:e2e
~~~

Playwright tự khởi động Vite hoặc dùng Vite đã chạy; thiếu token/password phải fail. Mỗi lần dùng lời mời mới vì response bất biến. Browser screenshots trong frontend/test-results; trace lỗi có thể chứa token/request headers, không chia sẻ trace thô.

Kịch bản nghiệm thu bổ sung đã lưu ở docs/evidence/web-acceptance/run-checks.ps1: tự tạo DB riêng, sinh secret chỉ trong process, start backend/Vite, chạy E2E repo + browser-check.mjs, lưu ảnh/log/SQL/XLSX, shutdown. Chỉ chạy khi3307/8080/5173 rảnh; không chạy song song hai lượt. Script ghi đè evidence lượt trước, sao chép evidence nếu cần lưu lịch sử. Có một lần chờ60s để tuân thủ rate limit. Scripts không cần secret do người dùng cấp cho lượt tự động này.

## 10. Dừng và khắc phục

Dừng Vite Ctrl+C trong B; dừng backend Ctrl+C trong A, trở về root. Shutdown đúng MySQL test vừa tạo:

~~~powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe' --no-defaults --host=127.0.0.1 --port=3307 --user=root shutdown
~~~

Không Stop-Service MySQL80 để dừng DB test. Datadir test được giữ; không reset smartcrm_l8.

| Triệu chứng | Kiểm tra / xử lý |
|---|---|
| Placeholder DB_*, JWT/service key error | Nạp env đúng terminal backend, secret đủ dài; không echo secret |
| Access denied / Communications link failure | Đúng port/schema/user; server test3307 khác MySQL80/3306; password rỗng chỉ test và truyền CLI rõ |
| Flyway checksum mismatch | Dừng, xác định migration khác bản đã áp dụng; không sửa V1/V2 hoặc repair tự động |
| 8080/5173 bận | Xác định PID, dừng đúng app của bạn; Vite strictPort không tự chuyển5174 |
| White screen / request fail | F12 Console/Network; kiểm JS/CSS, proxy target, backend, không đưa key vào frontend |
| 401 login | Account đã bootstrap chưa, đúng password đã tự đặt; bootstrap không reset existing account |
| 403 | Kiểm role và center, không cố mở dữ liệu trung tâm khác |
| 409 lời mời/phản hồi | Chống trùng đúng thiết kế, tạo fixture mới |
| 410/429 | Link mới còn hạn / đợi cửa sổ phút hết, không tắt limiter |
| Không thấy dữ liệu mới | Bộ lọc ngày Việt Nam và center; Làm mới số liệu / Áp dụng |
| Error UI | Khôi phục backend → Thử lại; form giữ dữ liệu ở lỗi tạm thời theo test |
| PowerShell native stderr warning | Thu stdout/stderr qua cmd /c “... 2>&1” trong runner; không coi warning là test FAIL nếu suite chưa hoàn tất |

