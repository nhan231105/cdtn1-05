-- Retry failed reminder deliveries without counting them as successful reminders.
ALTER TABLE survey_invitation
    ADD COLUMN reminder_attempts SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN last_reminder_attempt_at TIMESTAMP NULL,
    ADD CONSTRAINT chk_reminder_attempts CHECK (reminder_attempts BETWEEN 0 AND 3);
