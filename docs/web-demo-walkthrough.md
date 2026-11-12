# Demo và kiểm thử luồng sử dụng Smart CRM L8 — Mekong Mobile

Hướng dẫn thao tác được cập nhật theo V1–V6; kết quả chạy ngày 12/11/2026 nằm trong [verification report](verification-2026-11-12.md).

Tài liệu thao tác dành cho người demo trên Windows/IntelliJ/PowerShell/Workbench.
Nguồn đối chiếu: App.jsx, controller/service, fixture E2E và [API Contract](../api-contract.md).
Các bước dưới là hướng dẫn tái thực hiện, **không phải kết quả PASS của lần chạy mới**.

## Tra nhanh tài khoản đăng nhập

Mở website: http://127.0.0.1:5173/. Các username dưới đây được tạo khi bật bootstrap với dữ liệu trung tâm 101/102 của dự án:

| Tên đăng nhập | Vai trò | Chức năng demo | Màn hình sau đăng nhập |
|---|---|---|---|
| `manager101` | Quản lý trung tâm 101 | CSAT kỹ thuật viên, danh sách phản hồi, tỷ lệ phản hồi của trung tâm 101 | `/thong-ke/ky-thuat-vien` |
| `manager102` | Quản lý trung tâm 102 | Cùng chức năng quản lý, chỉ dữ liệu trung tâm 102; dùng để kiểm tra cách ly dữ liệu | `/thong-ke/ky-thuat-vien` |
| `marketing` | Nhân viên Marketing | Tra cứu phản hồi toàn công ty, lọc và xuất Excel | `/phan-hoi` |
| `director` | Ban giám đốc | Tổng hợp CSAT theo trung tâm/tháng, phân bố điểm và xu hướng | `/thong-ke/trung-tam` |

**Mật khẩu:** giá trị `BOOTSTRAP_PASSWORD` bạn tự đặt khi tài khoản được tạo lần đầu. Không có mật khẩu mặc định và không dùng `DB_PASSWORD` để đăng nhập website. Nếu tài khoản đã tồn tại, đổi `BOOTSTRAP_PASSWORD` không đổi mật khẩu cũ. Với môi trường demo riêng ở phần B, dùng mật khẩu demo đã nhập trong terminal A tại mục 7.

**Thứ tự đăng nhập khi demo:** `manager101` → Đăng xuất → `marketing` → Đăng xuất → `director`. Dùng thêm `manager102` để chứng minh không xem được phản hồi thuộc trung tâm 101. Đăng xuất trước khi chuyển vai trò trong cùng tab.

**Khách hàng không cần tài khoản:** mở link `/khao-sat/{token}` hợp lệ từ mục 9 để trả lời khảo sát. Project không có tài khoản Admin.

Nếu không đăng nhập được, trong Workbench kiểm tra tài khoản thực tế, không truy vấn mật khẩu/hash:

~~~sql
SELECT username, role, center_id, enabled
FROM smartcrm_l8.app_user
ORDER BY username;
~~~

Truy vấn trên dành cho MySQL thật. Nếu đang demo phần B, đổi schema thành `smartcrm_l8_test` trên connection 3307. Account phải tồn tại và `enabled=1`; nếu trung tâm khác 101/102, dùng username quản lý thực tế trong kết quả.

## 1. Bắt đầu từ file nào?

| Việc cần làm | File |
|---|---|
| Mở máy, cấu hình .env, Java21, MySQL3306, chạy backend/frontend | [local-env-setup-guide.md](local-env-setup-guide.md), lần chạy sau dùng mục13 |
| Demo từ đăng nhập đến khảo sát, thống kê và Excel | File hiện tại |
| Chi tiết expected/actual của TC01–TC20 | [web-acceptance-test-cases.md](web-acceptance-test-cases.md) |
| Bằng chứng lần kiểm thử đã thực hiện | [web-acceptance-results.md](web-acceptance-results.md) |

Hai chế độ dùng cùng website:
- **Phần A:** đang chạy MySQL80/3306/smartcrm_l8, xem dữ liệu hiện có và demo theo vai trò.
- **Phần B:** muốn tự gửi khảo sát/kiểm lỗi, chuyển backend sang MySQL riêng3307/smartcrm_l8_test để không làm sai dữ liệu thật. Đây vẫn là React → API → MySQL thật, chỉ schema và server dùng riêng cho thử nghiệm.

