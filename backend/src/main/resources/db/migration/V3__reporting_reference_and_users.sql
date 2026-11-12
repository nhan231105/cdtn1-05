-- Add missing reference labels without changing any existing ticket/response.
ALTER TABLE technician ADD COLUMN technician_name VARCHAR(120) NULL;

CREATE TABLE device (
    device_id BIGINT PRIMARY KEY,
    device_name VARCHAR(160) NOT NULL
);

-- Device IDs are already stored on tickets; retain unknown labels as explicit placeholders.
INSERT INTO device (device_id, device_name)
SELECT DISTINCT device_id, CONCAT('Thiết bị #', device_id) FROM ticket;

CREATE TABLE app_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    center_id BIGINT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (center_id) REFERENCES service_center(center_id),
    CHECK (role IN ('QL_TRUNG_TAM', 'MARKETING', 'BAN_GIAM_DOC')),
    CHECK ((role = 'QL_TRUNG_TAM' AND center_id IS NOT NULL) OR
           (role <> 'QL_TRUNG_TAM' AND center_id IS NULL))
);

CREATE INDEX idx_si_sent_at ON survey_invitation(sent_at);
