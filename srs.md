# **Đặc tả yêu cầu phần mềm (SRS rút gọn)**

## **Phân hệ Khảo sát hài lòng CSAT sau bảo hành – Smart CRM, Mekong Mobile (Luồng L8)**

| Sinh viên            | Võ Nguyễn Thành Nhân – MSSV 2374802010360                                                                     |
|----------------------|---------------------------------------------------------------------------------------------------------------|
| Track                | SE (Software Engineering)                                                                                     |
| Học phần             | Chuyên đề tốt nghiệp 1 – HK1, năm học 2026–2027                                                               |
| Phiên bản / ngày lập | 1.0 – 06/10/2026                                                                                              |
| Tài liệu đi kèm      | docs/usecase-diagram.drawio (sơ đồ gốc), docs/usecase-diagram.svg, docs/api-contract.md (mẫu đặc tả track SE) |

## **1. Giới thiệu**

### **1.1 Mục đích**

Tài liệu đặc tả yêu cầu cho phân hệ **Khảo sát hài lòng** của hệ thống Smart CRM: thu thập đánh giá của khách hàng ngay sau khi phiếu bảo hành được đóng, và cung cấp số liệu hài lòng cho Quản lý trung tâm, Nhân viên Marketing và Ban giám đốc. Phân hệ giải quyết vấn đề **V7** của case study: *sau khi bảo hành xong không có kênh thu thập phản hồi; khiếu nại chỉ được biết khi khách đăng lên mạng xã hội.*

### **1.2 Phạm vi (một câu)**

Khi một phiếu bảo hành chuyển sang trạng thái ĐÃ ĐÓNG, hệ thống tự gửi cho khách hàng một liên kết khảo sát để chấm điểm hài lòng 1–5 kèm nhận xét (mỗi phiếu một lần), lưu phản hồi vào cơ sở dữ liệu, và cho Quản lý trung tâm, Nhân viên Marketing, Ban giám đốc xem thống kê hài lòng theo kỹ thuật viên, trung tâm bảo hành và tháng.

**Điểm bắt đầu:** phiếu bảo hành chuyển sang ĐÃ ĐÓNG. **Điểm kết thúc:** phản hồi khảo sát được lưu thành công và số liệu thống kê phản ánh phản hồi đó.

### **1.3 Bảng thuật ngữ (mỗi khái niệm chỉ có MỘT tên, dùng thống nhất trong SRS, sơ đồ và API)**

| **Thuật ngữ**       | **Định nghĩa**                                                                                                                                                                     | **Tên kỹ thuật**                   |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------|
| Khách hàng          | Cá nhân đã mua sản phẩm hoặc sử dụng dịch vụ của Mekong Mobile                                                                                                                     | customer                           |
| Phiếu bảo hành      | Yêu cầu bảo hành/sửa chữa được ghi nhận, có mã duy nhất và vòng đời trạng thái                                                                                                     | ticket                             |
| Trạng thái phiếu    | Mới → Đã phân công → Đang xử lý → Chờ linh kiện → Hoàn tất → **Đã đóng** (nhánh phụ: Đã hủy). Giá trị lưu: MOI, DA_PHAN_CONG, DANG_XU_LY, CHO_LINH_KIEN, HOAN_TAT, DA_DONG, DA_HUY | ticket.status                      |
| Trung tâm bảo hành  | Nơi tiếp nhận và sửa chữa phiếu bảo hành (6 trung tâm)                                                                                                                             | service_center                     |
| Kỹ thuật viên       | Nhân viên sửa chữa phiếu bảo hành                                                                                                                                                  | technician                         |
| Quản lý trung tâm   | Quản lý Trung tâm bảo hành, phụ trách một trung tâm                                                                                                                                | vai trò QL_TRUNG_TAM               |
| Nhân viên Marketing | Nhân viên thuộc Bộ phận Marketing                                                                                                                                                  | vai trò MARKETING                  |
| Ban giám đốc        | Ban điều hành công ty                                                                                                                                                              | vai trò BAN_GIAM_DOC               |
| Khảo sát hài lòng   | Hoạt động hỏi khách về mức hài lòng sau khi phiếu bảo hành được đóng, thang điểm 1–5 kèm nhận xét                                                                                  | survey                             |
| Lời mời khảo sát    | Bản ghi gắn **một** phiếu bảo hành với **một** liên kết khảo sát duy nhất, có hạn dùng và trạng thái (CHO_GUI, DA_GUI, GUI_LOI, DA_PHAN_HOI)                                       | survey_invitation *(bảng bổ sung)* |
| Biểu mẫu khảo sát   | Trang web khách hàng mở bằng liên kết khảo sát để chấm điểm và nhận xét                                                                                                            | giao diện                          |
| Phản hồi khảo sát   | Kết quả một khách hàng gửi: điểm, nhận xét, thời điểm; tối đa một phản hồi cho mỗi phiếu bảo hành                                                                                  | survey_response                    |
| Điểm hài lòng       | Số nguyên 1–5 (1 = rất không hài lòng, 5 = rất hài lòng)                                                                                                                           | score                              |
| CSAT                | Điểm hài lòng trung bình của một nhóm phản hồi, làm tròn 2 chữ số thập phân                                                                                                        | csat_avg                           |
| Tỉ lệ hài lòng      | Tỉ lệ phản hồi có điểm hài lòng ≥ 4                                                                                                                                                | satisfied_rate                     |
| Tỉ lệ phản hồi      | Số phản hồi khảo sát ÷ số lời mời khảo sát đã gửi thành công                                                                                                                       | response_rate                      |

