CREATE TABLE survey_template (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    intro_text VARCHAR(1000) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    active_slot TINYINT GENERATED ALWAYS AS (CASE WHEN is_active THEN 1 ELSE NULL END) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_survey_template_active (active_slot),
    CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CHECK (CHAR_LENGTH(TRIM(title)) > 0),
    CHECK (CHAR_LENGTH(TRIM(intro_text)) > 0)
);

INSERT INTO survey_template (template_id, name, title, intro_text, is_active)
VALUES (1, 'CSAT sau bảo hành', 'Trải nghiệm của bạn thế nào?',
        'Hãy chia sẻ cảm nhận về dịch vụ bảo hành của Mekong Mobile.', TRUE);

ALTER TABLE survey_invitation
    ADD COLUMN survey_title VARCHAR(120) NOT NULL DEFAULT 'Trải nghiệm của bạn thế nào?',
    ADD COLUMN survey_intro VARCHAR(1000) NOT NULL DEFAULT 'Hãy chia sẻ cảm nhận về dịch vụ bảo hành của Mekong Mobile.';

CREATE TABLE cancelled_survey_invitation (
    ticket_id BIGINT PRIMARY KEY,
    invitation_id BIGINT NOT NULL UNIQUE,
    cancelled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255) NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id),
    CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
);
