# Hướng dẫn chạy Smart CRM L8 với MySQL thật trên máy

Dành cho Windows 11, IntelliJ IDEA và PowerShell.
Repository: D:\cdtn1\bt2\cdtn1-05-bt2.

**Luồng chính của tài liệu này dùng MySQL80 tại 127.0.0.1:3306, database smartcrm_l8. Không chạy scripts/start-test-db.ps1 cho luồng này.**

Project dùng MySQL qua MySQL Connector/J. Microsoft SQL Server là hệ quản trị khác; không thể chuyển sang SQL Server bằng cách chỉ sửa cổng hoặc tên database.

**Thứ tự đọc:** file này để khởi động; [web-demo-walkthrough.md](web-demo-walkthrough.md) để thao tác demo và kiểm thử từng luồng; [web-acceptance-test-cases.md](web-acceptance-test-cases.md) để tra TC01–TC20. Không cần tạo lại secret hoặc tài khoản mỗi lần mở máy.

## 0. Mỗi lần mở máy: dùng terminal nào?

1. Mở IntelliJ → Open `D:\cdtn1\bt2\cdtn1-05-bt2` → Alt + F12 → PowerShell.
2. Gọi terminal này là **A — Backend**. Nạp `.env`, chọn Java và chạy Maven trong cùng terminal A.
3. Mở dấu **+** để tạo **B — Frontend**. B không tự nhận biến đã nạp ở A.
4. Mở MySQL Workbench để chạy SQL. Không dán câu SQL vào PowerShell.
5. Chỉ chép nội dung trong khối lệnh, không chép dấu nhắc `PS D:\...>` hoặc các dòng kết quả.

Nếu đã có `.env` đúng và tài khoản đăng nhập hoạt động, giữ nguyên cấu hình và dùng khối lệnh khởi động lại ở mục 13. Lần đầu làm tuần tự mục 1–10.

## 1. Mở project và kiểm tra MySQL80

Mở IntelliJ IDEA → Open repository → Alt + F12 → PowerShell:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2
Get-Service MySQL80
Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalPort,OwningProcess
~~~

Kết quả đúng: service Running, cổng 3306 Listen.

Nếu service Stopped: mở services.msc → MySQL80 → Start. Không cần khởi động MySQL test ở3307.

## 2. Kiểm tra kết nối bằng MySQL Workbench

1. Mở MySQL Workbench.
2. Mở connection MySQL hiện có, hoặc tạo Standard TCP/IP:
   - Hostname: 127.0.0.1.
   - Port: 3306.
   - Username: tài khoản MySQL bạn đã cấu hình.
3. Bấm Test Connection, nhập mật khẩu MySQL của tài khoản đó.
4. Sau khi kết nối, chạy:

~~~sql
SELECT VERSION(), CURRENT_USER();
SHOW DATABASES LIKE 'smartcrm_l8';
~~~

DB_USER và DB_PASSWORD trong .env phải là tài khoản MySQL kết nối được ở bước này. Không tự mặc định là root hoặc smartcrm nếu chưa kiểm tra. Nếu không biết mật khẩu, kiểm tra cấu hình/ghi chú cài MySQL của bạn; không đặt lại mật khẩu hay xóa dữ liệu để tiếp tục.

Trên máy bạn, Workbench đã dùng tài khoản `root`. Nếu vẫn dùng connection đó, điền `DB_USER=root`, **không điền `root@localhost`**. Kết quả `CURRENT_USER()` có thể là `root@localhost`: `root` là username, `localhost` là host, không chép cả chuỗi vào DB_USER. Lỗi `Access denied for user 'root@localhost'@'localhost'` cho thấy username đã bị điền sai kiểu này.

Nếu database đã có dữ liệu, dùng Workbench → Server → Data Export → chọn smartcrm_l8 → Export to Self-Contained File → Start Export. Lưu backup ngoài repository trước khi khởi động backend lần đầu với cấu hình mới.

Nếu schema chưa tồn tại, tạo schema mới bằng tài khoản có quyền:

~~~sql
CREATE DATABASE IF NOT EXISTS smartcrm_l8
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
~~~

Không DROP/reset schema đang có. Chưa cần tự tạo bảng: Flyway quản lý schema khi backend chạy. DB_USER cần quyền cho ứng dụng đọc/ghi nghiệp vụ và thực hiện migration còn thiếu trong schema này.

Nếu schema đã tồn tại:

~~~sql
USE smartcrm_l8;
SHOW TABLES;
~~~