### **1.4 Tài liệu tham chiếu**

Case study Smart CRM – Mekong Mobile (Mục 2.2 V7; Mục 3 thuật ngữ; Mục 4 phát biểu của anh Khoa, ông Minh, chị Trâm; Mục 6.2 vòng đời phiếu; Mục 8 bảng survey_response; Mục 9 quy tắc QT-xx); Tài liệu tự học buổi 3 (mẫu API contract); Slide buổi 4.

## **2. Mô tả tổng quan**

### **2.1 Bối cảnh và nhu cầu thật**

| **Nguồn (case study)**       | **Phát biểu**                                                                                        | **Nhu cầu thật rút ra (hỏi “vì sao” hai lần)**                                                                       |
|------------------------------|------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| Anh Khoa – Marketing         | “Sau khi khách bảo hành xong, em muốn hỏi họ có hài lòng không, nhưng chưa có cách nào làm tự động.” | Cần biết mức hài lòng của **từng lần bảo hành** để chăm sóc kịp thời, không phụ thuộc việc khách tự lên mạng xã hội. |
| Ông Minh – Phó TGĐ           | “Nguyên nhân bảo hành nào nhiều nhất?…” và nhu cầu bức tranh về *chất lượng dịch vụ*                 | Cần so sánh chất lượng dịch vụ giữa các trung tâm và xu hướng theo tháng.                                            |
| Chị Trâm – Quản lý trung tâm | “Một màn hình thấy được… ai đang làm gì.”                                                            | Cần nhìn được chất lượng phục vụ theo **từng kỹ thuật viên** của trung tâm mình để hỗ trợ hoặc ghi nhận.             |
| Chị Lan – Tiếp nhận          | Mất 40 phút/ngày chép phiếu sang Excel                                                               | Báo cáo định kỳ phải xuất được trực tiếp từ hệ thống, không chép tay.                                                |

### **2.2 Các bên liên quan và actor**

| **Actor**                     | **Loại**                          | **Vai trò trong phân hệ**                                             | **Phạm vi dữ liệu (QT-14)**                      |
|-------------------------------|-----------------------------------|-----------------------------------------------------------------------|--------------------------------------------------|
| Khách hàng                    | Người dùng ngoài                  | Mở liên kết, chấm điểm, nhận xét                                      | Chỉ phiếu của chính mình (qua mã trong liên kết) |
| Quản lý trung tâm             | Người dùng nội bộ                 | Xem thống kê theo kỹ thuật viên, tra cứu phản hồi, xem tỉ lệ phản hồi | Trung tâm mình phụ trách                         |
| Nhân viên Marketing           | Người dùng nội bộ                 | Tra cứu, xuất Excel; hưởng lợi từ việc mỗi phiếu chỉ có một lời mời   | Toàn công ty (giả định A-05)                     |
| Ban giám đốc                  | Người dùng nội bộ                 | Xem tổng hợp theo trung tâm và tháng                                  | Toàn công ty                                     |
| Module Phiếu bảo hành (L2/L4) | Hệ thống nội bộ, ngoài phạm vi L8 | Phát sự kiện phiếu chuyển sang ĐÃ ĐÓNG                                | —                                                |
| Cổng gửi tin (SMS/Zalo)       | Hệ thống ngoài                    | Chuyển liên kết khảo sát tới điện thoại khách                         | —                                                |
| Bộ lập lịch                   | Thời gian                         | Kích hoạt nhắc lại khảo sát                                           | —                                                |

### **2.3 Ngoài phạm vi (WON’T trong học kỳ này)**

- **NPS (thang 0–10):** phiếu phạm vi có ghi “CSAT / NPS”, nhưng bảng survey_response của case study chỉ có thang 1–5. NPS cần thang và công thức khác (promoter/detractor) nên không làm; phân hệ chỉ thực hiện **CSAT**.

- Phân tích văn bản/cảm xúc của nhận xét; khảo sát cho đơn hàng mua mới; chiến dịch gửi khuyến mãi; sửa hoặc xóa phản hồi sau khi gửi.

### **2.4 Giả định và ràng buộc**

| **Mã** | **Nội dung**                                                                                                                                                                                  | **Trạng thái**   |
|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|
| A-01   | Module Phiếu bảo hành (L2/L4) đã có; L8 chỉ **đọc** ticket, ticket_status_log; dữ liệu mẫu sinh mô phỏng (tickets_history.csv, ticket_status_log.csv, survey_responses.csv, 200–500 bản ghi). | Xác nhận         |
| A-02   | Kênh gửi tin dùng cổng **giả lập** (ghi log) trong BT2; thiết kế để thay bằng SMS/Zalo thật.                                                                                                  | Xác nhận         |
| A-03   | Liên kết khảo sát hết hạn sau **7 ngày kể từ lúc tạo lời mời**; cấu hình được.                                                                                                                | Đề xuất – chờ GV |
| A-04   | Nhắc lại tối đa **một lần**, sau **72 giờ** kể từ lúc gửi.                                                                                                                                    | Đề xuất – chờ GV |
| A-05   | Nhân viên Marketing xem dữ liệu toàn công ty (QT-14 chưa nêu riêng Marketing).                                                                                                                | Đề xuất – chờ GV |
| A-06   | “Hài lòng” = điểm ≥ 4; “bất mãn” = điểm ≤ 2.                                                                                                                                                  | Đề xuất – chờ GV |
| A-07   | Kỹ thuật viên của một phản hồi = ticket.technician_id tại thời điểm phiếu đóng.                                                                                                               | Xác nhận         |
| C-01   | Ràng buộc dự án (ông Minh: “làm từng phần, phần nào xong dùng phần đó”): L8 triển khai độc lập, chỉ giao tiếp với L2/L4 qua dữ liệu phiếu.                                                    | Ràng buộc        |
| C-02   | Công nghệ: React.js (FE), Java Spring Boot (BE), MySQL (CSDL), GitHub, IntelliJ IDEA, Figma.                                                                                                  | Ràng buộc        |