Không chạy hai backend cùng cổng8080. Không chuyển database giữa lúc khách/người khác đang dùng website của bạn.

## 2. Website đang làm được gì?

| Chức năng | Ai thao tác | Màn hình/API | Điểm chứng minh |
|---|---|---|---|
| Đăng nhập và phân quyền | Quản lý/Marketing/Giám đốc | / | JWT, role và center |
| Tạo lời mời phiếu đã đóng | Hệ thống L2/L4 hoặc scheduler | POST /api/survey-invitations | Chống phiếu sai/trùng |
| Gửi lời mời SMS/ZALO | API nội bộ/tác vụ nền | POST /api/survey-invitations/{id}/send | DA_GUI/GUI_LOI và số lần gửi |
| Chấm điểm1–5 và nhận xét | Khách không cần login | /khao-sat/{token} | Lưu một response bất biến |
| Tra cứu/lọc/phân trang | Quản lý/Marketing | /phan-hoi | Dữ liệu API thật, SĐT che |
| CSAT theo kỹ thuật viên | Quản lý trung tâm | /thong-ke/ky-thuat-vien | Chỉ trung tâm của mình |
| Tổng hợp theo trung tâm/tháng | Ban giám đốc | /thong-ke/trung-tam | CSAT, phân bố điểm, xu hướng |
| Tỷ lệ phản hồi | Quản lý trung tâm | /thong-ke/ty-le | Phản hồi/lời mời gửi thành công |
| Xuất XLSX | Marketing | Nút Xuất Excel tại /phan-hoi | File đúng bộ lọc, SĐT che |
| Nhắc sau72h, retry | Scheduler | Không có màn hình/endpoint public | Integration tests, metadata |

Không có Admin, đăng ký public, UI tạo phiếu bảo hành, UI gửi lời mời thủ công hoặc sửa/xóa response.
Gateway SMS/Zalo là **mô phỏng**, không phải gửi tin qua nhà cung cấp thật.
Thiết bị/KTV thiếu tên reference hiển thị định danh #ID; không tự coi đây là tên thiết bị đã tích hợp L2/L4.

## Phần A — Demo dữ liệu hiện có trên MySQL3306

## 3. Kiểm tra trước buổi demo

1. Terminal A có dòng Started BackendApplication và Tomcat8080, không có Application run failed sau đó.
2. Terminal B chạy frontend:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npm ci
npm run dev
~~~

3. Mở http://127.0.0.1:5173/. Nếu Vite đang chạy rồi thì không start thêm.
4. Trong Workbench, connection127.0.0.1:3306, chạy toàn bộ từng câu đến dấu chấm phẩy:

~~~sql
SELECT DATABASE(), VERSION();
SELECT username,role,center_id,enabled FROM smartcrm_l8.app_user;
SELECT center_id,center_name FROM smartcrm_l8.service_center;
SELECT MIN(responded_at) AS first_response,MAX(responded_at) AS last_response,
       COUNT(*) AS response_count
FROM smartcrm_l8.survey_response;
~~~

Chọn khoảng ngày có dữ liệu. Bộ lọc mặc định tháng hiện tại: không có dữ liệu trong tháng này không phải lỗi. Ngày from/to được tính theo giờ Việt Nam, khoảng tối đa366ngày.

Account chuẩn từ seed101/102: manager101,manager102,marketing,director. Nếu DB có center khác, lấy username trong app_user. Mật khẩu là password khi account được bootstrap lần đầu, không phải DB_PASSWORD. Không biết password cũ thì không xóa account/reset database để demo.

## 4. Demo quản lý trung tâm — UC05/UC06/UC09

