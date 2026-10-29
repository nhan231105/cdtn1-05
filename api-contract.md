# **API (API contract) – Track SE**

## **Phân hệ Khảo sát hài lòng CSAT sau bảo hành (L8) – Smart CRM, Mekong Mobile**

Sinh viên: Võ Nguyễn Thành Nhân – MSSV 2374802010360 · Phiên bản 1.0 – 06/10/2026

Đi kèm docs/srs.md. Thuật ngữ, mã US/UC/FR/NFR/QT/KS dùng đúng như trong SRS.

## **1. Danh sách endpoint**

| **\#** | **Phương thức** | **Đường dẫn**                                                                               | **Mục đích**                                     | **User Story** | **FR**       | **MoSCoW** | **Ai gọi**                             |
|--------|-----------------|---------------------------------------------------------------------------------------------|--------------------------------------------------|----------------|--------------|------------|----------------------------------------|
| 1      | POST            | /api/survey-invitations                                                                     | Tạo lời mời khảo sát cho phiếu bảo hành đã đóng  | US2            | FR-01, FR-02 | MUST       | Hệ thống (Module Phiếu bảo hành)       |
| 2      | POST            | /api/survey-invitations/{invitation_id}/send                                                | Gửi (hoặc gửi lại) liên kết khảo sát tới khách   | US2            | FR-03        | MUST       | Hệ thống (tác vụ gửi tin)              |
| 3      | GET             | /api/surveys/{token}                                                                        | Lấy thông tin để hiển thị biểu mẫu khảo sát      | US1            | FR-04        | MUST       | Khách hàng (công khai theo mã)         |
| 4      | POST            | /api/surveys/{token}/responses                                                              | Gửi phản hồi khảo sát                            | US1            | FR-05        | MUST       | Khách hàng (công khai theo mã)         |
| 5      | GET             | /api/survey-stats/by-technician?center_id=&from=&to=                                        | Bảng điểm hài lòng theo kỹ thuật viên            | US3            | FR-06, FR-12 | MUST       | Quản lý trung tâm                      |
| 6      | GET             | /api/survey-stats/by-center?center_id=&from=&to=&group_by=month                             | Tổng hợp CSAT theo trung tâm và tháng            | US4            | FR-07, FR-12 | SHOULD     | Ban giám đốc                           |
| 7      | GET             | /api/survey-responses?center_id=&technician_id=&from=&to=&score_min=&score_max=&page=&size= | Tra cứu danh sách phản hồi, có lọc và phân trang | US6            | FR-08, FR-12 | SHOULD     | Quản lý trung tâm, Nhân viên Marketing |
| 8      | GET             | /api/survey-responses/export?... (cùng bộ lọc endpoint 7)                                   | Xuất danh sách phản hồi ra file .xlsx            | US5            | FR-09        | SHOULD     | Marketing                              |
| 9      | GET             | /api/survey-stats/response-rate?center_id=&from=&to=                                        | Tỉ lệ phản hồi theo trung tâm và tháng           | US7            | FR-10        | COULD      | Quản lý trung tâm                      |

US8 (nhắc lại sau 72 giờ) là tác vụ nền của hệ thống, không có API công khai.

## **2. Quy ước chung**

- **Định dạng:** JSON, UTF-8. Header bắt buộc Content-Type: application/json (trừ endpoint 8 trả file).

- **Tên trường:** snake_case, khớp tên cột trong cơ sở dữ liệu.

- **Thời gian:** ISO 8601 kèm múi giờ, ví dụ 2026-09-08T14:30:00+07:00. Tham số ngày (from, to) dạng yyyy-MM-dd, tính gồm cả hai đầu mút, theo giờ Việt Nam.

- **Điểm và tỉ lệ:** score là số nguyên 1–5; csat_avg làm tròn 2 chữ số thập phân, null khi nhóm không có phản hồi (KS-01); satisfied_rate, response_rate là số thập phân 0–1 làm tròn 2 chữ số (KS-02, KS-03).

- **Phân trang:** page (từ 1) và size (mặc định 20, tối đa 100); response danh sách kèm total.