Chỉ khi bảng flyway_schema_history đã có, chạy:

~~~sql
SELECT version,description,success
FROM flyway_schema_history ORDER BY installed_rank;
~~~

Khởi động backend sẽ tự áp dụng migration chưa chạy V1–V5, có thể thêm bảng/cột/dữ liệu seed theo migration. Không sửa migration V1/V2, không chạy clean/repair để bỏ qua lỗi checksum. Nếu gặp schema không có history nhưng đã có bảng nghiệp vụ, dừng để kiểm tra thay vì baseline tự động.

## 3. Chuẩn bị .env ở root repository

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2
if (!(Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
~~~

Điền cấu hình sau. Các chữ DIEN_... là hướng dẫn thay thế, không phải credential có sẵn:

~~~dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/smartcrm_l8?connectionTimeZone=Asia/Ho_Chi_Minh
DB_USER=DIEN_USERNAME_MYSQL_DA_TEST_CONNECTION
DB_PASSWORD=DIEN_MAT_KHAU_CUA_USERNAME_MYSQL

SERVICE_API_KEY=
JWT_SECRET=

BOOTSTRAP_ENABLED=false
BOOTSTRAP_PASSWORD=

SURVEY_PUBLIC_BASE_URL=http://127.0.0.1:5173/khao-sat
SURVEY_SCHEDULER_ENABLED=false
SURVEY_EXPIRY_DAYS=7
GATEWAY_SIMULATE_FAILURE=false
~~~

Phân biệt các giá trị:

| Biến | Nguồn giá trị |
|---|---|
| DB_USER / DB_PASSWORD | Tài khoản của MySQL80, đã xác nhận trong Workbench |
| SERVICE_API_KEY | Bạn sinh ngẫu nhiên, ít nhất32ký tự, cho API nội bộ |
| JWT_SECRET | Bạn sinh ngẫu nhiên riêng, ít nhất32byte, để ký JWT |
| BOOTSTRAP_PASSWORD | Bạn tự đặt để tạo account website mới khi bật bootstrap, ít nhất12ký tự, tối đa72byte |

Mật khẩu MySQL không phải mật khẩu website.

Scheduler để false trong lúc kiểm tra từng bước nhằm kiểm soát việc tự tạo/gửi lời mời trên dữ liệu hiện có. Khi đã muốn bật luồng tự động, đặt true, nạp env và restart backend. Scheduler sẽ thăm dò ticket đã đóng, tạo lời mời còn thiếu và xử lý gửi/retry/reminder.

## 4. Sinh SERVICE_API_KEY và JWT_SECRET

Chạy trong PowerShell:

~~~powershell
$bytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
Set-Clipboard ([Convert]::ToBase64String($bytes))
$rng.Dispose()
~~~

Giá trị được chép vào clipboard, không hiện trên terminal.

1. Trong Notepad đặt con trỏ ngay sau SERVICE_API_KEY= → Ctrl + V.
2. Chạy lại đoạn PowerShell để sinh giá trị khác.
3. Đặt con trỏ sau JWT_SECRET= → Ctrl + V.
4. Giữ nguyên toàn bộ chuỗi, kể cả dấu = ở cuối nếu có.
5. Không bọc giá trị bằng dấu nháy; mỗi biến chỉ có một dòng.

Set-Clipboard chỉ chép vào clipboard, không tự ghi .env hoặc tự đặt biến môi trường.

Nếu secret đã xuất hiện trong ảnh chia sẻ, hãy thay bằng secret mới. Đổi JWT_SECRET khiến JWT đã cấp không còn hợp lệ; người dùng đăng nhập lại. Nếu Service Key được module khác sử dụng, cập nhật đồng bộ cấu hình của module đó.

## 5. Chuẩn bị tài khoản đăng nhập website

Nếu bảng app_user đã có, trong Workbench chạy truy vấn chỉ đọc:

Kiểm tra bảng trước bằng câu sau; bôi đen toàn bộ câu đến hết dấu `;` rồi Run:

~~~sql
SHOW TABLES FROM smartcrm_l8 LIKE 'app_user';
~~~

Nếu không có kết quả, bỏ qua SELECT app_user bên dưới, cấu hình bootstrap rồi tiếp tục khởi động backend để Flyway tạo bảng V3. Không tự CREATE TABLE app_user. Nếu chỉ bôi đen đến `FROM smartcrm_l8`, Workbench sẽ chạy câu thiếu `.app_user` và báo lỗi tên bảng; hãy chạy nguyên câu.

~~~sql
SELECT username,role,center_id,enabled FROM smartcrm_l8.app_user;
~~~

- Nếu đã có account và biết mật khẩu: giữ BOOTSTRAP_ENABLED=false.
- Nếu cần tạo account demo còn thiếu: đặt BOOTSTRAP_ENABLED=true và điền BOOTSTRAP_PASSWORD trong .env bằng mật khẩu bạn tự chọn, ít nhất12ký tự/tối đa72byte.
- Nếu chưa có app_user vì chưa migrate: có thể bật bootstrap để ứng dụng tạo account sau khi Flyway hoàn thành.

Bootstrap tạo marketing, director và manager{center_id} cho các trung tâm đang có. V2 có center101/102 nên database được seed theo dự án sẽ có manager101/manager102. Dùng dữ liệu thực tế từ service_center/app_user để xác nhận, không tự giả định các trung tâm này luôn tồn tại.

Bootstrap không đổi mật khẩu/quyền của account đã tồn tại. Nếu account cũ login không được, đổi BOOTSTRAP_PASSWORD không reset account đó; cần xác định cách khôi phục riêng.

Sau lần tạo thành công, đổi BOOTSTRAP_ENABLED=false cho lần khởi động sau.

## 6. Lưu và nạp .env

Nhấn Ctrl + S trong Notepad, rồi đóng file.

File đã lưu và biến trong terminal là hai nơi khác nhau: sửa Notepad không tự đổi `$env:...`. Sau mỗi lần sửa phải chạy lại đoạn dưới, kể cả khi backend đã chạy trước đó; nếu backend đang chạy, Ctrl + C rồi nạp lại và khởi động lại.

Trong chính terminal sẽ chạy backend:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2

$configPath = 'D:\cdtn1\bt2\cdtn1-05-bt2\.env'
if (!(Test-Path -LiteralPath $configPath)) { throw 'Khong tim thay .env o root repository.' }
Get-Content -LiteralPath $configPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -and !$line.StartsWith('#')) {
        $pair = $line -split '=', 2
        if ($pair.Length -eq 2) {
            [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1], 'Process')
        }
    }
}
~~~

