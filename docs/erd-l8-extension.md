# ERD bổ sung L8 – migration V6

Tài liệu này bổ sung ERD BT1; không sửa `ddl-l8.sql` hay migrations V1/V2.

```mermaid
erDiagram
  TICKET ||--o| SURVEY_INVITATION : invites
  TICKET ||--o| SURVEY_RESPONSE : receives
  TICKET ||--o| CANCELLED_SURVEY_INVITATION : cancellation-tombstone
  SURVEY_TEMPLATE ||--o{ SURVEY_INVITATION : snapshots-content-at-create

  SURVEY_TEMPLATE {
    bigint template_id PK
    varchar name UK
    varchar title
    varchar intro_text
    boolean is_active
    tinyint active_slot UK_generated
    timestamp created_at
    timestamp updated_at
  }
  SURVEY_INVITATION {
    bigint invitation_id PK
    bigint ticket_id FK_UK
    char token UK
    varchar status
    varchar channel
    varchar survey_title_snapshot
    varchar survey_intro_snapshot
  }
  SURVEY_RESPONSE {
    bigint response_id PK
    bigint ticket_id FK_UK
    smallint score
    text comment
    timestamp responded_at
  }
  CANCELLED_SURVEY_INVITATION {
    bigint ticket_id PK_FK
    bigint invitation_id UK_historical_id
    timestamp cancelled_at
    varchar reason
  }
```

`survey_invitation.survey_title` và `survey_intro` là snapshot của mẫu hoạt động lúc tạo; hai cột không có FK vì lời mời phải giữ nguyên nội dung nếu mẫu được sửa hoặc xóa. Khi xóa lời mời `CHO_GUI` chưa có lần gửi, bản ghi lời mời bị xóa vật lý và `cancelled_survey_invitation.ticket_id` giữ khóa duy nhất để ticket không được phát hành lại. `survey_response` không thay đổi và vẫn tối đa một bản ghi cho mỗi ticket.

`survey_template.active_slot` là generated column: mẫu hoạt động có giá trị 1, mẫu không hoạt động là NULL. Unique index cho phép nhiều mẫu nháp nhưng tối đa một mẫu hoạt động. Ứng dụng ngăn tắt/xóa mẫu hoạt động cuối cùng.
