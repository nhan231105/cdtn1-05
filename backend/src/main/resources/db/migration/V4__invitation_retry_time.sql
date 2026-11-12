-- Additive retry metadata. V1/V2 and existing submitted responses are preserved.
ALTER TABLE survey_invitation ADD COLUMN last_attempt_at TIMESTAMP NULL;
CREATE INDEX idx_si_retry ON survey_invitation(status, last_attempt_at);
