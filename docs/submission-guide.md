# Hướng dẫn nghiệm thu và nộp BT2

Theo đề BT2 đã tìm thấy ở thư mục học liệu, nộp một PDF và link repository/tag `v0.2`. Tài liệu này chuẩn bị công việc có thể kiểm tra; không có thao tác push, merge hay tạo tag được tự thực hiện.

## Nghiệm thu bắt buộc trước nộp

1. Xem `git diff`, bảo đảm đúng branch, giữ thay đổi của người dùng, không có `.env`, password/key thật, build/node_modules/target, backend.zip trong phần dự định commit.
2. Chạy nguyên bộ backend bằng `powershell -File scripts/test.ps1`; unit/integration không skip, có happy/boundary/invalid. Chạy frontend tests/build/E2E theo README; lưu output thật vào minh chứng, ghi ngày/lệnh/số test. Nếu lỗi, giữ lỗi và xử lý hoặc ghi chưa đạt, không chỉ chụp đoạn BUILD SUCCESS của suite rút gọn.
3. Thử UC03: form hợp lệ, chọn score, gửi, cảm ơn; gửi lại 409; expired 410; input sai 400; transaction rollback khi DB lỗi; đối chiếu một response và trạng thái DA_PHAN_HOI. Không xóa response để chạy demo lại.
4. Thử manager đúng center và sửa center khác 403; marketing list/export; director by-center; chưa đăng nhập 401. File XLSX và danh sách đều phải che phone.
5. Ghi ảnh ba màn hình chính ở loading, empty và lỗi kết nối có retry; bộ lọc rỗng khác chưa có phản hồi. Ảnh phải chụp phiên bản đang chạy, không dùng wireframe làm ảnh hiện thực.
6. Dựng từ thư mục mới theo đúng README bằng database mới riêng; khởi động lại lần hai không nhân đôi dữ liệu, giữ Flyway history/checksum V1/V2. Chưa clone thật thì ghi NOT_VERIFIED, không tuyên bố đạt C1/C5.
7. Xin GV xác nhận cách đánh giá S1 thay CRUD trái nghiệp vụ. Ghi rõ các NFR tải/usability chưa đo; không bỏ khỏi báo cáo.

## Bộ tài liệu

| Thành phần | Vị trí |
|---|---|
| SRS/API/Use Case/wireframe BT1 | Root repository, liên kết trong README |
| Ma trận toàn bộ use case/rubric | `docs/bt2-gap-analysis.md` |
| Thay đổi thiết kế | `docs/thay-doi-thiet-ke.md` |
| Kiến trúc thực tế | `docs/architecture-thuc-te.md` |
| AI với tệp/hàm và cách kiểm chứng | `docs/ai-disclosure.md` |
| Báo cáo cập nhật có giới hạn bằng chứng | `docs/reports/bt2-implementation-report.md` |
| PDF cập nhật8trang từ Markdown và7ảnh thực | `docs/reports/BT2_L8_implementation_2026-11-03.pdf` |
| Bản PDF/DOCX hiện có | `docs/reports/BT2_2374802010360_VONGUYENTHANHNHAN.*` – là báo cáo cũ, cần biên tập lại từ kết quả mới trước nộp |
| Môi trường và chạy lại | `.env.example`, README, Maven/npm lock/config |

PDF mới8trang đã có bìa (họ tên/MSSV/track/L8/remote cấu hình/tag chưa tạo), fullmatrixUC/rubric, kiến trúc/rule, tests, ảnhdata/loading/empty/error, dựng lại, thay đổi BT1 và AI. File cũ được giữ; sinh viên cần đọc/kiểm chứng và chấp nhận nội dung, bổ sung commit/tag thật sau phê duyệt trước khi dùng làm bản nộp. Không tự ghi tag chưa tạo là đã xuất bản.

## Thao tác Git cuối

Chủ repository duyệt diff và kết quả tests trước commit/merge/tag/push. Khi được phê duyệt, người phụ trách chọn commit nộp, kiểm tra tag trỏ đúng mã mới và tự mở trang GitHub theo tag để xác nhận truy cập được. Không coi lệnh push branch là đã push tag. Repository private phải bảo đảm giảng viên có quyền xem. Các bước cần quyền này hiện chưa thực hiện.
