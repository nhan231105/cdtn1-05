# Hướng dẫn nghiệm thu và nộp BT2

Theo đề BT2 đã tìm thấy ở thư mục học liệu, nộp một PDF và link repository/tag `v0.2`. Kết quả từng bước kiểm tra và Git cuối được ghi trong [checkpoint 3](checkpoint-3.md); quy trình hiện tại đang thực hiện theo yêu cầu hoàn thiện đã được người dùng cung cấp.

## Nghiệm thu bắt buộc trước nộp

1. Xem `git diff`, bảo đảm đúng branch, giữ thay đổi của người dùng, không có `.env`, password/key thật, build/node_modules/target, backend.zip trong phần dự định commit.
2. Chạy nguyên bộ backend bằng `powershell -File scripts/test.ps1`; unit/integration không skip, có happy/boundary/invalid. Chạy frontend tests/build/E2E theo README; lưu output thật vào minh chứng, ghi ngày/lệnh/số test. Nếu lỗi, giữ lỗi và xử lý hoặc ghi chưa đạt, không chỉ chụp đoạn BUILD SUCCESS của suite rút gọn.
3. Thử UC03: form hợp lệ, chọn score, gửi, cảm ơn; gửi lại 409; expired 410; input sai 400; transaction rollback khi DB lỗi; đối chiếu một response và trạng thái DA_PHAN_HOI. Không xóa response để chạy demo lại.
4. Thử manager đúng center và sửa center khác 403; marketing list/export; director by-center; chưa đăng nhập 401. File XLSX và danh sách đều phải che phone.
5. Ghi ảnh ba màn hình chính ở loading, empty và lỗi kết nối có retry; bộ lọc rỗng khác chưa có phản hồi. Ảnh phải chụp phiên bản đang chạy, không dùng wireframe làm ảnh hiện thực.
6. Đã clone sạch commit `b5199d5` trên cùng máy; `npm ci`, backend/frontend test, build, E2E và browser/API checks đều PASS trên MySQL test mới. Kết quả không đại diện thử nghiệm trên máy khác.
7. Xin GV xác nhận cách tính S1. NFR01/02/03/05/06 đã đo cục bộ với các giới hạn ghi trong verification; NFR07 chưa thử với 5 người thật, NFR04 chưa thử nhiều instance/pentest.

## Bộ tài liệu

| Thành phần | Vị trí |
|---|---|
| SRS/API/Use Case/wireframe BT1 | Root repository, liên kết trong README |
| Ma trận toàn bộ use case/rubric | `docs/bt2-gap-analysis.md` |
| Thay đổi thiết kế | `docs/thay-doi-thiet-ke.md` |
| Kiến trúc thực tế | `docs/architecture-thuc-te.md` |
| AI với tệp/hàm và cách kiểm chứng | `docs/ai-disclosure.md` |
| Báo cáo cập nhật có giới hạn bằng chứng | `docs/reports/bt2-implementation-report.md` |
| PDF cập nhật8trang từ Markdown và7ảnh thực | `docs/reports/BT2_L8_implementation_2026-11-12.pdf` |
| Bản PDF/DOCX hiện có | `docs/reports/BT2_2374802010360_VONGUYENTHANHNHAN.*` – là báo cáo cũ, cần biên tập lại từ kết quả mới trước nộp |
| Môi trường và chạy lại | `.env.example`, README, Maven/npm lock/config |

PDF mới8trang có bìa, matrix UC/rubric, kiến trúc/quy tắc, kết quả test, ảnh data/loading/empty/error, dựng lại, thay đổi BT1 và khai báo AI. PDF/DOCX cũ được giữ nguyên. Sinh viên cần đọc, giải thích và xác nhận nội dung trước khi nộp. Trạng thái tag/repository được xác minh ở checkpoint 3.

## Thao tác Git cuối

Lệnh push branch, merge `main` và tạo annotated tag `v0.2` được người dùng yêu cầu trong prompt tổng. Ghi kết quả lệnh và hash xác minh thực tế tại checkpoint 3; push branch không thay cho push tag. Nếu repository private, người nộp cần bảo đảm giảng viên có quyền xem.