Trình duyệt:
1. Ở /, nhập manager101 (hoặc account quản lý thực tế), password website → Đăng nhập.
2. Phải vào “Chất lượng từ đội ngũ của bạn”.
3. Chọn Từ ngày/Đến ngày đúng kỳ → Áp dụng.
4. Xem Tên KTV, Đã gửi, Phản hồi, CSAT TB, Hài lòng.
5. Bấm Làm mới số liệu.
6. Bấm Danh sách phản hồi trên menu.
7. Kiểm trung tâm bị khóa, không có nút Xuất Excel, SĐT đã che.
8. Thử lọc kỹ thuật viên/điểm/ngày → Áp dụng; kết quả phải theo bộ lọc.
9. Đổi Dòng/trang20/50/100. Nếu đủ dữ liệu, bấm Sau/Trước; nếu ít dòng, Sau bị disable là đúng.
10. Bấm Tỉ lệ phản hồi, chọn cùng kỳ → Áp dụng.
11. F5 một màn: session còn trong tab và dữ liệu tải lại.
12. Đăng xuất.

Kỳ vọng: manager chỉ xem center của account; CSAT là AVG(score), Hài lòng là score≥4/tổngresponse, không response hiển thị “—”.
Không lấy response count theo ngày trả lời chia tùy ý cho invitations của tập khác: tỷ lệ phản hồi theo cohort tháng gửi thành công, đếm response của cohort đó đến hết kỳ.

Câu thuyết trình: “Quản lý theo dõi chất lượng và mức tham gia khảo sát trong phạm vi trung tâm, số liệu lấy từ API/MySQL.”

Minh chứng: ảnh bảng kỹ thuật viên, danh sách SĐT che, tỷ lệ phản hồi và bộ lọc.

## 5. Demo Marketing — UC05/UC08

1. Login marketing.
2. Phải vào /phan-hoi, tiêu đề “Lắng nghe từng trải nghiệm”.
3. Chọn Trung tâm, Kỹ thuật viên, Từ ngày, Đến ngày, Điểm từ/Điểm đến → Áp dụng.
4. Kiểm total, dòng và phân trang theo bộ lọc.
5. Nhấn Xuất Excel; phải có thông báo đã tải file.
6. Mở file bằng Excel/LibreOffice. So dữ liệu với **cùng snapshot/bộ lọc** của danh sách, không đối chiếu với số liệu đã thay đổi sau khi tải.

Bảy cột: Mã phiếu, Trung tâm, Kỹ thuật viên, Điểm, Nhận xét, Số điện thoại (che), Thời điểm phản hồi.
Số điện thoại trong file cũng phải che. Giới hạn xuất10000dòng; nếu chưa tạo10001rows thì không tự nói đã thử giới hạn đó.

Câu thuyết trình: “Marketing tra cứu nhiều trung tâm và xuất đúng tập phản hồi đã lọc, giữ che số điện thoại.”

## 6. Demo ban giám đốc — UC07

1. Đăng xuất Marketing, login director.
2. Phải vào /thong-ke/trung-tam, “Bức tranh hài lòng toàn công ty”.
3. Chọn kỳ/ngày và trung tâm → Áp dụng.
4. Xem số phản hồi, CSAT, tỷ lệ hài lòng và phân bố điểm1→5.
5. Muốn trình bày xu hướng, chọn nhiều tháng có dữ liệu, vẫn tối đa366ngày.
6. Nhập trực tiếp http://127.0.0.1:5173/phan-hoi.
7. Phải hiện “Không có quyền truy cập”; quay lại menu CSAT theo trung tâm.

Một tháng/nhóm rỗng có thể hiện giá trị “—”; không tự tạo dữ liệu để biến ảnh thành đẹp.

Câu thuyết trình: “Giám đốc xem bức tranh tổng hợp theo trung tâm và tháng; quyền tổng hợp không đồng nghĩa được xem danh sách chi tiết.”

## Phần B — Demo gửi khảo sát từ đầu đến cuối trên MySQL riêng

## 7. Chuẩn bị backend demo riêng

Đây là lựa chọn khi cần tự tạo/gửi dữ liệu mới. Không sửa .env đang dùng MySQL3306; chỉ ghi đè biến trong process terminal A. Vite vẫn dùng backend8080.