## **3. Yêu cầu chức năng**

### **3.1 User Story và MoSCoW**

Khuôn: *Là \<vai trò\>, tôi muốn \<mục tiêu\> để \<giá trị\>.* Cột “Vấn đề” nối về Bảng 2.2 của case study.

| **Mã**  | **User Story**                                                                                                                                                                                                                                              | **Vấn đề** | **MoSCoW** | **Lý do**                                                                       |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|------------|---------------------------------------------------------------------------------|
| **US1** | Là **khách hàng**, tôi muốn **gửi điểm hài lòng (1–5) kèm nhận xét ngay trên liên kết khảo sát nhận được sau khi phiếu bảo hành đóng**, để **phản ánh trải nghiệm của mình mà không phải gọi điện hay đăng lên mạng xã hội**.                               | V7         | **MUST**   | Không có bước này thì không có dữ liệu hài lòng nào.                            |
| **US2** | Là **nhân viên Marketing**, tôi muốn **mỗi phiếu bảo hành sau khi đóng tự động phát sinh đúng một lời mời khảo sát**, để **số liệu hài lòng không bị trùng và khách không bị hỏi lặp**.                                                                     | V7, V5     | **MUST**   | Là quy tắc QT-10; sai ở đây thì toàn bộ số liệu sai.                            |
| **US3** | Là **quản lý trung tâm**, tôi muốn **xem bảng điểm hài lòng theo từng kỹ thuật viên thuộc trung tâm mình phụ trách**, để **biết kỹ thuật viên nào cần hỗ trợ hoặc ghi nhận kịp thời mà không phải chờ đến khi có khiếu nại**.                               | V7, V2     | **MUST**   | Giá trị phân tích đầu tiên và trực tiếp nhất của dữ liệu thu được.              |
| **US4** | Là **Ban giám đốc**, tôi muốn **xem tổng hợp CSAT, phân bố điểm 1–5 và xu hướng theo tháng của từng trung tâm**, để **so sánh chất lượng dịch vụ giữa các trung tâm và phát hiện trung tâm đang đi xuống**.                                                 | V4, V7     | SHOULD     | Quan trọng cho điều hành nhưng đọc cùng nguồn dữ liệu với US3; làm sau US1–US3. |
| **US5** | Là **nhân viên Marketing**, tôi muốn **xuất danh sách phản hồi đang lọc ra file Excel**, để **báo cáo định kỳ trong cuộc họp mà không phải chép tay**.                                                                                                      | V4         | SHOULD     | Tiết kiệm công sức lớn nhưng hệ thống vẫn dùng được nếu chưa có.                |
| **US6** | Là **quản lý trung tâm**, tôi muốn **tra cứu danh sách phản hồi của trung tâm mình theo kỹ thuật viên, khoảng thời gian và khoảng điểm (ví dụ chỉ điểm 1–2)**, để **đọc nhận xét chi tiết và liên hệ lại khách bất mãn trước khi họ đăng lên mạng xã hội**. | V7         | SHOULD     | Điểm số cần nhận xét đi kèm mới hành động được.                                 |
| **US7** | Là **quản lý trung tâm**, tôi muốn **xem tỉ lệ khách đã phản hồi trên số lời mời đã gửi của trung tâm mình theo tháng**, để **biết số liệu hài lòng có đủ đại diện để tin hay không**.                                                                      | V7         | COULD      | Hữu ích để đánh giá độ tin cậy, không chặn các story khác.                      |
| **US8** | Là **nhân viên Marketing**, tôi muốn **hệ thống tự nhắc một lần những khách chưa phản hồi sau 72 giờ**, để **tăng tỉ lệ phản hồi mà không phải gọi điện từng khách**.                                                                                       | V7         | COULD      | Cải thiện số lượng phản hồi; cần có US1–US2 chạy ổn trước.                      |

Không làm trong học kỳ này (WON’T): khảo sát NPS thang 0–10; phân tích văn bản nhận xét (xem 2.3).

### **3.1.1 Tiêu chí chấp nhận Given–When–Then (các story MUST)**

**US1 – Khách hàng gửi phản hồi khảo sát**

- **AC1.1 (luồng chính)**

  - *Given* phiếu bảo hành BH000231/2026 ở trạng thái ĐÃ ĐÓNG, khách đã nhận liên kết khảo sát còn hạn và chưa phản hồi,

  - *When* khách chọn điểm 4, nhập nhận xét “Kỹ thuật viên nhiệt tình, trả máy đúng hẹn” và bấm “Gửi phản hồi”,

  - *Then* hệ thống lưu đúng 1 phản hồi khảo sát (score = 4, comment, responded_at = thời điểm hiện tại), chuyển lời mời sang DA_PHAN_HOI và hiển thị lời cảm ơn.

- **AC1.2 (ngoại lệ – dữ liệu sai)**

  - *Given* khách đang mở một biểu mẫu khảo sát hợp lệ,

  - *When* khách bấm “Gửi phản hồi” khi chưa chọn điểm (hoặc nhận xét dài hơn 1000 ký tự),

  - *Then* hệ thống không lưu gì, báo lỗi tại trường sai (“Vui lòng chọn điểm từ 1 đến 5”) và giữ nguyên nội dung nhận xét đã nhập (HTTP 400).

