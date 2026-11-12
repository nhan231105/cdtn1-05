# Smart CRM – Mekong Mobile · L8 · BT2

Phân hệ CSAT sau bảo hành tiếp nối thiết kế BT1 trên cùng repository. L8 đọc phiếu đã đóng từ L2/L4, tạo và gửi lời mời qua cổng SMS/Zalo giả lập, nhận một phản hồi bất biến cho mỗi phiếu và cung cấp tra cứu, thống kê, Excel theo vai trò. Backend dùng Spring Boot/MySQL; React lấy dữ liệu từ API thật. Các số liệu trong giao diện được truy vấn từ database đang kết nối.

Thiết kế gốc: [SRS](srs.md), [API Contract](api-contract.md), [Use Case](usecase-diagram.drawio), [wireframe](wireframe-l8.png). Xem [ma trận khoảng trống](docs/bt2-gap-analysis.md), [thay đổi thiết kế](docs/thay-doi-thiet-ke.md), [khai báo AI BT2](docs/ai-disclosure.md) và [hướng dẫn nộp](docs/submission-guide.md) để biết giới hạn bằng chứng hiện tại.

## Môi trường

- Java JDK 21; `JAVA_HOME` trỏ tới JDK, `java -version` phải chạy được. Không cần cài Maven riêng; dùng `backend/mvnw.cmd` trên Windows, `./mvnw` trên Linux/macOS.
- MySQL 8.0.16+ (CHECK được thực thi), một database riêng cho L8; tài khoản có quyền tạo/đổi bảng trong database để Flyway migrate. Không dùng chung schema L2/L4 đang vận hành khi dựng bản demo độc lập.
- Node.js 24, npm; dùng `frontend/package-lock.json` với `npm ci`.
- Windows PowerShell 5.1 trở lên cho script kiểm thử. Chạy lần đầu cần Internet tải Maven/npm dependencies. Không đổi Spring Boot 4.1.2 hoặc Java21 để khắc phục cấu hình môi trường.

Kiểm tra từ thư mục gốc repository:

```powershell
java -version
node --version
npm --version
git branch --show-current
```

Nhánh triển khai `feat/bt2-setup` đã được merge vào `main` và phát hành bằng annotated tag `v0.2`. Hash các ref đã xác minh được ghi tại [checkpoint 3](docs/checkpoint-3.md). Các bước dựng và kiểm thử bên dưới không thực hiện thao tác Git phát hành.

## Cấu hình lần đầu

1. Tạo database **mới** bằng MySQL client; bước này không xóa hay reset database đã có:

```powershell
mysql -u root -p
```

Trong MySQL shell (thay mật khẩu mẫu bằng giá trị chỉ lưu trên máy):

```sql
CREATE DATABASE IF NOT EXISTS smartcrm_l8 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'smartcrm'@'localhost' IDENTIFIED BY '<YOUR_NEW_LOCAL_PASSWORD>';
GRANT ALL PRIVILEGES ON smartcrm_l8.* TO 'smartcrm'@'localhost';
```

Nếu tài khoản đã tồn tại, `CREATE USER IF NOT EXISTS` giữ mật khẩu cũ; dùng đúng mật khẩu của tài khoản đó. Nếu MySQL client chưa ở PATH, gọi đường dẫn `mysql.exe` trong thư mục cài MySQL.

2. Sao chép file mẫu, mở và điền cấu hình:

```powershell
Copy-Item .env.example .env
notepad .env
```

Điền `DB_USER`, `DB_PASSWORD`, `SERVICE_API_KEY`, `JWT_SECRET`; hai secret phải khác nhau, mỗi secret ít nhất 32 byte. Có thể sinh từng giá trị bằng lệnh sau rồi dán vào `.env` trên máy:

```powershell
$randomBytes = New-Object byte[] 48
$randomGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
$randomGenerator.GetBytes($randomBytes)
[Convert]::ToBase64String($randomBytes)
$randomGenerator.Dispose()
```