- **Số điện thoại:** luôn trả dạng che 090\*\*\*\*567 (QT-15). Không có endpoint nào trả số điện thoại đầy đủ.

- **Cấu trúc lỗi (mọi lỗi dùng chung):**

{ "error": { "code": "VALIDATION_FAILED", "message": "Dữ liệu không hợp lệ", "fields": { "score": "Vui lòng chọn điểm từ 1 đến 5" } } }

- **Mã lỗi nghiệp vụ dùng trong tài liệu này:** VALIDATION_FAILED, UNAUTHENTICATED, FORBIDDEN_SCOPE, TICKET_NOT_FOUND, INVITATION_NOT_FOUND, SURVEY_NOT_FOUND, CENTER_NOT_FOUND, INVITATION_ALREADY_EXISTS, TICKET_NOT_CLOSED, ALREADY_RESPONDED, SURVEY_EXPIRED, INVITATION_CLOSED, INVALID_PHONE, RATE_LIMITED, SMS_GATEWAY_ERROR, EXPORT_TOO_LARGE, SERVICE_UNAVAILABLE.

## **3. Xác thực và phân quyền**

| **Cơ chế**                          | **Dùng cho**  | **Chi tiết**                                                                                                      |
|-------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------|
| Mã khảo sát (token) trong đường dẫn | Endpoint 3, 4 | Mã ngẫu nhiên ≥ 256 bit, hết hạn 7 ngày, chống dò bằng giới hạn 10 yêu cầu/phút/IP (NFR-04). Không cần đăng nhập. |
| Khóa dịch vụ X-Service-Key          | Endpoint 1, 2 | Chỉ Module Phiếu bảo hành và tác vụ nền nội bộ biết khóa.                                                         |
| JWT Authorization: Bearer \<token\> | Endpoint 5–9  | Chứa vai trò và center_id (với Quản lý trung tâm).                                                                |

| **Endpoint** | **Khách hàng (mã)** | **Hệ thống (khóa dịch vụ)** | **Quản lý trung tâm**  | **Nhân viên Marketing** | **Ban giám đốc** |
|--------------|---------------------|-----------------------------|------------------------|-------------------------|------------------|
| 1, 2         | —                   | ✔                           | —                      | —                       | —                |
| 3, 4         | ✔                   | —                           | —                      | —                       | —                |
| 5            | —                   | —                           | ✔ (chỉ trung tâm mình) | —                       | —                |
| 6            | —                   | —                           | —                      | —                       | ✔ (toàn công ty) |
| 7            | —                   | —                           | ✔ (chỉ trung tâm mình) | ✔ (toàn công ty, A-05)  | —                |
| 8            | —                   | —                           | —                      | ✔                       | —                |
| 9            | —                   | —                           | ✔ (chỉ trung tâm mình) | —                       | —                |

Vi phạm phạm vi → 403 FORBIDDEN_SCOPE (QT-14). Chưa đăng nhập → 401 UNAUTHENTICATED.

## **4. Bảng dữ liệu liên quan (để đối chiếu trường request/response)**

Kiểu viết theo MySQL (quy đổi từ PostgreSQL của case study: BIGSERIAL → BIGINT AUTO_INCREMENT, TIMESTAMP giữ nguyên; ràng buộc CHECK cần MySQL ≥ 8.0.16).

**survey_invitation** *(bảng bổ sung của L8)*

| **Cột**       | **Kiểu**              | **Ràng buộc**                     | **Ý nghĩa**                              |
|---------------|-----------------------|-----------------------------------|------------------------------------------|
| invitation_id | BIGINT AUTO_INCREMENT | PK                                | Khóa chính                               |
| ticket_id     | BIGINT                | FK → ticket, NOT NULL, **UNIQUE** | Mỗi phiếu tối đa một lời mời (QT-10)     |
| token         | CHAR(43)              | NOT NULL, UNIQUE                  | Mã trong liên kết khảo sát (base64url)   |
| status        | VARCHAR(20)           | NOT NULL                          | CHO_GUI / DA_GUI / GUI_LOI / DA_PHAN_HOI |
| channel       | VARCHAR(10)           | NOT NULL, mặc định SMS            | SMS / ZALO                               |
| send_attempts | SMALLINT              | NOT NULL, mặc định 0              | Số lần đã thử gửi (tối đa 3)             |
| created_at    | TIMESTAMP             | NOT NULL                          | Lúc tạo lời mời                          |
| sent_at       | TIMESTAMP             | NULL được                         | Lúc gửi thành công                       |
| reminded_at   | TIMESTAMP             | NULL được                         | Lúc nhắc lại (tối đa một lần, KS-05)     |
| expires_at    | TIMESTAMP             | NOT NULL                          | = created_at + 7 ngày (KS-04)            |