1. Đảm bảo không có người khác đang sử dụng backend hiện tại.
2. Terminal A đang chạy backend3306: Ctrl + C, đợi backend dừng.
3. Nạp .env theo mục6 của local-env-setup-guide.md để có JWT_SECRET/SERVICE_API_KEY. Nếu vẫn terminal A cũ và chưa đổi cấu hình thì các biến còn tồn tại.
4. Đặt Java và chạy MySQL test:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
if (!(Test-Path -LiteralPath "$env:JAVA_HOME\bin\java.exe")) { throw 'Khong tim thay JDK21.' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$demoDatabase = & .\scripts\start-test-db.ps1
$demoDatabase | Select-Object Port,DataDirectory
~~~

Kỳ vọng3307 và datadir mới. Nếu3307bận, xác định server test của bạn; không khởi tạo thêm và không truy cập server lạ.

5. Trong cùng terminal A, ghi đè cấu hình chỉ cho phiên demo:

~~~powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3307/smartcrm_l8_test?connectionTimeZone=Asia/Ho_Chi_Minh'
$env:DB_USER = 'root'
$env:BOOTSTRAP_ENABLED = 'true'
$env:SURVEY_SCHEDULER_ENABLED = 'false'
$env:SURVEY_EXPIRY_DAYS = '7'
$env:SURVEY_PUBLIC_BASE_URL = 'http://127.0.0.1:5173/khao-sat'
$env:GATEWAY_SIMULATE_FAILURE = 'false'
Remove-Item Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue

$demoPasswordInput = Read-Host 'Tu dat mat khau website demo moi (12-72 byte, it nhat12ky tu)' -AsSecureString
$env:BOOTSTRAP_PASSWORD = [Net.NetworkCredential]::new('', $demoPasswordInput).Password
Remove-Variable demoPasswordInput
if (([string]$env:BOOTSTRAP_PASSWORD).Length -lt 12 -or
    [Text.Encoding]::UTF8.GetByteCount([string]$env:BOOTSTRAP_PASSWORD) -gt 72) {
    throw 'Mat khau demo chua du dieu kien; nhap lai truoc khi chay backend.'
}
~~~

Password vừa nhập chỉ cho database demo mới; ghi nhớ để login. Không sửa password các account database3306.

6. Chạy backend test:

~~~powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="
~~~

Chỉ lượt này mới truyền password MySQL rỗng vì server tạm initialize-insecure. Kiểm log JDBC **3307/smartcrm_l8_test**, FlywayV1–V6 và Started BackendApplication. Nếu log vẫn3306, dừng ngay và nạp lại cấu hình đúng trước thao tác fixture.
7. Terminal B giữ Vite127.0.0.1:5173. Đăng xuất phiên browser cũ vì bạn đã chuyển database/account.

## 8. Workbench chụp dữ liệu trước thao tác

Tạo connection Workbench thứ hai:127.0.0.1:3307, username root, password trống cho server test tạm.

~~~sql
USE smartcrm_l8_test;
SELECT COUNT(*) AS invitations_before FROM survey_invitation;
SELECT COUNT(*) AS responses_before FROM survey_response;
SELECT ticket_id,ticket_code,status FROM ticket ORDER BY ticket_id;
~~~

Trên DB mới, hai count bằng0. Giữ kết quả làm mốc; nếu đã demo trước, ghi count thực tế, không xóa response.

## 9. Tạo/gửi lời mời và mở form — UC01/UC02

Không có nút UI tạo/gửi lời mời. Thao tác này thuộc hệ thống nội bộ; dùng fixture hiện có gọi API thật.

Mở terminal **C — Fixture**, chạy:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2
$serviceKeyLines = @(Get-Content -LiteralPath '.env' |
    Where-Object { $_ -match '^\s*SERVICE_API_KEY\s*=' })
if ($serviceKeyLines.Count -ne 1) { throw 'Can dung mot dong SERVICE_API_KEY trong .env.' }
$env:E2E_SERVICE_KEY = ($serviceKeyLines[0] -split '=',2)[1]
Remove-Variable serviceKeyLines
Remove-Item Env:TEST_DB_PASSWORD -ErrorAction SilentlyContinue

$fixture = & .\frontend\e2e\prepare-fixture.ps1 -TestPort 3307 -TestDatabase smartcrm_l8_test -ApiBase 'http://127.0.0.1:8080'
$fixture | Select-Object TicketCode,InvitationId
Start-Process ('http://127.0.0.1:5173/khao-sat/' + $fixture.Token)
~~~

Không in toàn bộ $fixture vì chứa token. Không đổi tham số sang3306. Cấu hình terminal A phải đúng3307 như mục7; fixture không thể tự xác minh JDBC của backend hộ bạn.
Fixture thêm ticket tổng hợp E2E mới rồi gọi create/send; không xóa dữ liệu. Nếu có401, key của C phải trùng SERVICE_API_KEY backend A đang dùng. Chưa sửa .env giữa hai bước.

Workbench:

~~~sql
SELECT t.ticket_id,t.ticket_code,i.invitation_id,i.status,i.channel,i.send_attempts,
       i.created_at,i.sent_at,i.expires_at
FROM ticket t JOIN survey_invitation i USING(ticket_id)
WHERE t.ticket_code LIKE 'E2E-%'
ORDER BY t.ticket_id DESC;
~~~

Kỳ vọng DA_GUI,channelSMS,send_attempts1,sent_atcógiátrị. Log “Simulated message accepted” nghĩa cổng mô phỏng chấp nhận, không gửi SMS thật.

Câu thuyết trình: “Module bảo hành kích hoạt tạo/gửi lời mời cho phiếu đóng; UI khách dùng link có mã riêng.”

## 10. Khách gửi phản hồi — UC03

Trình duyệt vừa mở:
1. Kiểm mã phiếu đúng TicketCode, trung tâm và thiết bị.
2. Chưa chọn điểm, nhập nhận xét rồi bấm GỬI PHẢN HỒI → phải báo chọn điểm, chưa ghi DB.
3. Chọn5điểm, nhập nhận xét tổng hợp dễ tìm → GỬI PHẢN HỒI.
4. Kỳ vọng “Đánh giá đã được ghi nhận”, hiển thị điểm/comment.
5. F5 → “Bạn đã gửi phản hồi”, không có form gửi lại.
6. Dùng DevTools Device Toolbar thử390×844 và375×667; kiểm form trước khi submit bằng một fixture mới nếu form cũ đã đóng.

Workbench sau gửi:

~~~sql
SELECT t.ticket_code,i.status,i.send_attempts,r.response_id,r.score,r.comment,r.responded_at
FROM ticket t JOIN survey_invitation i USING(ticket_id)
LEFT JOIN survey_response r USING(ticket_id)
WHERE t.ticket_code LIKE 'E2E-%'
ORDER BY t.ticket_id DESC;

SELECT COUNT(*) AS invitations_after FROM survey_invitation;
SELECT COUNT(*) AS responses_after FROM survey_response;
SELECT ticket_id,COUNT(*) FROM survey_response GROUP BY ticket_id HAVING COUNT(*)>1;
~~~

Nếu chỉ tạo1fixture/gửi1response, counts tăng mỗi bảng1; trạng thái DA_PHAN_HOI, điểm/commentkhớp; duplicate query không có dòng. Không có edit/delete response.

Chọn1–4 để demo thêm: mỗi điểm phải tạo fixture mới theo mục9, không tái dùng token đã gửi. PublicAPI giới hạn10request/phút/IP: nếu429, đợi cửa sổ phút rồi thử lại, không tắt limiter.

## 11. Chứng minh dữ liệu vừa gửi đi vào báo cáo

Trên browser nội bộ thực hiện lại mục4–6 với password demo vừa nhập ở terminal A:
1. Login manager101 → ngày hiện tại → Làm mới số liệu.
2. Danh sách phản hồi có mã E2E và comment vừa gửi; center101bịkhóa, phoneche.
3. Tỷ lệ phản hồi trong tháng có lời mời này.
4. Logout → manager102: cùng kỳ không thấy response center101.
5. Logout → marketing: lọc center101/điểm5, thấy response → Xuất Excel → mở file.
6. Logout → director: trung tâm101 có phản hồi/phân bốđiểm5.

Để đối chiếu SQL với báo cáo, Workbench test:
- Chọn cùng kỳ tháng hiện tại.
- Truy vấn dưới chỉ center101/technician301, phù hợp fixture mặc định.

~~~sql
SELECT COUNT(*) AS responses,ROUND(AVG(r.score),2) AS csat_avg,
       ROUND(SUM(r.score>=4)/NULLIF(COUNT(*),0),2) AS satisfied_rate
FROM survey_response r JOIN ticket t USING(ticket_id)
WHERE t.center_id=101 AND t.technician_id=301
  AND r.responded_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')
  AND r.responded_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01');

SELECT COUNT(*) AS invitations_sent,
       SUM(r.response_id IS NOT NULL AND r.responded_at <
           DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01')) AS responses,
       ROUND(SUM(r.response_id IS NOT NULL AND r.responded_at <
           DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01')) /
           NULLIF(COUNT(*),0),2) AS response_rate
FROM survey_invitation i JOIN ticket t USING(ticket_id)
LEFT JOIN survey_response r USING(ticket_id)
WHERE t.center_id=101 AND i.status IN ('DA_GUI','DA_PHAN_HOI')
  AND i.sent_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')
  AND i.sent_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01');
~~~

DB test server mặc định dùng timezone máy; máy demo cần giờ Việt Nam, cùng kỳ với UI. API trả tỷ lệ0–1 còn UI hiển thị phần trăm:1.00→100%, không phải chênh số liệu.

Kết luận có thể nói khi đã quan sát đủ: “Dữ liệu từ form đã lưu MySQL và được các màn hình theo vai trò truy vấn lại.” Không coi việc chỉ mở trang là bằng chứng end-to-end.

## 12. Demo lỗi và phân quyền

Làm trên backend test3307. Không in key/token/JWT ra log công khai.

| Phép thử | Cách làm | Kỳ vọng |
|---|---|---|
| Sai login | Logout, nhập marketing + password sai | Lỗi xác thực, vẫn ở form |
| Role không được xem | Director gõ /phan-hoi | Không có quyền truy cập |
| Scope trung tâm | manager102 xem cùng kỳ có fixturecenter101 | Không thấy phản hồi đó; integration test chứng minh API403 khi cố center khác |
| Validation form | Chưa chọn điểm bấm gửi | Không lưu, yêu cầu chọn điểm |
| Filter sai | Từ ngày sau Đến ngày → Áp dụng | UI báo lỗi |
| Survey sai | Mở /khao-sat/invalid | Liên kết không hợp lệ, API404 |
| Gửi lại | F5 link đã gửi | Đã phản hồi, không cho gửi tiếp |
| Empty | Marketing lọc một kỳ cũ không có response | Thông báo chưa có dữ liệu; kiểm SQL nếu muốn xác nhận |
| Loading | F12→Network→Slow3G, reload một màn | Đang tải dữ liệu; ảnh loading rồi trả mạng về No throttling |
| Error/recovery | Terminal A test Ctrl+C; browser reload màn report; chạy lại đúng lệnh backend test, bấm Thử lại | Báo lỗi thay vì trắng trang; khôi phục dữ liệu khi API trở lại |

Để kiểm HTTP thiếu credential bằng terminal C mà không cần biết JWT:

~~~powershell
try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8080/api/survey-responses' -TimeoutSec 5
    Write-Host 'Khong mong doi: request khong JWT lai thanh cong.'
} catch {
    if ($_.Exception.Response) {
        Write-Host ('HTTP status: ' + [int]$_.Exception.Response.StatusCode)
    } else { throw }
}
~~~

Kỳ vọng401. Các test403/expiry/retry/rollback/limit xem suite backend và TC01–TC20. Không tự sửa timestamp của dữ liệu thật để dựng linkhếthạn.

## 13. Retry và nhắc72h: trình bày đúng mức chứng minh

Không có nút “Nhắc khảo sát”. Scheduler mặc định chạy mỗi30s khi bật, xử lý lời mời đủ điều kiện; reminder sau72h dùng cùng token.
Bộ integration tests kiểm:
- Gateway lỗi, GUI_LOI và metadata được lưu; retry≤3, cách≥5phút.
- Reminder chỉ một lần thành công, đủ72h/chưaphảnhồi/cònhạn, retry riêng.
- Hai submission cạnh tranh chỉ một response, transaction rollback khi DBfail.

Tests dùng gateway mock và thiết lập thời gian fixture để không chờ72h. Khi chỉ xem testsPASS, hãy nói “đã kiểm logic bằng integration tests”; không nói đã chờ72h hay gửiSMS thật.
Môi trường demo mục7 tắt scheduler để thao tác có thứ tự; sau phần demo có thể giải thích cấu hình thay vì bật scheduler làm phát sinh dữ liệu ngoài kịch bản.

## 14. Chạy automated tests để chốt demo

Backend suite cần3307rảnh. Kết thúc phần B trước:
1. Terminal A Ctrl+C backend test.
2. Terminal C shutdown đúng server test của mình:

~~~powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe' --no-defaults --host=127.0.0.1 --port=3307 --user=root shutdown
~~~

3. Terminal C đặt Java21 rồi chạy suite backend từroot:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
~~~

Script tự tạo DBtestmới, chạy toàn suite, shutdown server do nó sở hữu; giữ datadir. Đọc tổngTestsrun/Failures/Errors/Skipped và BUILDSUCCESS; không lấy kết quả lịch sử làm kết quả lượt này.

Frontend:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npm ci
npm test
npm run build
~~~

E2E thực nếu muốn demo tự động: cần dựng lại backendtest nhưmục7 vì server suitebackend đãshutdown; mỗi lượt cần token chưa gửi. ViteB còn chạy có thể dùng lại. Terminal C:

~~~powershell
cd D:\cdtn1\bt2\cdtn1-05-bt2\frontend
npx playwright install chromium
$fixture = & .\e2e\prepare-fixture.ps1 -TestPort 3307 -ApiBase 'http://127.0.0.1:8080'
$env:E2E_SURVEY_TOKEN = $fixture.Token
$env:E2E_TICKET_CODE = $fixture.TicketCode
$e2ePasswordInput = Read-Host 'Nhap password website demo da dat o terminal A' -AsSecureString
$env:E2E_PASSWORD = [Net.NetworkCredential]::new('', $e2ePasswordInput).Password
Remove-Variable e2ePasswordInput
$env:E2E_BASE_URL = 'http://127.0.0.1:5173'
npm run test:e2e
~~~

E2E_SERVICE_KEY ở C phải vẫn được nạp nhưmục9. Test gửi response thật vào DBtest, kiểm báo cáo/lookup/download và director403. Không dùng token manualdemo đã gửi. Nếu E2E fail, giữ lỗi để phân tích; không đổi expected status đểPASS.

Không chạy loadtest trên database3306. Các số liệu50/100CCU,10000responses,usability5người cần phép đo riêng; không kết luận đạt từ suite chức năng.

## 15. Ghi nhận buổi demo

Tạo thư mục lưu ảnh riêng cho buổi của bạn; không ghi đè kết quả lịch sử trong web-acceptance-results.md nếu chưa có kết quả mới. Không chụp .env/clipboard/password/JWT/token trong URL.

| Mục | Kết quả tự điền | Minh chứng |
|---|---|---|
| Backend + MySQL kết nối | PASS/FAIL/BLOCKED | Logstartup đã che bí mật |
| Đăng nhập đúng/sai | | ẢnhUI |
| Quản lý đúng center | | Bảng/filter |
| Marketing + fileExcel | | Ảnh/file đã chephone |
| Director + sai quyền | | Ảnhthốngkê/khôngquyền |
| Khách gửi và chống gửi lại | | Form/success/duplicate |
| MySQL trước/sau | | Kết quảSQL khôngtoken |
| Loading/empty/error/retry | | Ảnh, ghi rõ thực/môphỏng |
| Backendtests | | Total/pass/fail/skip |
| Frontendtests/build | | Total/pass/fail/skip/build |
| E2E | | Kết quả thật hoặc BLOCKED |

Tên/ngày/người thực hiện: tự ghi sau buổi chạy.
PASS khi kết quả thực khớp expected; FAIL khi có sai lệch tái hiện; BLOCKED khi thiếu điều kiện hoặc chưa chạy. Không tự điền sẵnPASS.

## 16. Kết thúc và trở về MySQL3306

- Dừng backendtest và shutdown server3307 của mình nhưmục14 nếu còn chạy.
- Muốn tiếp tục dùng database thật: terminal A dùng **mục13 của local-env-setup-guide.md**, nạp lại.env có DB_URL3306; không dùng lại biến DB_URL3307 đang còn trongprocess.
- Đăng xuất/login lại browser sau đổi database; kiểm logJDBCđúng3306.
- Dừng ViteB bằng Ctrl+C nếu đã xong.
- MySQL80/3306 giữ Running; không DROP/TRUNCATE/reset hay sửa response để dọn demo.
- Datadirtestđược giữ; nếu cần lượt mới, tạo server mới khi3307rảnh. Không xóa dữ liệu thật.

