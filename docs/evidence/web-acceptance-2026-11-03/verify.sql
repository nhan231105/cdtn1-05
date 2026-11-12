-- Read-only. Workbench: choose the isolated 127.0.0.1:3307 connection.
USE smartcrm_l8_test;
SELECT DATABASE(), VERSION();
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT COUNT(*) AS invitations FROM survey_invitation;
SELECT COUNT(*) AS responses FROM survey_response;
SELECT t.ticket_id,t.ticket_code,i.status,i.channel,i.send_attempts,i.last_attempt_at,
       i.sent_at,i.reminded_at,i.reminder_attempts,i.expires_at,r.score,r.comment,r.responded_at
FROM ticket t LEFT JOIN survey_invitation i USING(ticket_id)
LEFT JOIN survey_response r USING(ticket_id) ORDER BY t.ticket_id;
SELECT ticket_id,COUNT(*) FROM survey_invitation GROUP BY ticket_id HAVING COUNT(*)>1;
SELECT ticket_id,COUNT(*) FROM survey_response GROUP BY ticket_id HAVING COUNT(*)>1;
SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA=DATABASE();
SELECT t.center_id,t.technician_id,COUNT(*) AS responses,ROUND(AVG(r.score),2) AS csat_avg,
       ROUND(SUM(r.score>=4)/COUNT(*),2) AS satisfied_rate
FROM survey_response r JOIN ticket t USING(ticket_id)
WHERE r.responded_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')
  AND r.responded_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01')
GROUP BY t.center_id,t.technician_id;
SELECT t.center_id,DATE_FORMAT(i.sent_at,'%Y-%m') AS month,COUNT(*) AS invitations_sent,
       SUM(r.response_id IS NOT NULL AND r.responded_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01')) AS responses,
       ROUND(SUM(r.response_id IS NOT NULL AND r.responded_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01'))/COUNT(*),2) AS response_rate
FROM survey_invitation i JOIN ticket t USING(ticket_id)
LEFT JOIN survey_response r USING(ticket_id)
WHERE i.status IN ('DA_GUI','DA_PHAN_HOI')
  AND i.sent_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')
  AND i.sent_at < DATE_FORMAT(CURRENT_DATE+INTERVAL 1 MONTH,'%Y-%m-01')
GROUP BY t.center_id,DATE_FORMAT(i.sent_at,'%Y-%m');