**survey_response** *(theo Mục 8 case study, giữ nguyên cột)*: response_id (PK), ticket_id (FK → ticket, NOT NULL, **UNIQUE**), score (SMALLINT, NOT NULL, 1 ≤ score ≤ 5), comment (TEXT, NULL được; ứng dụng giới hạn ≤ 1000 ký tự), responded_at (TIMESTAMP, NOT NULL).

**Bảng chỉ đọc (do L2/L4 quản lý):** ticket (ticket_id, ticket_code, customer_id, device_id, center_id, technician_id, status), ticket_status_log (to_status, changed_at), technician, service_center, customer (phone), device.

## **5. Chi tiết endpoint cho các story MUST**

### **5.1 POST /api/survey-invitations · Tạo lời mời khảo sát · US2**

**Request body**

{ "ticket_id": 88231 }

**201 Created**

{  
"invitation_id": 5012,  
"ticket_id": 88231,  
"ticket_code": "BH000231/2026",  
"status": "CHO_GUI",  
"survey_url": "<https://crm.mekongmobile.vn/khao-sat/q8Zk3vT1nWc0Yb7XhJ2mLpR9sD4eFgA6uIoKtVxNzBE>",  
"created_at": "2026-09-08T14:30:00+07:00",  
"expires_at": "2026-09-15T14:30:00+07:00"  
}

| **Mã** | **Khi nào**                                                                          | **Body lỗi (error.code)** |
|--------|--------------------------------------------------------------------------------------|---------------------------|
| 400    | ticket_id thiếu hoặc không phải số nguyên dương                                      | VALIDATION_FAILED         |
| 401    | Thiếu hoặc sai X-Service-Key                                                         | UNAUTHENTICATED           |
| 404    | Không có phiếu với ticket_id                                                         | TICKET_NOT_FOUND          |
| 409    | Phiếu đã có lời mời (kèm existing_invitation_id)                                     | INVITATION_ALREADY_EXISTS |
| 422    | Phiếu không ở trạng thái ĐÃ ĐÓNG (QT-10); fields.status cho biết trạng thái hiện tại | TICKET_NOT_CLOSED         |

Ví dụ 422:

{ "error": { "code": "TICKET_NOT_CLOSED", "message": "Chỉ gửi khảo sát cho phiếu ở trạng thái ĐÃ ĐÓNG", "fields": { "status": "HOAN_TAT" } } }

### **5.2 POST /api/survey-invitations/{invitation_id}/send · Gửi liên kết khảo sát · US2**

**Request body** (tùy chọn; mặc định kênh SMS)

{ "channel": "SMS" }

**200 OK**

{ "invitation_id": 5012, "status": "DA_GUI", "channel": "SMS", "sent_at": "2026-09-08T14:31:02+07:00", "send_attempts": 1 }

| **Mã** | **Khi nào**                                                                       | **error.code**       |
|--------|-----------------------------------------------------------------------------------|----------------------|
| 400    | channel không thuộc SMS / ZALO                                                    | VALIDATION_FAILED    |
| 404    | Không có lời mời                                                                  | INVITATION_NOT_FOUND |
| 409    | Lời mời đã DA_PHAN_HOI hoặc đã hết hạn nên không gửi nữa                          | INVITATION_CLOSED    |
| 422    | Số điện thoại của khách không đúng dạng chuẩn QT-02; lời mời chuyển GUI_LOI       | INVALID_PHONE        |
| 502    | Cổng gửi tin lỗi; hệ thống thử lại tối đa 3 lần cách 5 phút, hết lượt thì GUI_LOI | SMS_GATEWAY_ERROR    |