Để tạo tài khoản demo từ DB hiện có, đặt `BOOTSTRAP_ENABLED=true` và `BOOTSTRAP_PASSWORD` riêng dài 12–72 byte. Bootstrap chỉ thêm tài khoản chưa tồn tại, không đổi mật khẩu/quyền của tài khoản đang có. Sau lần đầu, đổi `BOOTSTRAP_ENABLED=false`. Không có mật khẩu mặc định trong mã hoặc migration.

3. Nạp biến môi trường trong **mỗi cửa sổ PowerShell chạy backend**. Spring Boot không tự đọc `.env` ở root. File dùng dạng `KEY=value`, không bọc nháy, không dùng biểu thức shell:

```powershell
Get-Content .env | ForEach-Object {
    $configLine = $_.Trim()
    if ($configLine -and -not $configLine.StartsWith('#')) {
        $configPair = $configLine -split '=', 2
        if ($configPair.Length -eq 2) {
            Set-Item -LiteralPath ('Env:' + $configPair[0].Trim()) -Value $configPair[1]
        }
    }
}
```

`.env` bị Git ignore. Không đưa secret vào frontend hoặc `VITE_*`; service key chỉ dành cho module nội bộ gọi EP1/EP2.

## Chạy backend và React

Từ cửa sổ đã nạp `.env`:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

Flyway tự chạy V1–V6 theo lịch sử schema. V1/V2 đã áp dụng được giữ nguyên. Các migration mới bổ sung tên tham chiếu, tài khoản, retry gửi/nhắc, mẫu khảo sát và snapshot nội dung trên lời mời; không thay đổi phản hồi đã gửi. Chạy lại backend không replay seed hoặc nhân đôi lời mời. Bản demo V2 có phiếu `401`, `402`, `403` đã đóng, `404` đang xử lý; dữ liệu này là giả lập. Tên thiết bị chưa tích hợp hiển thị định danh `Thiết bị #...` từ database, không nhận là tên sản phẩm thực tế.

Mở cửa sổ thứ hai tại root:

```powershell
Set-Location frontend
npm ci
npm run dev
```

Mở `http://localhost:5173`. Vite proxy `/api` đến backend `http://127.0.0.1:8080` theo `frontend/vite.config.js`; bản chạy cục bộ mặc định không cần biến frontend. Nếu backend riêng dùng cổng khác, đặt `$env:VITE_API_TARGET='http://127.0.0.1:8081'` trước `npm run dev`. `frontend/.env.example` nhắc không đưa secret vào trình duyệt. Không cần bật CORS rộng. `npm run build` tạo `frontend/dist` cho triển khai cùng origin hoặc reverse proxy `/api`; thư mục build bị ignore.

## Đăng nhập và sử dụng

Khi đã bật bootstrap một lần, tài khoản từ V2 là `manager101`, `manager102`, `marketing`, `director`; dùng mật khẩu đã tự đặt trong `BOOTSTRAP_PASSWORD`. Không đổi `center_id` phía giao diện để mở rộng phạm vi; backend kiểm lại user/role/center từ MySQL mỗi request. JWT hết hạn sau một giờ; đăng xuất và đăng nhập lại khi có lỗi phiên.

| Người dùng | Chức năng |
|---|---|
| Khách hàng | Mở `/khao-sat/{token}`, chấm 1–5, nhận xét ≤1.000 ký tự, gửi một lần; không cần đăng nhập |
| Quản lý trung tâm | CSAT theo kỹ thuật viên, danh sách phản hồi và tỉ lệ phản hồi của trung tâm mình |
| Marketing | Tra cứu toàn công ty, lọc và tải XLSX theo đúng bộ lọc, tối đa 10.000 dòng |
| Ban giám đốc | Tổng hợp trung tâm/tháng, phân bố điểm và xu hướng |

