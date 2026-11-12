# Thay đổi thiết kế so với BT1 – L8 BT2

Giữ nguyên luồng L8, modular monolith, Spring Boot/React/MySQL, phản hồi bất biến và ranh giới L2/L4 chỉ đọc. V1/V2 không chỉnh sửa. Các mục dưới ghi thay đổi gì, lý do và ảnh hưởng theo đúng đề BT2 §2.5; không thay cho biên bản phê duyệt của giảng viên.

## 1. Bổ sung nhãn tham chiếu và tài khoản nội bộ bằng V3

- Thay đổi: thêm `technician.technician_name` nullable, bảng `device` với nhãn định danh cho device_id đang có, bảng `app_user` lưu BCrypt/role/center; tạo chỉ mục thời điểm gửi. Chưa có tên thiết bị L2/L4 thật thì hiển thị định danh rõ `Thiết bị #...`, không tự nhận là tên sản phẩm.
- Vì sao: V1 rút gọn 6 bảng chưa đủ tên hiển thị theo contract/wireframe hoặc nguồn xác thực JWT. Đăng nhập cơ bản được BT2 yêu cầu, không mở CRUD dữ liệu tham chiếu.
- Ảnh hưởng: migration V3, API form/báo cáo và login/bootstrap; dữ liệu survey_response/ticket/customer không bị sửa/xóa. Bootstrap mặc định tắt, thêm user còn thiếu khi bật và không đổi user đang có.

## 2. Lưu thời điểm thử gửi bằng V4

- Thay đổi: thêm `survey_invitation.last_attempt_at` để giãn lần thử gửi ít nhất 5 phút, giữ `sent_at` cho lần gửi thành công ban đầu.
- Vì sao: FR03 có retry qua restart; đếm attempts và chỉ dùng created_at/sent_at không thể biết lần lỗi cuối. Không giữ request chờ 5 phút hoặc dùng retry chỉ trong bộ nhớ.
- Ảnh hưởng: migration V4, entity, delivery service/scheduler/tests. Không replay V1/V2, không đổi phản hồi đã nộp.

## 3. Bộ thăm dò chỉ đọc làm adapter sự kiện L2/L4 khi chạy độc lập

- Thay đổi: scheduler mặc định 30 giây đọc phiếu DA_DONG chưa có lời mời, gọi service UC01/UC02; giới hạn batch. EP1/EP2 nhận tích hợp service-key vẫn giữ.
- Vì sao: module phát sự kiện đóng phiếu ở ngoài phạm vi và chưa có trong repository. Thăm dò cho phép dùng bản độc lập mà không thêm chức năng sửa ticket.
- Ảnh hưởng: scheduler/config và tests, lịch tạo/gửi/retry/reminder; khi nối event thật có thể tắt `SURVEY_SCHEDULER_ENABLED`. SLO từ thời điểm đóng vẫn cần đo riêng.

V5 bổ sung `reminder_attempts` và `last_reminder_attempt_at`: UC04 include UC02 nên cổng gửi lỗi khi nhắc cũng cần retry hữu hạn ba lần, cách5 phút. `reminded_at` chỉ ghi khi gửi nhắc thành công, không coi lần lỗi là đã nhắc; giữ một lần thành công/cùng token và giữ `sent_at` ban đầu. Ảnh hưởng entity/delivery/scheduler/tests, không thay đổi response.

## 4. Che điện thoại cho mọi response và XLSX

- Thay đổi: luôn che số điện thoại ở API/Excel, kể cả manager.
- Vì sao: API Contract §2 nói không endpoint nào trả phone đầy đủ, trong khi SRS FR12/QT15 nêu ngoại lệ cho quản lý/giám đốc. Chọn hợp đồng chặt hơn và đúng yêu cầu người dùng che phone.
- Ảnh hưởng: mapper dữ liệu danh sách/Excel và tests; không sửa số điện thoại gốc trong MySQL.

## 5. Truy vết US8 và phạm vi màn hình phụ