### **5.3 GET /api/surveys/{token} · Lấy thông tin biểu mẫu khảo sát · US1**

**200 OK**

{  
"ticket_code": "BH000231/2026",  
"center_name": "Trung tâm bảo hành Quận Tân Bình",  
"device_name": "iPhone 13 128GB",  
"closed_at": "2026-09-08T14:30:00+07:00",  
"expires_at": "2026-09-15T14:30:00+07:00",  
"score_min": 1,  
"score_max": 5,  
"comment_max_length": 1000  
}

| **Mã** | **Khi nào**                                             | **error.code**    |
|--------|---------------------------------------------------------|-------------------|
| 404    | Mã không tồn tại hoặc sai định dạng (UC03 – 2a)         | SURVEY_NOT_FOUND  |
| 409    | Phiếu đã có phản hồi; body kèm responded_at (UC03 – 2b) | ALREADY_RESPONDED |
| 410    | Quá 7 ngày kể từ lúc tạo lời mời (UC03 – 2c)            | SURVEY_EXPIRED    |
| 429    | Quá 10 yêu cầu/phút/IP (UC03 – 2d)                      | RATE_LIMITED      |

### **5.4 POST /api/surveys/{token}/responses · Gửi phản hồi khảo sát · US1**

**Request body**

{ "score": 4, "comment": "Kỹ thuật viên nhiệt tình, trả máy đúng hẹn" }

**201 Created**

{  
"response_id": 7305,  
"ticket_code": "BH000231/2026",  
"score": 4,  
"comment": "Kỹ thuật viên nhiệt tình, trả máy đúng hẹn",  
"responded_at": "2026-09-08T19:42:10+07:00",  
"message": "Cảm ơn bạn đã phản hồi"  
}

| **Mã** | **Khi nào**                                                                             | **error.code**      |
|--------|-----------------------------------------------------------------------------------------|---------------------|
| 400    | score thiếu, không phải số nguyên hoặc ngoài 1–5; comment \> 1000 ký tự (UC03 – 7a, 7b) | VALIDATION_FAILED   |
| 404    | Mã không tồn tại (2a)                                                                   | SURVEY_NOT_FOUND    |
| 409    | Đã có phản hồi, kể cả khi hai yêu cầu đến đồng thời (2b, 8b)                            | ALREADY_RESPONDED   |
| 410    | Liên kết hết hạn (2c)                                                                   | SURVEY_EXPIRED      |
| 429    | Quá 10 yêu cầu/phút/IP (2d)                                                             | RATE_LIMITED        |
| 503    | Lỗi cơ sở dữ liệu, giao dịch đã hoàn tác; khách thử lại được (8a)                       | SERVICE_UNAVAILABLE |

Ví dụ 400:

{ "error": { "code": "VALIDATION_FAILED", "message": "Dữ liệu không hợp lệ", "fields": { "score": "Vui lòng chọn điểm từ 1 đến 5" } } }

### **5.5 GET /api/survey-stats/by-technician?center_id=2&from=2026-09-01&to=2026-09-30 · Bảng điểm theo kỹ thuật viên · US3**

center_id không bắt buộc với Quản lý trung tâm (mặc định là trung tâm của người đăng nhập; nếu có thì phải trùng). from, to mặc định là tháng hiện tại. Mặc định sắp theo csat_avg giảm dần, kỹ thuật viên chưa có phản hồi xếp cuối.

**200 OK**

{  
"center_id": 2,  
"center_name": "Trung tâm bảo hành Quận Tân Bình",  
"from": "2026-09-01",  
"to": "2026-09-30",  
"items": \[  
{ "technician_id": 12, "technician_name": "Lê Thị Hoa", "invitations_sent": 15, "responses": 12, "csat_avg": 4.58, "satisfied_rate": 0.92 },  
{ "technician_id": 7, "technician_name": "Nguyễn Văn Dũng", "invitations_sent": 18, "responses": 14, "csat_avg": 4.36, "satisfied_rate": 0.86 },  
{ "technician_id": 15, "technician_name": "Phạm Quốc Bảo", "invitations_sent": 9, "responses": 0, "csat_avg": null, "satisfied_rate": null }  
\]  
}