- **AC1.3 (ngoại lệ – đã phản hồi hoặc hết hạn)**

  - *Given* phiếu đã có phản hồi, hoặc lời mời đã quá 7 ngày kể từ lúc tạo,

  - *When* khách mở lại liên kết hoặc bấm gửi lần nữa,

  - *Then* hệ thống hiển thị “Khảo sát này đã hoàn tất” hoặc “Liên kết đã hết hạn”, không lưu thêm phản hồi; số phản hồi của phiếu vẫn là 1 (HTTP 409 / 410).

**US2 – Mỗi phiếu đóng sinh đúng một lời mời**

- **AC2.1 (luồng chính)**

  - *Given* phiếu BH000231/2026 vừa chuyển sang ĐÃ ĐÓNG và chưa có lời mời khảo sát,

  - *When* hệ thống ghi nhận lần chuyển trạng thái đó,

  - *Then* hệ thống tạo đúng 1 lời mời (mã duy nhất, hạn 7 ngày, trạng thái CHO_GUI) và gửi liên kết tới số điện thoại của khách trong vòng 5 phút, trạng thái chuyển sang DA_GUI.

- **AC2.2 (ngoại lệ – trùng lời mời)**

  - *Given* phiếu BH000231/2026 đã có một lời mời khảo sát,

  - *When* hệ thống nhận lại yêu cầu tạo lời mời cho phiếu này (sự kiện đóng phiếu bị gửi lặp),

  - *Then* hệ thống không tạo lời mời mới, trả HTTP 409 và số lời mời của phiếu vẫn là 1.

- **AC2.3 (ngoại lệ – sai trạng thái)**

  - *Given* một phiếu ở trạng thái HOAN_TAT hoặc DA_HUY (chưa/không ĐÃ ĐÓNG),

  - *When* có yêu cầu tạo lời mời khảo sát cho phiếu đó,

  - *Then* hệ thống từ chối (HTTP 422, theo QT-10) và không tạo bản ghi nào.

**US3 – Quản lý trung tâm xem điểm theo kỹ thuật viên**

- **AC3.1 (luồng chính)**

  - *Given* quản lý Trung tâm Tân Bình đã đăng nhập; trung tâm có 3 kỹ thuật viên và 26 phản hồi trong tháng 09/2026,

  - *When* chị mở bảng điểm theo kỹ thuật viên và chọn kỳ 01/09/2026–30/09/2026,

  - *Then* bảng liệt kê từng kỹ thuật viên của trung tâm mình gồm số lời mời đã gửi, số phản hồi, CSAT (2 chữ số thập phân) và tỉ lệ hài lòng, mặc định sắp theo CSAT giảm dần.

- **AC3.2 (ngoại lệ – sai phạm vi đơn vị)**

  - *Given* quản lý Trung tâm Tân Bình đã đăng nhập,

  - *When* chị yêu cầu dữ liệu của một trung tâm khác (đổi center_id),

  - *Then* hệ thống từ chối (HTTP 403, theo QT-14) và không trả dữ liệu nào.

- **AC3.3 (ngoại lệ – chưa có dữ liệu)**

  - *Given* một kỹ thuật viên chưa có phản hồi nào trong kỳ đã chọn,

  - *When* quản lý xem bảng,

  - *Then* kỹ thuật viên đó vẫn hiện với số phản hồi 0 và CSAT hiển thị “—” (không hiển thị 0,00 gây hiểu nhầm).

Tổng: 9 tiêu chí GWT, trong đó 6 tiêu chí ngoại lệ (AC1.2, AC1.3, AC2.2, AC2.3, AC3.2, AC3.3).

### **3.1.2 Kiểm INVEST và bốn lỗi thường gặp**

| **Story** | **I** | **N** | **V** | **E** | **S** | **T** | **Ghi chú**                                                                        |
|-----------|-------|-------|-------|-------|-------|-------|------------------------------------------------------------------------------------|
| US1       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Test độc lập bằng lời mời được sinh sẵn trong dữ liệu mẫu, không cần chạy US2.     |
| US2       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Chỉ phụ thuộc dữ liệu phiếu của L2/L4 (A-01).                                      |
| US3       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Một màn hình + một API; test bằng dữ liệu mô phỏng.                                |
| US4       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Nếu quá lớn thì tách phần biểu đồ xu hướng thành story COULD.                      |
| US5       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | “Excel” là định dạng người dùng yêu cầu cho cuộc họp, không phải giải pháp áp đặt. |
| US6       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Bộ lọc điểm là tham số, không tạo story phụ thuộc.                                 |
| US7       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Tách riêng khỏi US3/US4 để mỗi story chỉ có một yêu cầu.                           |
| US8       | ✔     | ✔     | ✔     | ✔     | ✔     | ✔     | Dùng lại liên kết của lời mời cũ, không tạo khảo sát thứ hai (QT-10).              |

Bốn lỗi: (1) **mơ hồ** – đã gắn con số cụ thể (thang 1–5, điểm 1–2, 72 giờ, 7 ngày); (2) **không kiểm chứng được** – story MUST có tiêu chí GWT, story còn lại kiểm chứng qua FR/NFR; (3) **hai yêu cầu trong một câu** – đã tách tỉ lệ phản hồi (US7) khỏi bảng điểm (US3, US4); (4) **nêu giải pháp thay vì mục tiêu** – phần “để …” của mọi story nối về V7/V4/V5.

