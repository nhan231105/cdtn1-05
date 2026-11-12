# Checkpoint 0 – Kiểm kê và đánh giá baseline L8

Ngày 03/11/2026; branch xác nhận `feat/bt2-setup`; HEAD lúc bắt đầu `1f62008`.

## Đã thực hiện

- Đọc AGENTS và kiểm tra branch/Git status trước sửa. Giữ thay đổi người dùng tại `backend/src/main/java/com/mekongmobile/smartcrm/survey/entity/SurveyInvitation.java` (markSent/markSendFailed) và các file chưa tracked.
- Kiểm kê backend Spring Boot 4.1.2/Java21, Maven wrapper, migration V1/V2, DTO/entity/repository/service/controller/gateway/filter/advice và 8 test classes. Baseline không có frontend.
- Đối chiếu SRS/API Contract/Drawio, văn bản báo cáo BT1/BT2 và ảnh wireframe. Tìm thấy đề BT2 và học liệu buổi 7–12 ở thư mục cha, có bản Markdown chuyển từ PDF.
- Lập ma trận đầy đủ UC01–09/FR01–12/US1–8 với C1–C7/S1–S4 trong `docs/bt2-gap-analysis.md`; ghi mâu thuẫn S1/immutability và SRS/API phone masking.

## File thay đổi tại checkpoint audit

- `docs/bt2-gap-analysis.md`: nguồn yêu cầu, baseline trạng thái, truy vết API/database/frontend/test/rubric, rủi ro và ưu tiên.
- `docs/checkpoint-0.md`: bằng chứng kiểm kê và giới hạn audit. Không có sửa/xóa dữ liệu DB trong công việc audit.

## Lệnh và kết quả

| Lệnh/kiểm tra | Kết quả |
|---|---|
| `git branch --show-current` | `feat/bt2-setup` |
| `git status --short` trước thực hiện | 1 file entity modified, AGENTS/backend.zip/báo cáo DOCX untracked; đã ghi mốc |
| `rg --files .. -g '!node_modules' -g '!target' -g '!dist' -g '!*.zip'` | Backend, tài liệu BT1/SRS/contract/schema/wireframe và báo cáo; không frontend/requirements/hướng dẫn HoanThien |
| `rg --files ../.. -g '*.md' -g '!node_modules' -g '!target'` | Tìm ra bản Markdown đề BT2 và buổi 7–12 ngoài repo |
| Tìm `Codex_SmartCRM_L8_BT2_HoanThien.md` dưới `D:/cdtn1` | Không tìm thấy; chưa thể kiểm tra chỉ dẫn riêng |
| Đọc DOCX bằng Python `zipfile` + `xml.etree.ElementTree` | Đọc được nội dung BT1/BT2; BT1 ghi gộp US8 vào US2 mở rộng nhưng vẫn giữ UC04 |
| Xem `wireframe-l8.png` | 3 màn: form CSAT, phản hồi, thống kê KTV; XLSX chỉ Marketing |
| Đọc toàn bộ source/test baseline | Chỉ UC01 wired API, UC02 mới foundation; UC03–09 chưa wired |

Kết quả thực thi Maven/integration/E2E phải được bổ sung từ checkpoint kiểm thử chính. Tài liệu audit này không tuyên bố `BUILD SUCCESS` hoặc suy ra test pass từ báo cáo cũ. Kiểm tra cấu trúc C2/S2 là đọc source thực tế, không thay cho nghiệm thu runtime.

## Giới hạn và checkpoint kế tiếp

- Thiếu file hướng dẫn HoanThien; thiếu requirements trong repo; nguồn học liệu ngoài repo chưa portable.
- Chưa chạy môi trường máy mới/khẳng định kết quả MySQL/E2E tại audit.
- Chưa đạt S1 full CRUD; không tạo nghiệp vụ sai để đạt rubric. Cần GV xác nhận cách xử lý mâu thuẫn.
- Tiếp theo: UC02 có service/controller thật, retry theo thiết kế và tests; sau đó UC03/UC06/phân quyền, hoàn thiện các UC còn lại, frontend và dựng lại theo README.