Nạp lại sau mỗi lần sửa .env hoặc mở terminal mới. Spring Boot không tự đọc file .env ở root. Không in file .env hoặc secret để gửi minh chứng.

## 7. Kiểm tra cấu hình database thật

~~~powershell
[pscustomobject]@{
    ServiceKeyReady = (
        ![string]::IsNullOrWhiteSpace($env:SERVICE_API_KEY) -and
        $env:SERVICE_API_KEY.Length -ge 32
    )
    JwtSecretReady = (
        ![string]::IsNullOrWhiteSpace($env:JWT_SECRET) -and
        [Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -ge 32
    )
    DatabaseConfigured = (
        $env:DB_URL -eq 'jdbc:mysql://127.0.0.1:3306/smartcrm_l8?connectionTimeZone=Asia/Ho_Chi_Minh'
    )
    DatabaseUserPresent = (![string]::IsNullOrWhiteSpace($env:DB_USER))
    BootstrapReady = (
        $env:BOOTSTRAP_ENABLED -eq 'false' -or (
            $env:BOOTSTRAP_ENABLED -eq 'true' -and
            ![string]::IsNullOrWhiteSpace($env:BOOTSTRAP_PASSWORD) -and
            $env:BOOTSTRAP_PASSWORD.Length -ge 12 -and
            [Text.Encoding]::UTF8.GetByteCount($env:BOOTSTRAP_PASSWORD) -le 72
        )
    )
}
~~~

Kỳ vọng tất cả True. Kiểm tra này xác nhận định dạng/cấu hình, không thay thế việc Test Connection trong Workbench hay kiểm tra kết nối thực tế khi backend khởi động.

Không dùng tiêu chí TestDatabaseConfigured từ hướng dẫn cũ: bạn đang chủ động dùng database thật3306.

Nếu `BootstrapReady=False`, chạy các kiểm tra không hiển thị mật khẩu:

~~~powershell
[pscustomobject]@{
    EnabledIsTrue = ($env:BOOTSTRAP_ENABLED -eq 'true')
    PasswordLength = ([string]$env:BOOTSTRAP_PASSWORD).Length
    PasswordPresent = (![string]::IsNullOrWhiteSpace($env:BOOTSTRAP_PASSWORD))
    MinimumLengthOK = (([string]$env:BOOTSTRAP_PASSWORD).Length -ge 12)
    MaximumBytesOK = ([Text.Encoding]::UTF8.GetByteCount([string]$env:BOOTSTRAP_PASSWORD) -le 72)
}
~~~

- Nếu đang bật bootstrap: PasswordLength phải từ 12 trở lên, tối đa 72 byte. Mật khẩu 11 ký tự vẫn chưa đủ; đếm bằng lệnh thay vì nhìn ước lượng.
- Nếu PasswordLength là 0: terminal chưa nhận password hoặc file có giá trị rỗng. Lưu file và chạy lại mục 6 trong chính terminal đó.
- Nếu đã có account và chủ động đặt bootstrap=false: không cần password bootstrap; `EnabledIsTrue=False` là bình thường nhưng `BootstrapReady` ở phép kiểm chính phải True.

Đối chiếu file đã lưu với terminal mà không in giá trị:

~~~powershell
$passwordDefinitions = @(Get-Content -LiteralPath 'D:\cdtn1\bt2\cdtn1-05-bt2\.env' |
    Where-Object { $_ -match '^\s*BOOTSTRAP_PASSWORD\s*=' })
if ($passwordDefinitions.Count -ne 1) { throw 'Can dung mot dong BOOTSTRAP_PASSWORD trong .env.' }
$savedPassword = ($passwordDefinitions[0] -split '=', 2)[1]
[pscustomobject]@{
    SavedFileLength = $savedPassword.Length
    TerminalLength = ([string]$env:BOOTSTRAP_PASSWORD).Length
    SameValue = ($savedPassword -ceq $env:BOOTSTRAP_PASSWORD)
}
Remove-Variable savedPassword
~~~

SavedFileLength hợp lệ nhưng TerminalLength khác hoặc SameValue=False: nạp lại mục 6. Không chạy `Get-Content .env` trực tiếp ra màn hình để gửi ảnh hỗ trợ.

## 8. Chọn Java21

~~~powershell
java -version
.\backend\mvnw.cmd -version
~~~

Maven phải dùng Java21. Nếu đang dùng27, tìm JDK21:

~~~powershell
Get-ChildItem 'C:\Program Files\Eclipse Adoptium','C:\Program Files\Java',"$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
    Select-Object FullName
~~~

Máy bạn đã xác minh có JDK 21 ở đường dẫn dưới đây. Chạy nguyên đoạn trong terminal A từ root; không cần cài lại JDK:

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
if (!(Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) { throw 'Khong tim thay JDK21; xem danh sach thu muc JDK o tren.' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
.\backend\mvnw.cmd -version
~~~

Cả `java -version` và Maven phải báo 21. Lỗi `JAVA_HOME environment variable is not defined correctly` trước đó do gán nguyên chữ mẫu `THAY_BANG_...`, không phải thiếu JDK. Không đặt JAVA_HOME đến thư mục `bin` hoặc file java.exe; đặt đến thư mục JDK như trên. Cấu hình này áp dụng cho terminal hiện tại; mở terminal mới cần chạy lại.

Trong IntelliJ nếu chạy bằng Run Configuration: chọn JDK21 và nhập cùng các biến môi trường cho cấu hình đó. Biến trong Terminal không tự truyền sang Run Configuration.

## 9. Khởi động backend trên MySQL thật

Kiểm tra không có backend khác chiếm8080, rồi trong terminal đã nạp env:

~~~powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalPort,OwningProcess
~~~

Nếu có kết quả, xem `Get-Process -Id` với PID được hiển thị; Ctrl + C tại đúng terminal backend của bạn nếu muốn thay phiên. Không kill một PID chưa xác định. Nếu không có kết quả, cổng đang rảnh.

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2\backend
.\mvnw.cmd spring-boot:run
~~~

**Không thêm --spring.datasource.password= rỗng.** Backend phải dùng DB_PASSWORD đã cấu hình cho MySQL80.

Kết quả đúng:

- Hikari kết nối thành công.
- Flyway áp dụng migration còn thiếu hoặc thông báo schema up to date ởV5.
- Tomcat started on port8080.
- Started BackendApplication.

Giữ terminal chạy. GET /404 có thể bình thường vì backend phục vụ API.

Chờ cả dòng `Started BackendApplication` và không có `Application run failed`/`BUILD FAILURE` sau đó. Nếu dấu nhắc `PS ...>` quay lại vì lỗi, backend chưa chạy. Các warning metadata HTTP401 ở feed Maven trong log trước đây không phải lỗi đăng nhập MySQL1045; đọc phần `Caused by` cuối và lỗi cụ thể.

Terminal B có thể kiểm tra API trước khi chạy frontend:

~~~powershell
try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/api/auth/me' -TimeoutSec 5
    Write-Host 'API tra thanh cong; kiem tra ngu canh xac thuc.'
} catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401) {
        Write-Host 'API dang chay; 401 la dung khi chua dang nhap.'
    } else { throw }
}
~~~