### **3.2 Use Case**

#### **3.2.1 Sơ đồ**

*Chú thích:* khung lớn là ranh giới hệ thống (phạm vi L8); actor nằm ngoài ranh giới (người: nền trắng; hệ thống/thời gian: nền xám); màu use case theo MoSCoW (xanh lá = MUST, xanh dương = SHOULD, vàng = COULD); đường nét liền là liên kết actor–use case; nét đứt có mũi tên là «include»/«extend». File gốc: docs/usecase-diagram.drawio.

#### **3.2.2 Danh sách use case**

| **Mã** | **Tên use case**                                                 | **Actor tham gia**                     | **Bên hưởng lợi**   | **Story** | **MoSCoW** |
|--------|------------------------------------------------------------------|----------------------------------------|---------------------|-----------|------------|
| UC01   | Tạo lời mời khảo sát cho phiếu bảo hành đã đóng                  | Module Phiếu bảo hành (kích hoạt)      | Nhân viên Marketing | US2       | MUST       |
| UC02   | Gửi liên kết khảo sát tới khách hàng (được UC01, UC04 «include») | Cổng gửi tin                           | Khách hàng          | US2       | MUST       |
| UC03   | **Gửi phản hồi khảo sát hài lòng**                               | Khách hàng                             | —                   | US1       | MUST       |
| UC04   | Nhắc lại khảo sát chưa phản hồi                                  | Bộ lập lịch                            | Nhân viên Marketing | US8       | COULD      |
| UC05   | Tra cứu danh sách phản hồi khảo sát                              | Quản lý trung tâm, Nhân viên Marketing | —                   | US6       | SHOULD     |
| UC06   | Xem điểm hài lòng theo kỹ thuật viên                             | Quản lý trung tâm                      | —                   | US3       | MUST       |
| UC07   | Xem tổng hợp CSAT theo trung tâm và tháng                        | Ban giám đốc                           | —                   | US4       | SHOULD     |
| UC08   | Xuất danh sách phản hồi ra file Excel («extend» UC05)            | Nhân viên Marketing                    | —                   | US5       | SHOULD     |
| UC09   | Xem tỉ lệ phản hồi khảo sát                                      | Quản lý trung tâm                      | —                   | US7       | COULD      |

UC01 do hệ thống kích hoạt thay mặt Nhân viên Marketing, nên Marketing là bên hưởng lợi chứ không tương tác trực tiếp.

#### **3.2.3 Đặc tả chi tiết UC03 – Gửi phản hồi khảo sát hài lòng**

*(Chọn UC03 vì đây là điểm kết thúc của phạm vi — phản hồi được lưu thành công — và chứa nhiều quy tắc nghiệp vụ, nhiều ngoại lệ nhất.)*

