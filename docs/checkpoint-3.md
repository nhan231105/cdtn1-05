# Checkpoint 3 – Hoàn thiện và xác minh BT2

Ngày 12/11/2026 · branch `feat/bt2-setup`.

## Đã hoàn thành

- Bổ sung V6, CRUD mẫu khảo sát cho Marketing và CRUD lời mời theo vòng đời (danh sách/chi tiết, PATCH/DELETE an toàn); response vẫn bất biến, dữ liệu L2/L4 chỉ đọc.
- Chạy full backend suite: 54/54 PASS, không fail/error/skip; frontend 18/18 PASS và production build PASS.
- Chạy lại E2E/browser/API trên MySQL 8 mới, migration V1–V6: Playwright 1/1 PASS và browser/API checks PASS.
- Ghi lại NFR01/02/03/05/06 với phương pháp và giới hạn trong [verification report](verification-2026-11-12.md).
- Cập nhật API contract, ERD, README, thay đổi thiết kế, disclosure AI, gap analysis, evidence, báo cáo Markdown/PDF và hướng dẫn nộp.
- V1/V2 không thay đổi; không dùng hoặc chỉnh MySQL cổng 3306.

## Còn chờ

- S1: cần giảng viên xác nhận CRUD lời mời có điều kiện cùng CRUD mẫu khảo sát đáp ứng rubric.
- NFR07: cần thực hiện với 5 người dùng thật; không dùng dữ liệu mô phỏng thay thế.
- NFR04 phân tán/pentest và tích hợp event/SMS/Zalo thật ngoài môi trường hiện tại.
- Trạng thái commit, clone sạch, push, merge và tag được ghi sau khi các thao tác Git kiểm tra xong.

Chi tiết kiểm thử và NFR: [verification 12/11](verification-2026-11-12.md). Ma trận lịch sử và trạng thái hiện hành: [gap analysis](bt2-gap-analysis.md).