| Lỗi | Kiểm tra |
|---|---|
| SERVICE_API_KEY/JWT_SECRET placeholder | File đã lưu và nạp trong đúng terminal chưa |
| Access denied | DB_USER/DB_PASSWORD có đúng account Test Connection chưa |
| Access denied root@localhost@localhost | Sửa DB_USER=root nếu Workbench dùng root; lưu và nạp lại env rồi restart |
| Communications link failure | MySQL80 Running, đúng127.0.0.1:3306 |
| Bootstrap password invalid | Password phải12ký tự trở lên, tối đa72byte khi bootstrap bật |
| Flyway checksum/table conflict | Dừng, kiểm lịch sử/schema; không sửaV1/V2 hoặc resetDB |
| Port8080 in use | Xác định và dừng đúng backend của bạn |

## 10. Khởi động frontend và đăng nhập

Terminal PowerShell thứ hai:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npm ci
npm run dev
~~~

Mở đúng URL Vite hiển thị, mặc định http://127.0.0.1:5173/.

Đăng nhập bằng account website đã có hoặc vừa được bootstrap:

- manager{center_id}: thống kê kỹ thuật viên, danh sách phản hồi và tỷ lệ trong trung tâm của mình.
- marketing: danh sách phản hồi toàn công ty và xuấtXLSX.
- director: tổng hợp CSAT theo trung tâm.