Scheduler mặc định thăm dò phiếu `DA_DONG` mỗi 30 giây thay nguồn sự kiện L2/L4 trong bản chạy độc lập; chỉ đọc ticket, không sửa trạng thái phiếu. Nó tạo lời mời còn thiếu, gửi lời mời chờ, thử lại gateway lỗi tối đa ba lần cách nhau ít nhất 5 phút và nhắc sau 72 giờ nếu còn hạn/chưa trả lời. Một lời mời chỉ có một lần nhắc thành công; lỗi cổng khi nhắc được thử lại tối đa ba lần cách 5 phút bằng metadata riêng, không đổi thời điểm gửi ban đầu hoặc tạo token khác. `GATEWAY_SIMULATE_FAILURE=true` phục vụ thử lỗi cổng gửi; tắt lại để nhận gửi thành công. Không dùng `Thread.sleep` để chờ retry trong request.

Có thể kiểm tra luồng bằng API trong PowerShell. Nếu scheduler đã tạo lời mời của ticket 401, yêu cầu dưới nhận 409 là đúng chống trùng; không xóa lời mời để chạy lại:

```powershell
$inviteHeaders = @{ 'X-Service-Key' = $env:SERVICE_API_KEY }
$invitation = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/survey-invitations -Headers $inviteHeaders -ContentType application/json -Body '{"ticket_id":401}'
$delivery = Invoke-RestMethod -Method Post -Uri ('http://localhost:8080/api/survey-invitations/' + $invitation.invitation_id + '/send') -Headers $inviteHeaders -ContentType application/json -Body '{"channel":"SMS"}'
Start-Process $invitation.survey_url
```

Để demo thao tác API này từ DB mới, đặt `SURVEY_SCHEDULER_ENABLED=false` **trước lần khởi động đầu tiên**. Trong chạy tự động, liên kết thuộc lời mời đã tạo có thể đọc bằng truy vấn MySQL nội bộ chỉ đọc rồi mở frontend; không thêm API công khai lộ danh sách token:

```sql
SELECT invitation_id, ticket_id, status, CONCAT('http://localhost:5173/khao-sat/', token) AS survey_url
FROM survey_invitation ORDER BY invitation_id;
```

Sau gửi thành công, mở danh sách/thống kê đúng vai trò và đúng kỳ hiện tại để thấy dữ liệu mới. Điểm trống hiển thị “—”; phone luôn che. Mở lại link sau gửi nhận 409; phản hồi đã gửi không thể sửa hoặc xóa. Khi bộ lọc không ra kết quả, đổi kỳ/điểm để thử; khi tắt backend, giao diện báo lỗi và có “Thử lại”.

### Quản lý mẫu và vòng đời lời mời

Marketing có thể quản lý mẫu khảo sát qua API có JWT: `GET/POST /api/survey-templates`, `GET/PUT/DELETE /api/survey-templates/{id}`. Body gồm `name`, `title`, `intro_text`, `is_active`; chỉ một mẫu có thể hoạt động. Mỗi lời mời lưu tiêu đề/nội dung tại thời điểm tạo, nên sửa mẫu chỉ ảnh hưởng lời mời mới. Giao diện khách hàng hiển thị nội dung lấy từ API.