| **Mã** | **Khi nào**                                                                                 | **error.code**    |
|--------|---------------------------------------------------------------------------------------------|-------------------|
| 400    | from \> to, sai định dạng ngày, hoặc khoảng \> 366 ngày                                     | VALIDATION_FAILED |
| 401    | Chưa đăng nhập                                                                              | UNAUTHENTICATED   |
| 403    | Quản lý trung tâm yêu cầu trung tâm khác, hoặc vai trò không phải Quản lý trung tâm (QT-14) | FORBIDDEN_SCOPE   |
| 404    | center_id không tồn tại                                                                     | CENTER_NOT_FOUND  |

## **6. Chi tiết rút gọn cho story SHOULD / COULD**

### **6.1 GET /api/survey-stats/by-center?from=2026-07-01&to=2026-09-30&group_by=month · US4**

**200 OK** (rút gọn; distribution là số phản hồi theo từng mức điểm)

{  
"from": "2026-07-01", "to": "2026-09-30", "group_by": "month",  
"items": \[  
{ "center_id": 2, "center_name": "Trung tâm bảo hành Quận Tân Bình", "month": "2026-07", "responses": 38, "csat_avg": 4.13, "satisfied_rate": 0.82, "distribution": { "1": 1, "2": 3, "3": 3, "4": 14, "5": 17 } },  
{ "center_id": 2, "center_name": "Trung tâm bảo hành Quận Tân Bình", "month": "2026-08", "responses": 41, "csat_avg": 4.05, "satisfied_rate": 0.78, "distribution": { "1": 2, "2": 2, "3": 5, "4": 15, "5": 17 } },  
{ "center_id": 2, "center_name": "Trung tâm bảo hành Quận Tân Bình", "month": "2026-09", "responses": 26, "csat_avg": 4.46, "satisfied_rate": 0.88, "distribution": { "1": 0, "2": 1, "3": 2, "4": 7, "5": 16 } }  
\]  
}

Lỗi: **400** VALIDATION_FAILED (khoảng ngày sai, group_by khác month); **401** UNAUTHENTICATED; **403** FORBIDDEN_SCOPE (vai trò không phải Ban giám đốc).

### **6.2 GET /api/survey-responses?center_id=2&score_max=2&from=2026-09-01&to=2026-09-30&page=1&size=20 · US6**

**200 OK**

{  
"page": 1, "size": 20, "total": 1,  
"items": \[  
{  
"response_id": 7301,  
"ticket_code": "BH000198/2026",  
"center_name": "Trung tâm bảo hành Quận Tân Bình",  
"technician_name": "Nguyễn Văn Dũng",  
"score": 2,  
"comment": "Máy sửa xong nhưng trả trễ hẹn một ngày",  
"customer_phone": "090\*\*\*\*567",  
"responded_at": "2026-09-10T09:12:44+07:00"  
}  
\]  
}

Lỗi: **400** (score_min \> score_max, điểm ngoài 1–5, size \> 100, khoảng ngày sai); **401**; **403** (khác trung tâm).

### **6.3 GET /api/survey-responses/export?center_id=2&from=2026-09-01&to=2026-09-30 · US5**

**200 OK** – file nhị phân, header Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet và Content-Disposition: attachment; filename="phan-hoi-khao-sat_2026-09.xlsx". Cột: Mã phiếu, Trung tâm, Kỹ thuật viên, Điểm, Nhận xét, Số điện thoại (che), Thời điểm phản hồi. Lỗi: **400** (bộ lọc sai); **403** (không phải Nhân viên Marketing); **422** EXPORT_TOO_LARGE (kết quả \> 10.000 dòng, yêu cầu thu hẹp bộ lọc).

### **6.4 GET /api/survey-stats/response-rate?center_id=2&from=2026-09-01&to=2026-09-30 · US7**

**200 OK**