- Thay đổi: ghi đồng thời US8 của SRS gốc và US2 mở rộng của BT1 đối với UC04; thêm login, CSAT trung tâm và tỷ lệ phản hồi bên cạnh ba màn hình wireframe.
- Vì sao: báo cáo BT1 đã gộp US8 để tuân số story nhưng giữ FR11/UC04. Các endpoint SHOULD/COULD cần giao diện sử dụng theo yêu cầu bổ sung, không bỏ UC vì wireframe chỉ có ba màn chính.
- Ảnh hưởng: frontend routes/role navigation, tài liệu truy vết; UC04 vẫn là tác vụ nền không public endpoint.

## 6. Khả năng kiểm thử trên MySQL riêng

- Thay đổi: kiểm thử tích hợp dùng MySQL test tạm/schema riêng và cấu hình test, tự dựng fixture; không phụ thuộc database cá nhân đang chạy.
- Vì sao: baseline integration tests lấy DB_USER/DB_PASSWORD máy người phát triển; không đáp ứng clone-and-run hoặc phép thử an toàn.
- Ảnh hưởng: test resources/scripts/README. Test khởi tạo và dọn tài nguyên do nó tạo trong vùng tạm xác minh; không clean schema MySQL hiện hữu.

## 7. Kỳ tính tỷ lệ phản hồi

- Thay đổi: tỷ lệ theo cohort lời mời đã gửi trong tháng/kỳ; tử số là số phản hồi của chính các lời mời đó nhận đến cuối kỳ báo cáo. Không chia số phản hồi độc lập theo ngày gửi phản hồi cho số lời mời khác theo ngày gửi tin.
- Vì sao: KS03 yêu cầu phản hồi trên số lời mời gửi thành công, tỷ lệ 0–1. Hai tập thời gian độc lập có thể làm tỷ lệ vượt 100% khi khách trả lời lời mời tháng trước; cohort giữ mẫu số và tử số cùng tập phiếu.
- Ảnh hưởng: query response-rate và tests; ngày gửi dùng để phân tháng, các phản hồi nhận sau ngày cuối báo cáo chưa được tính. Cần ghi ngữ cảnh này trong báo cáo và giao diện để người đọc hiểu mẫu.

## 8. Giải pháp kỹ thuật S1 – CRUD theo vòng đời và mẫu khảo sát

- Thay đổi: lời mời có API đọc danh sách/chi tiết, sửa kênh trước lần gửi đầu và xóa vật lý khi chưa gửi/chưa có phản hồi. Bảng `cancelled_survey_invitation` lưu dấu hủy duy nhất theo phiếu để chặn phát hành lại token; việc hủy không xóa phiếu L2/L4. Thêm entity `survey_template` do L8 quản lý với POST/GET/PUT/DELETE, validation, quyền Marketing, duy nhất tối đa một mẫu hoạt động; mẫu được chụp vào lời mời khi tạo.
- Vì sao: prompt hoàn thiện BT2 yêu cầu đánh giá CRUD thực chất trước; vòng đời lời mời cho phép một phạm vi CRUD có điều kiện. Mẫu khảo sát là cấu hình L8 có ý nghĩa sử dụng thực tế và là entity thứ hai, không sửa/xóa phản hồi hoặc dữ liệu tham chiếu.
- Ảnh hưởng: migration bổ sung V6, các API nội bộ/mẫu, giao diện form đọc title/intro qua API, tests CRUD/role/race. V1/V2 và tài liệu gốc BT1 được giữ nguyên; API Contract có phụ lục riêng.
- Trạng thái xác nhận: hiện thực và kiểm thử đã có; **chờ xác nhận học thuật** rằng CRUD có điều kiện theo vòng đời lời mời và CRUD mẫu khảo sát đáp ứng cách chấm S1. Không xem việc thêm mã là biên bản duyệt của giảng viên.

Các giả định A03–A06 vẫn ghi “đề xuất – chờ GV” trong hồ sơ gốc; triển khai dùng giá trị đã thiết kế (7 ngày, 72 giờ, Marketing toàn công ty, score≥4) theo yêu cầu hiện thực L8 hiện tại, không ghi thành đã có biên bản GV duyệt.