| **Mục**                    | **Nội dung**                                                                                                                                                              |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Mã / tên                   | UC03 – Gửi phản hồi khảo sát hài lòng                                                                                                                                     |
| Actor chính                | Khách hàng                                                                                                                                                                |
| Mô tả                      | Khách mở liên kết khảo sát nhận được sau khi phiếu bảo hành đóng, chấm điểm 1–5, nhập nhận xét (tùy chọn) và gửi; hệ thống lưu phản hồi và làm số liệu thống kê cập nhật. |
| Story / FR                 | US1 / FR-04, FR-05                                                                                                                                                        |
| Kích hoạt                  | Khách bấm vào liên kết khảo sát (vd: [https://crm.mekongmobile.vn/khao-sat/{token](https://crm.mekongmobile.vn/khao-sat/{token)})                                         |
| Tiền điều kiện             | \(1\) Phiếu bảo hành ở trạng thái ĐÃ ĐÓNG; (2) lời mời khảo sát tồn tại, trạng thái DA_GUI; (3) phiếu chưa có phản hồi; (4) chưa quá 7 ngày kể từ lúc tạo lời mời.        |
| Hậu điều kiện (thành công) | Có đúng 1 bản ghi survey_response của phiếu; lời mời ở trạng thái DA_PHAN_HOI; UC05–UC07 hiển thị phản hồi mới trong ≤ 60 giây (NFR-08).                                  |
| Hậu điều kiện (thất bại)   | Không có bản ghi mới; trạng thái lời mời không đổi; khách thấy thông báo lỗi, và được thử lại nếu là lỗi tạm thời.                                                        |
| Quy tắc áp dụng            | QT-10, QT-13, KS-04, KS-06                                                                                                                                                |

**Luồng chính**

| **Bước** | **Thực hiện bởi** | **Hành động**                                                                                                                |
|----------|-------------------|------------------------------------------------------------------------------------------------------------------------------|
| 1        | Khách hàng        | Bấm vào liên kết khảo sát.                                                                                                   |
| 2        | Hệ thống          | Kiểm tra liên kết: mã tồn tại, phiếu chưa có phản hồi, còn hạn, chưa vượt giới hạn truy cập.                                 |
| 3        | Hệ thống          | Hiển thị biểu mẫu: mã phiếu, tên trung tâm bảo hành, tên thiết bị, thang điểm 1–5, ô nhận xét (tùy chọn, tối đa 1000 ký tự). |
| 4        | Khách hàng        | Chọn điểm hài lòng từ 1 đến 5.                                                                                               |
| 5        | Khách hàng        | Nhập nhận xét (có thể bỏ qua).                                                                                               |
| 6        | Khách hàng        | Bấm “Gửi phản hồi”.                                                                                                          |
| 7        | Hệ thống          | Kiểm tra dữ liệu: điểm là số nguyên 1–5; nhận xét ≤ 1000 ký tự.                                                              |
| 8        | Hệ thống          | Lưu phản hồi và chuyển lời mời sang DA_PHAN_HOI trong **cùng một giao dịch**.                                                |
| 9        | Hệ thống          | Hiển thị trang cảm ơn kèm điểm khách đã chọn.                                                                                |
| 10       | Hệ thống          | Số liệu thống kê (UC05–UC07) phản ánh phản hồi mới. Use case kết thúc.                                                       |

**Luồng ngoại lệ (đánh số theo bước xảy ra)**

| **Mã** | **Tại bước** | **Điều kiện**                                      | **Xử lý của hệ thống**                                                                                                              | **Kết quả**                                 |
|--------|--------------|----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| 2a     | 2            | Mã trong liên kết không tồn tại hoặc sai định dạng | Hiển thị “Liên kết không hợp lệ” (HTTP 404)                                                                                         | Kết thúc, không lưu gì                      |
| 2b     | 2            | Phiếu đã có phản hồi (QT-10)                       | Hiển thị “Bạn đã gửi phản hồi ngày dd/mm/yyyy. Cảm ơn bạn!” (HTTP 409)                                                              | Kết thúc, không cho gửi lần hai             |
| 2c     | 2            | Liên kết quá 7 ngày kể từ lúc tạo lời mời          | Hiển thị “Liên kết đã hết hạn” (HTTP 410)                                                                                           | Kết thúc, không lưu gì                      |
| 2d     | 2            | Một địa chỉ IP gọi quá 10 yêu cầu/phút             | Hiển thị “Thao tác quá nhanh, vui lòng thử lại sau 1 phút” (HTTP 429)                                                               | Kết thúc, không lưu gì                      |
| 7a     | 7            | Chưa chọn điểm hoặc điểm ngoài 1–5                 | Báo lỗi tại trường điểm, giữ nội dung nhận xét đã nhập (HTTP 400)                                                                   | Quay lại bước 4                             |
| 7b     | 7            | Nhận xét dài hơn 1000 ký tự                        | Báo lỗi tại ô nhận xét, giữ nguyên nội dung (HTTP 400)                                                                              | Quay lại bước 5                             |
| 8a     | 8            | Lỗi cơ sở dữ liệu hoặc mất kết nối khi lưu         | Hoàn tác giao dịch, giữ nguyên trạng thái lời mời, hiển thị “Hệ thống tạm thời gặp sự cố, vui lòng thử lại” (HTTP 503), ghi log lỗi | Quay lại bước 6, dữ liệu trên form được giữ |
| 8b     | 8            | Hai yêu cầu gửi đồng thời (bấm đúp, hai tab)       | Ràng buộc duy nhất theo phiếu chặn bản ghi thứ hai; yêu cầu đến sau nhận thông báo như 2b (HTTP 409)                                | Kết thúc, vẫn chỉ có 1 phản hồi             |

### **3.3 Danh sách yêu cầu chức năng**

| **Mã** | **Yêu cầu**                                                                                                                                                                                                                                                                                                                            | **Quy tắc liên quan** |
|--------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| FR-01  | Khi phiếu bảo hành chuyển sang ĐÃ ĐÓNG, hệ thống phải tạo một lời mời khảo sát gồm mã duy nhất, trạng thái CHO_GUI, hạn dùng 7 ngày.                                                                                                                                                                                                   | QT-06, QT-10, KS-04   |
| FR-02  | Hệ thống phải từ chối tạo lời mời khi phiếu không ở trạng thái ĐÃ ĐÓNG (HTTP 422) hoặc đã có lời mời (HTTP 409).                                                                                                                                                                                                                       | QT-10                 |
| FR-03  | Hệ thống phải gửi liên kết khảo sát tới số điện thoại đã chuẩn hóa của khách qua kênh tin nhắn, ghi nhận kết quả (DA_GUI / GUI_LOI) và thử lại tối đa 3 lần, cách nhau 5 phút, khi cổng gửi tin lỗi.                                                                                                                                   | QT-02                 |
| FR-04  | Hệ thống phải hiển thị biểu mẫu khảo sát (mã phiếu, trung tâm, thiết bị, thang điểm 1–5, ô nhận xét) khi liên kết hợp lệ; nếu liên kết không hợp lệ, đã hoàn tất hoặc hết hạn thì hiển thị thông báo tương ứng.                                                                                                                        | QT-10                 |
| FR-05  | Hệ thống phải kiểm tra hợp lệ, lưu phản hồi (điểm, nhận xét, thời điểm) và đánh dấu lời mời DA_PHAN_HOI trong một giao dịch; mỗi phiếu tối đa một phản hồi; không cho sửa hoặc xóa phản hồi sau khi lưu.                                                                                                                               | QT-10, QT-13, KS-06   |
| FR-06  | Hệ thống phải hiển thị bảng thống kê theo kỹ thuật viên của một trung tâm trong kỳ chọn: số lời mời đã gửi, số phản hồi, CSAT, tỉ lệ hài lòng; kỹ thuật viên chưa có phản hồi hiển thị “—”.                                                                                                                                            | KS-01, KS-02, A-07    |
| FR-07  | Hệ thống phải hiển thị tổng hợp theo trung tâm và theo tháng: số phản hồi, CSAT, phân bố điểm 1–5, tỉ lệ hài lòng, và xu hướng giữa các tháng.                                                                                                                                                                                         | KS-01, KS-02          |
| FR-08  | Hệ thống phải cho tra cứu danh sách phản hồi, lọc theo trung tâm, kỹ thuật viên, khoảng thời gian, khoảng điểm; có phân trang; số điện thoại khách hiển thị dạng che.                                                                                                                                                                  | QT-15                 |
| FR-09  | Hệ thống phải xuất danh sách phản hồi theo đúng bộ lọc đang áp dụng ra file .xlsx (tối đa 10.000 dòng).                                                                                                                                                                                                                                | QT-15                 |
| FR-10  | Hệ thống phải tính tỉ lệ phản hồi theo trung tâm và theo tháng.                                                                                                                                                                                                                                                                        | KS-03                 |
| FR-11  | Hệ thống phải tự nhắc lại **một lần** sau 72 giờ cho lời mời đã gửi nhưng chưa phản hồi và chưa hết hạn, dùng lại cùng liên kết (không tạo lời mời mới).                                                                                                                                                                               | QT-10, KS-05          |
| FR-12  | Hệ thống phải giới hạn dữ liệu theo vai trò: mỗi chức năng chỉ mở cho đúng vai trò của use case tương ứng; Quản lý trung tâm chỉ xem trung tâm mình phụ trách; Ban giám đốc và Nhân viên Marketing xem toàn công ty; người chưa đăng nhập không truy cập được thống kê; số điện thoại che với mọi vai trò trừ Quản lý và Ban giám đốc. | QT-14, QT-15          |

## **4. Yêu cầu phi chức năng (có ngưỡng đo được)**

| **Mã** | **Loại**                  | **Yêu cầu (ngưỡng)**                                                                                                                                                                                          | **Cách kiểm chứng**                                            | **FR liên quan**    |
|--------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|---------------------|
| NFR-01 | Hiệu năng                 | Biểu mẫu khảo sát tải xong trong **≤ 2,0 giây** ở phân vị 95 (p95) với 100 người truy cập đồng thời, kết nối 4G.                                                                                              | Kiểm thử tải (k6/JMeter)                                       | FR-04               |
| NFR-02 | Hiệu năng                 | API lưu phản hồi trả kết quả trong **≤ 500 ms** ở p95 với 50 yêu cầu đồng thời.                                                                                                                               | Kiểm thử tải                                                   | FR-05               |
| NFR-03 | Hiệu năng                 | Màn hình thống kê và tra cứu trả kết quả trong **≤ 2,0 giây** ở p95 trên tập **10.000 phản hồi**.                                                                                                             | Sinh dữ liệu mô phỏng 10.000 dòng (có seed), đo thời gian      | FR-06…FR-10         |
| NFR-04 | Bảo mật                   | Mã khảo sát sinh bằng bộ sinh số ngẫu nhiên an toàn, **≥ 256 bit** (43 ký tự base64url), hết hạn sau **7 ngày**; một địa chỉ IP bị chặn (HTTP 429) khi gọi **\> 10 yêu cầu/phút** vào /api/surveys/\*\*.      | Kiểm thử đơn vị độ dài/độ ngẫu nhiên; kiểm thử gọi 11 lần/phút | FR-04, FR-05, FR-11 |
| NFR-05 | Toàn vẹn dữ liệu          | **100%** phiếu có tối đa 1 lời mời và tối đa 1 phản hồi, bảo đảm bằng ràng buộc duy nhất ở CSDL; gửi **100 yêu cầu đồng thời** cùng một mã thì đúng **1** bản ghi được tạo, 99 yêu cầu còn lại nhận HTTP 409. | Kiểm thử đồng thời                                             | FR-02, FR-05        |
| NFR-06 | Kịp thời                  | **≥ 99%** lời mời được gửi trong **≤ 5 phút** kể từ lúc phiếu chuyển sang ĐÃ ĐÓNG (không tính sự cố từ cổng gửi tin).                                                                                         | So created_at/sent_at trên 500 phiếu mô phỏng                  | FR-01, FR-03        |
| NFR-07 | Khả dụng                  | Biểu mẫu hiển thị đúng trên màn hình rộng **≥ 360 px**; **5/5** người thử hoàn thành khảo sát (chấm điểm + gửi) trong **≤ 30 giây** và **≤ 3 thao tác chạm**.                                                 | Thử nghiệm người dùng 5 người                                  | FR-04, FR-05        |
| NFR-08 | Tính cập nhật của số liệu | Phản hồi mới xuất hiện trong bảng thống kê và danh sách tra cứu trong **≤ 60 giây** sau khi lưu.                                                                                                              | Kiểm thử tích hợp (gửi phản hồi → truy vấn thống kê)           | FR-05…FR-08         |

## **5. Quy tắc nghiệp vụ**

### **5.1 Quy tắc từ case study áp dụng cho L8**

| **Mã**    | **Quy tắc (rút gọn)**                                                                               | **Cách áp dụng trong L8**                                                                                                                               | **FR**                |
|-----------|-----------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| **QT-10** | Chỉ gửi khảo sát cho phiếu ở trạng thái ĐÃ ĐÓNG, mỗi phiếu chỉ khảo sát một lần.                    | Kiểm tra trạng thái khi tạo lời mời; ràng buộc duy nhất theo phiếu cho survey_invitation và survey_response.                                            | FR-01, 02, 04, 05, 11 |
| QT-06     | Phiếu chỉ chuyển trạng thái theo vòng đời; mọi lần chuyển được ghi vào ticket_status_log.           | Dấu hiệu kích hoạt UC01 là bản ghi to_status = DA_DONG trong ticket_status_log; phiếu không quay lại trạng thái trước nên điều kiện không bị đảo ngược. | FR-01                 |
| QT-02     | Số điện thoại chuẩn hóa về 10 chữ số bắt đầu bằng 0.                                                | Chỉ gửi tới số đã chuẩn hóa; số sai dạng → lời mời GUI_LOI.                                                                                             | FR-03                 |
| QT-13     | Không xóa vật lý phiếu, đơn hàng, hồ sơ khách.                                                      | Áp dụng cho cả lời mời và phản hồi: không có chức năng xóa; phản hồi bất biến sau khi gửi.                                                              | FR-05                 |
| QT-14     | Nhân viên chỉ xem dữ liệu đơn vị mình; quản lý xem đơn vị phụ trách; Ban giám đốc xem toàn công ty. | Lọc dữ liệu theo vai trò và center_id ở tầng dịch vụ.                                                                                                   | FR-12                 |
| QT-15     | Số điện thoại hiển thị dạng che (ví dụ 090\*\*\*\*567) với mọi vai trò trừ Quản lý và Ban giám đốc. | Áp dụng ở danh sách phản hồi và file Excel.                                                                                                             | FR-08, 09, 12         |

### **5.2 Quy tắc riêng của L8 (đề xuất )**

| **Mã** | **Quy tắc**                                                                                                                                |
|--------|--------------------------------------------------------------------------------------------------------------------------------------------|
| KS-01  | CSAT = trung bình cộng score của nhóm phản hồi, làm tròn 2 chữ số thập phân. Nhóm rỗng → không có giá trị (hiển thị “—”), không tính là 0. |
| KS-02  | Tỉ lệ hài lòng = số phản hồi có score ≥ 4 ÷ tổng số phản hồi của nhóm.                                                                     |
| KS-03  | Tỉ lệ phản hồi = số phản hồi ÷ số lời mời có trạng thái DA_GUI hoặc DA_PHAN_HOI (không tính GUI_LOI).                                      |
| KS-04  | Liên kết khảo sát hết hạn sau 7 ngày kể từ lúc tạo lời mời.                                                                                |
| KS-05  | Nhắc lại tối đa 1 lần, sau 72 giờ kể từ lúc gửi thành công, cùng một liên kết.                                                             |
| KS-06  | Phản hồi không được sửa sau khi lưu; khách không gửi lại bằng liên kết đã dùng.                                                            |

## **6. Bảng truy vết**

### **6.1 FR ↔ User Story ↔ Use Case ↔ MoSCoW**

| **FR** | **Tóm tắt yêu cầu**                        | **User Story** | **Use Case**     | **MoSCoW** | **NFR liên quan**      |
|--------|--------------------------------------------|----------------|------------------|------------|------------------------|
| FR-01  | Tạo lời mời khi phiếu ĐÃ ĐÓNG              | US2            | UC01             | MUST       | NFR-06                 |
| FR-02  | Chặn lời mời trùng / sai trạng thái        | US2            | UC01             | MUST       | NFR-05                 |
| FR-03  | Gửi liên kết, ghi nhận kết quả, thử lại    | US2            | UC02             | MUST       | NFR-06                 |
| FR-04  | Hiển thị biểu mẫu khảo sát                 | US1            | UC03             | MUST       | NFR-01, 04, 07         |
| FR-05  | Kiểm tra hợp lệ và lưu phản hồi            | US1            | UC03             | MUST       | NFR-02, 04, 05, 07, 08 |
| FR-06  | Bảng điểm theo kỹ thuật viên               | US3            | UC06             | MUST       | NFR-03, 08             |
| FR-07  | Tổng hợp theo trung tâm và tháng           | US4            | UC07             | SHOULD     | NFR-03, 08             |
| FR-08  | Tra cứu danh sách phản hồi có lọc          | US6            | UC05             | SHOULD     | NFR-03, 08             |
| FR-09  | Xuất danh sách ra Excel                    | US5            | UC08             | SHOULD     | NFR-03                 |
| FR-10  | Tỉ lệ phản hồi theo trung tâm và tháng     | US7            | UC09             | COULD      | NFR-03                 |
| FR-11  | Nhắc lại một lần sau 72 giờ                | US8            | UC04             | COULD      | NFR-04                 |
| FR-12  | Phân quyền theo vai trò, che số điện thoại | US3, US4, US6  | UC05, UC06, UC07 | MUST       | —                      |

### **6.2 Kiểm tra ngược: mỗi User Story đều có yêu cầu và use case**

| **User Story** | **MoSCoW** | **FR**              | **Use Case** | **Endpoint (xem api-contract.md)**                                              |
|----------------|------------|---------------------|--------------|---------------------------------------------------------------------------------|
| US1            | MUST       | FR-04, FR-05        | UC03         | GET /api/surveys/{token}, POST /api/surveys/{token}/responses                   |
| US2            | MUST       | FR-01, FR-02, FR-03 | UC01, UC02   | POST /api/survey-invitations, POST /api/survey-invitations/{invitation_id}/send |
| US3            | MUST       | FR-06, FR-12        | UC06         | GET /api/survey-stats/by-technician                                             |
| US4            | SHOULD     | FR-07, FR-12        | UC07         | GET /api/survey-stats/by-center                                                 |
| US5            | SHOULD     | FR-09               | UC08         | GET /api/survey-responses/export                                                |
| US6            | SHOULD     | FR-08, FR-12        | UC05         | GET /api/survey-responses                                                       |
| US7            | COULD      | FR-10               | UC09         | GET /api/survey-stats/response-rate                                             |
| US8            | COULD      | FR-11               | UC04         | Tác vụ nền của hệ thống, không có API công khai                                 |
