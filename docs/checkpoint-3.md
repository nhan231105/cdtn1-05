# Checkpoint 3 – Hoàn thiện và xác minh BT2

Ngày 12/11/2026 · `feat/bt2-setup` đã phát hành lên `main` với tag `v0.2`.

## Đã hoàn thành

- Bổ sung V6, CRUD mẫu khảo sát cho Marketing và CRUD lời mời theo vòng đời (danh sách/chi tiết, PATCH/DELETE an toàn); response vẫn bất biến, dữ liệu L2/L4 chỉ đọc.
- Tạo clone sạch của `feat/bt2-setup` tại `b5199d5`; `npm ci` báo 0 vulnerabilities.
- Trong clone mới: backend 54/54 PASS (0 fail/error/skip), frontend 18/18 PASS, production build PASS, E2E 1/1 PASS (5,8 giây), browser/API checks PASS trên MySQL 8 mới với migration V1–V6.
- Ghi lại NFR01/02/03/05/06 với phương pháp và giới hạn trong [verification report](verification-2026-11-12.md).
- Cập nhật API contract, ERD, README, thay đổi thiết kế, disclosure AI, gap analysis, evidence, báo cáo Markdown/PDF và hướng dẫn nộp.
- V1/V2 không thay đổi; không dùng hoặc chỉnh MySQL cổng 3306.

## Git phát hành đã hoàn tất

- `feat/bt2-setup` đã push lên remote tại `9af61a0c0133cf5801e8623e13f7ba74782d6984`.
- Merge không fast-forward vào `main`: `b99577567a166baef32d87eae8681978f45f2a7f` (giữ commit remote PDF `21be826`).
- Annotated tag `v0.2` đã push; tag object `2532f2c58600e3931d31f17407592720ad4760a1`, trỏ tới merge commit `b99577567a166baef32d87eae8681978f45f2a7f`.
- Đối chiếu `git ls-remote` sau push xác nhận `origin/main`, `origin/feat/bt2-setup` và tag peeled `v0.2^{}` đúng các hash trên.

## Còn chờ

- S1: cần giảng viên xác nhận CRUD lời mời có điều kiện cùng CRUD mẫu khảo sát đáp ứng rubric.
- NFR07: cần thực hiện với 5 người dùng thật; không dùng dữ liệu mô phỏng thay thế.
- NFR04 phân tán/pentest và tích hợp event/SMS/Zalo thật ngoài môi trường hiện tại.
- Không nộp E-learning trong lượt này; sinh viên cần tự tải báo cáo và hoàn tất checklist nộp.

Chi tiết kiểm thử và NFR: [verification 12/11](verification-2026-11-12.md). Ma trận lịch sử và trạng thái hiện hành: [gap analysis](bt2-gap-analysis.md).