Nếu account vừa tạo, dùng BOOTSTRAP_PASSWORD bạn đã chọn. Nếu account tồn tại từ trước, dùng mật khẩu cũ của account.

Mật khẩu MySQL không được nhập vào form website.

## 11. Kiểm tra trên dữ liệu thật

Trong Workbench, chỉ đọc để kiểm chứng dữ liệu:

~~~sql
USE smartcrm_l8;

SELECT version,description,success
FROM flyway_schema_history ORDER BY installed_rank;

SELECT username,role,center_id,enabled FROM app_user;

SELECT t.ticket_code,i.status,i.sent_at,i.expires_at,r.score,r.responded_at
FROM ticket t
LEFT JOIN survey_invitation i USING(ticket_id)
LEFT JOIN survey_response r USING(ticket_id)
ORDER BY t.ticket_id;
~~~

Trong website:

1. Đăng nhập đúng vai trò.
2. Mở báo cáo/danh sách được phép.
3. Chọn đúng kỳ ngày và trung tâm rồi Áp dụng.
4. So số liệu với truy vấn cùng bộ lọc.
5. Marketing có thể xuấtXLSX để đối chiếu dữ liệu đang hiển thị.

Không gửi phản hồi thử tùy ý trên phiếu khách thật: phản hồi bất biến và sẽ được tính vào thống kê thật. Các test ghi dữ liệu, lỗi giả lập và concurrency tiếp tục chạy database test độc lập.

Không chạy frontend/e2e/prepare-fixture.ps1 vào3306: script chỉ dành cho môi trường test và từ chối3306. Không trỏ backend integration tests hoặc Playwright có ghi phản hồi vào smartcrm_l8. Xem web-run-guide.md cho quy trình test riêng.

Gateway hiện vẫn là SMS/Zalo mô phỏng, kể cả khi dùng MySQL thật.

## 12. Dừng website

- Terminal frontend: Ctrl + C.
- Terminal backend: Ctrl + C.
- MySQL80 có thể tiếp tục chạy như service bình thường.

Không chạy mysqladmin shutdown cổng3306 chỉ để đóng website. Không xóa schema hoặc datadir.

## 13. Lần chạy sau: một khối lệnh backend trong terminal A

Dùng khi `.env` đã cấu hình đúng và tài khoản đã có. Khối lệnh không thay `.env`, không sinh lại secret và không tạo database. Nó kiểm tra trước rồi chạy backend; nếu có lỗi sẽ dừng tại lỗi cần xử lý. Khởi động vẫn tự áp dụng migration chưa chạy theo cấu hình Flyway.