{ "items": \[ { "center_id": 2, "month": "2026-09", "invitations_sent": 42, "responses": 26, "response_rate": 0.62 } \] }

Lỗi: **400** (khoảng ngày sai); **401**; **403** (khác trung tâm).

## **7. Bảng validation**

| **Trường**           | **Ở đâu**      | **Bắt buộc** | **Kiểu / ràng buộc**                                                         | **Thông báo lỗi khi vi phạm**                                 |
|----------------------|----------------|--------------|------------------------------------------------------------------------------|---------------------------------------------------------------|
| ticket_id            | Body (EP1)     | Có           | Số nguyên dương; phải tồn tại trong ticket                                   | “Không tìm thấy phiếu bảo hành”                               |
| invitation_id        | Path (EP2)     | Có           | Số nguyên dương; phải tồn tại                                                | “Không tìm thấy lời mời khảo sát”                             |
| channel              | Body (EP2)     | Không        | SMS hoặc ZALO; mặc định SMS                                                  | “Kênh gửi không hợp lệ”                                       |
| token                | Path (EP3, 4)  | Có           | Chuỗi 43 ký tự thuộc \[A-Za-z0-9\_-\]                                        | “Liên kết không hợp lệ”                                       |
| score                | Body (EP4)     | Có           | Số nguyên, 1 ≤ score ≤ 5                                                     | “Vui lòng chọn điểm từ 1 đến 5”                               |
| comment              | Body (EP4)     | Không        | Chuỗi 0–1000 ký tự; cắt khoảng trắng hai đầu; loại bỏ thẻ HTML trước khi lưu | “Nhận xét tối đa 1000 ký tự”                                  |
| center_id            | Query (EP5–9)  | Tùy vai trò  | Số nguyên dương; phải tồn tại; Quản lý trung tâm phải trùng trung tâm mình   | “Trung tâm không hợp lệ” / “Không có quyền xem trung tâm này” |
| technician_id        | Query (EP7)    | Không        | Số nguyên dương; phải thuộc center_id đã chọn                                | “Kỹ thuật viên không thuộc trung tâm”                         |
| from, to             | Query (EP5–9)  | Không        | yyyy-MM-dd; from ≤ to; khoảng ≤ 366 ngày; mặc định tháng hiện tại            | “Khoảng thời gian không hợp lệ”                               |
| score_min, score_max | Query (EP7, 8) | Không        | Số nguyên 1–5; score_min ≤ score_max                                         | “Khoảng điểm không hợp lệ”                                    |
| group_by             | Query (EP6)    | Không        | Chỉ nhận month                                                               | “Chỉ hỗ trợ nhóm theo tháng”                                  |
| page, size           | Query (EP7)    | Không        | page ≥ 1; 1 ≤ size ≤ 100; mặc định 1 và 20                                   | “Tham số phân trang không hợp lệ”                             |

## **8. Tự kiểm trước khi nộp**

| **Câu hỏi**                                                          | **Kết quả**                                                                                                           |
|----------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Mỗi endpoint nối được về ít nhất một User Story trong bảng truy vết? | ✔ Cột “User Story” ở mục 1; trùng với bảng 6.2 của SRS.                                                               |
| Mỗi endpoint có ít nhất một response thành công và hai response lỗi? | ✔ Endpoint 1: 5 lỗi · 2: 5 · 3: 4 · 4: 6 · 5: 4 · 6: 3 · 7: 3 · 8: 3 · 9: 3.                                          |
| Mọi trường trong request body tồn tại trong mô hình dữ liệu?         | ✔ ticket_id, channel, score, comment ↔ mục 4.                                                                         |
| Có endpoint nào không phục vụ story nào?                             | Không.                                                                                                                |
| Quy tắc nghiệp vụ đã xuất hiện trong validation hoặc mã lỗi?         | ✔ QT-10 (409, 422), QT-02 (INVALID_PHONE), QT-13 (không có endpoint sửa/xóa), QT-14 (403), QT-15 (số điện thoại che). |
| Mỗi mã trạng thái là một test case cho BT3?                          | Có: tổng 36 trường hợp lỗi + 9 trường hợp thành công.                                                                 |