API nội bộ đọc danh sách `GET /api/survey-invitations` và chi tiết `GET /{id}`, đổi kênh bằng `PATCH` với `{"channel":"SMS"}` hoặc `ZALO`, và xóa `DELETE /{id}` chỉ khi lời mời chưa từng gửi. Các thao tác cần `X-Service-Key`. Khi xóa, hệ thống lưu dấu hủy để không thể phát hành token khác cho cùng phiếu. Lời mời đã gửi và phản hồi không thể sửa/xóa. Xem [phụ lục API](api-contract.md#9-phụ-lục-hiện-thực-bt2-ngày-12112026-bổ-sung-không-thay-hợp-đồng-bt1) và [thay đổi thiết kế](docs/thay-doi-thiet-ke.md).

## Kiểm thử

Lệnh backend đầy đủ trên Windows, từ root, dùng MySQL test tạm riêng tại cổng 3307; không dùng DB_USER/DB_PASSWORD của database đang làm việc:

```powershell
powershell -File scripts/test.ps1
```

Script cần bộ cài MySQL server có `mysqld.exe` và `mysql.exe`; mặc định `C:\Program Files\MySQL\MySQL Server 8.0\bin`. Nếu cài ở nơi khác, chạy `powershell -File scripts/test.ps1 -MySqlBin '<duong-dan-bin-MySQL8>'`. Nếu cổng test đang bận hoặc thiếu server, script phải báo lỗi; không chuyển qua DB đang vận hành và không bỏ qua tests integration. Chỉ chạy bộ unit riêng không thay cho nghiệm thu toàn suite.

Kiểm thử React và build:

```powershell
Set-Location frontend
npm ci
npm test
npm run build
```

Kiểm thử trình duyệt cần một backend **riêng** đang chạy trên MySQL test, bật bootstrap, scheduler tắt và một lời mời mới chưa sử dụng. Bộ test gửi phản hồi bất biến thật, không dùng database đang làm việc. Thiết lập fixture/biến E2E theo [frontend/README.md](frontend/README.md), rồi chạy trong thư mục frontend:

Trên Windows, biến môi trường mật khẩu rỗng có thể bị xóa. Với server test tạm `initialize-insecure` của script, truyền rõ `--spring.datasource.password=` khi chạy jar/backend để tránh placeholder `${DB_PASSWORD}` bị coi là giá trị. Chỉ áp dụng cho MySQL test tạm trên loopback; kết nối database đang sử dụng vẫn cần mật khẩu thật đã tự cấu hình.

```powershell
npx playwright install chromium
npm run test:e2e
```

Fixture E2E chỉ chấp nhận schema `_test` ở cổng khác 3306, thêm ticket/lời mời tổng hợp mới, không xóa dữ liệu. Chạy lại phải cấp lời mời mới, không reset phản hồi. Xem `frontend/playwright.config.js` và file tests để biết điều kiện đầy đủ; thiếu biến bắt buộc phải fail rõ. Test giao diện có mock API chỉ chứng minh hành vi giao diện; không được coi là bằng chứng MySQL. Kết quả từng bộ chạy và giới hạn chưa kiểm chứng ghi trong [báo cáo BT2](docs/reports/bt2-implementation-report.md).

## Dựng lại và giới hạn nghiệm thu

Đã kiểm tra clone Git sạch của `feat/bt2-setup` tại commit `b5199d5`: `npm ci`, 54 backend tests, 18 frontend tests, production build, E2E và browser/API checks PASS trên MySQL test mới. Kết quả chỉ xác minh cùng máy; không dùng `flyway:clean`, `DROP DATABASE`, xóa volume hoặc reset schema để tạo lại dữ liệu. Tình trạng push/merge/tag xem tại [checkpoint 3](docs/checkpoint-3.md).

S1 hiện có CRUD có điều kiện cho lời mời và CRUD mẫu khảo sát thuộc L8. Cách tính điểm rubric vẫn **chờ xác nhận học thuật**; phản hồi bất biến và bảng L2/L4 chỉ đọc. NFR01/02 được đo bằng MockMvc, NFR03 trên loopback/MySQL test, NFR05 với 100 POST cạnh tranh và NFR06 qua gateway giả lập; giới hạn phép đo ở [bảng xác minh](docs/verification-2026-11-12.md). NFR07 chưa thử 5 người thật.

Báo cáo cập nhật: [PDF ngày 12/11](docs/reports/BT2_L8_implementation_2026-11-12.pdf), [Markdown nguồn](docs/reports/bt2-implementation-report.md), [xác minh mới](docs/verification-2026-11-12.md), [ma trận gap](docs/bt2-gap-analysis.md), [ERD V6](docs/erd-l8-extension.md), [checkpoint 3](docs/checkpoint-3.md) và [minh chứng trình duyệt](docs/evidence/README.md). Sau `cd frontend; npm ci; npx playwright install chromium`, quay lại root và chạy `node scripts/render-report.mjs` để xuất lại PDF; script giữ PDF/DOCX cũ.
