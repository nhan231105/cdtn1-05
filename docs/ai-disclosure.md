# Khai báo sử dụng công cụ AI – BT2

Sinh viên: Võ Nguyễn Thành Nhân – 2374802010360. Dự án: Smart CRM – Mekong Mobile, L8. Khai báo BT1 gốc vẫn giữ tại [ai-disclosure.md](../ai-disclosure.md); tài liệu này bổ sung phần BT2, không thay hoặc suy diễn bằng chứng BT1.

| Công cụ | Dùng vào việc gì | Tệp/hàm áp dụng | Cách kiểm chứng và giới hạn |
|---|---|---|---|
| Codex | Kiểm kê source và đối chiếu UC/US/FR với rubric từ đề BT2 | `docs/bt2-gap-analysis.md`, `docs/checkpoint-0.md` | Đọc SRS/API/Drawio, văn bản DOCX BT1/BT2, xem wireframe, kiểm branch/diff; nguồn HoanThien chưa được cung cấp thì ghi rõ |
| Codex | Hoàn thiện gửi SMS/Zalo giả lập, transaction phản hồi, scheduler/retry/reminder | `SurveyDeliveryService`, `SurveyResponseService`, `SurveyScheduler`, controllers/repositories, migration V4/V5 | Kiểm thử unit/integration và bằng chứng Maven tại báo cáo; không tự coi source là runtime pass; V1/V2 đối chiếu checksum |
| Codex | Hỗ trợ login JWT/BCrypt, scope dữ liệu, aggregate/filter/XLSX | `auth/`, `reporting/`, migration V3 | Tests role/scope/filter/rounding, XLSX và MySQL thật theo kết quả suite; chưa đo NFR tải thì giữ NOT_VERIFIED |
| Codex | Hiện thực React theo wireframe và các màn phụ | `frontend/src/`, `frontend/tests/`, API client/Vite config | Unit giao diện, build và E2E; phân biệt mock API với luồng dùng backend/MySQL thật |
| Codex | Thực hiện CRUD có điều kiện cho lời mời và CRUD mẫu khảo sát; đo NFR01/02/03/05/06 | `SurveyInvitationService`, `SurveyTemplateService`, migration V6, `SurveyFlowIntegrationTest`, các load integration tests | 54 backend tests PASS; đo trên MySQL/servlet cục bộ và gateway giả lập. S1 vẫn chờ xác nhận học thuật; NFR07 chưa có người tham gia thật |
| Codex | Tài liệu chạy lại, môi trường, nghiệm thu, khai báo và báo cáo | `README.md`, `.env.example`, `docs/`, `scripts/test.ps1` | So biến với properties/source, chạy lệnh theo README và ghi kết quả thực; clone trên máy mới chưa làm thì không khẳng định |

Phần giao cho AI vẫn cần sinh viên đọc, giải thích quy tắc/TX/SQL/phân quyền và tự xác nhận trước khi nộp. Không có bằng chứng thử nghiệm người dùng 5 người thì không ghi đã đạt NFR07. Không dùng AI để giả ảnh kiểm thử hoặc hợp thức hóa sửa/xóa phản hồi trái BT1.