~~~powershell
& {
    $ErrorActionPreference = 'Stop'
    Set-Location 'D:\cdtn1\bt2\cdtn1-05-bt2'
    $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
    if (!(Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) { throw 'Khong tim thay JDK21.' }
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    $configPath = Join-Path (Get-Location) '.env'
    if (!(Test-Path -LiteralPath $configPath)) { throw 'Chua co .env. Lam muc 3 truoc.' }
    Get-Content -LiteralPath $configPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith('#')) {
            $pair = $line -split '=', 2
            if ($pair.Length -ne 2 -or $pair[0].Trim() -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
                throw 'Dong .env sai dinh dang; can TEN_BIEN=gia_tri.'
            }
            [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1], 'Process')
        }
    }
    if ($env:DB_URL -ne 'jdbc:mysql://127.0.0.1:3306/smartcrm_l8?connectionTimeZone=Asia/Ho_Chi_Minh') { throw 'DB_URL chua tro dung MySQL that3306/smartcrm_l8.' }
    if ([string]::IsNullOrWhiteSpace($env:DB_USER)) { throw 'DB_USER dang rong.' }
    if ($env:DB_USER -eq 'root@localhost') { throw 'Username sai: neu dung root thi DB_USER=root.' }
    if ([string]::IsNullOrWhiteSpace($env:SERVICE_API_KEY) -or $env:SERVICE_API_KEY.Length -lt 32) { throw 'SERVICE_API_KEY chua du32ky tu.' }
    if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or [Text.Encoding]::UTF8.GetByteCount([string]$env:JWT_SECRET) -lt 32) { throw 'JWT_SECRET chua du32byte.' }
    if ($env:BOOTSTRAP_ENABLED -notin @('true','false')) { throw 'BOOTSTRAP_ENABLED phai la true hoac false.' }
    if ($env:BOOTSTRAP_ENABLED -eq 'true' -and (
        [string]::IsNullOrWhiteSpace($env:BOOTSTRAP_PASSWORD) -or
        ([string]$env:BOOTSTRAP_PASSWORD).Length -lt 12 -or
        [Text.Encoding]::UTF8.GetByteCount([string]$env:BOOTSTRAP_PASSWORD) -gt 72
    )) { throw 'BOOTSTRAP_PASSWORD can it nhat12ky tu, toi da72byte.' }
    if ((Get-Service MySQL80).Status -ne 'Running') { throw 'Hay khoi dong service MySQL80.' }
    if (!(Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue)) { throw 'MySQL3306 chua Listen.' }
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { throw 'Cong8080 dang ban. Kiem tra backend cu.' }
    Set-Location backend
    & .\mvnw.cmd -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven/Java chua san sang.' }
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw 'Backend chua khoi dong thanh cong; doc Caused by trong log.' }
}
~~~

Các boolean chỉ kiểm tra cấu hình; mật khẩu MySQL đúng hay không được xác nhận bằng Workbench hoặc kết nối Hikari thực tế. Nếu gặp lỗi native warning từ Maven/PowerShell, dùng cách chạy từng bước mục 6–9 để xem log đầy đủ, không bỏ qua lỗi.

Backend chạy xong: mở terminal B làm mục 10, rồi chuyển sang [hướng dẫn demo từng luồng](web-demo-walkthrough.md). Khi cần hỗ trợ, gửi đoạn lỗi hoặc kết quả True/False; không gửi `.env`, token hay ảnh secret.

## Checklist

- [ ] MySQL80 Running ở3306.
- [ ] Workbench Test Connection thành công.
- [ ] Đã kiểm tra schema và backup dữ liệu hiện có.
- [ ] .env trỏsmartcrm_l8 ở3306.
- [ ] DB_USER/DB_PASSWORD đúng account MySQL.
- [ ] Hai secret riêng biệt, đủ độ dài.
- [ ] Đã quyết định có cần bootstrap account còn thiếu.
- [ ] Đã lưu và nạp.env.
- [ ] Các kiểm tra cấu hình đềuTrue.
- [ ] Maven dùngJava21.
- [ ] Backend kết nối MySQL và khởi động thành công.
- [ ] Frontend mở được, đăng nhập đúng account.
- [ ] Chỉ thực hiện kiểm thử có ghi dữ liệu trên môi trường test riêng.

Tài liệu nghiệm thu: [web-acceptance-test-cases.md](web-acceptance-test-cases.md), [web-acceptance-results.md](web-acceptance-results.md). Hướng dẫn test riêng: [web-run-guide.md](web-run-guide.md).

