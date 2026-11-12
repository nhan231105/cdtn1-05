CREATE TABLE service_center (
  center_id BIGINT PRIMARY KEY,
  center_name VARCHAR(120) NOT NULL
);
CREATE TABLE customer (
  customer_id BIGINT PRIMARY KEY,
  phone VARCHAR(20) NOT NULL UNIQUE
);
CREATE TABLE technician (
  technician_id BIGINT PRIMARY KEY,
  center_id BIGINT NOT NULL,
  FOREIGN KEY (center_id) REFERENCES service_center(center_id)
);
CREATE TABLE ticket (
  ticket_id BIGINT PRIMARY KEY,
  ticket_code VARCHAR(20) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL,
  center_id BIGINT NOT NULL,
  technician_id BIGINT NULL,
  device_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  closed_at TIMESTAMP NULL,
  FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
  FOREIGN KEY (center_id) REFERENCES service_center(center_id),
  FOREIGN KEY (technician_id) REFERENCES technician(technician_id)
);

CREATE TABLE survey_invitation (
  invitation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL UNIQUE,
  token CHAR(43) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,
  channel VARCHAR(10) NOT NULL DEFAULT 'SMS',
  send_attempts SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL,
  reminded_at TIMESTAMP NULL,
  expires_at TIMESTAMP NOT NULL,
  FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id),
  CHECK (status IN ('CHO_GUI','DA_GUI','GUI_LOI','DA_PHAN_HOI')),
  CHECK (channel IN ('SMS','ZALO')),
  CHECK (send_attempts BETWEEN 0 AND 3)
);
CREATE TABLE survey_response (
  response_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  ticket_id BIGINT NOT NULL UNIQUE,
  score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
  comment TEXT NULL,
  responded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id)
);
CREATE INDEX idx_ticket_center_tech ON ticket(center_id, technician_id);
CREATE INDEX idx_sr_time ON survey_response(responded_at);
CREATE INDEX idx_si_status_sent ON survey_invitation(status, sent_at);
