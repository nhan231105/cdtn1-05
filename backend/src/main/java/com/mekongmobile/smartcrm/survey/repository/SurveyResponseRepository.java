package com.mekongmobile.smartcrm.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public class SurveyResponseRepository {
    private final JdbcTemplate jdbc;

    public SurveyResponseRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Map<String, Object> formDetails(long ticketId) {
        return jdbc.queryForMap("""
                SELECT t.ticket_code, c.center_name,
                       COALESCE(d.device_name, CONCAT('Thiết bị #', t.device_id)) AS device_name, t.closed_at
                FROM ticket t JOIN service_center c ON c.center_id=t.center_id
                LEFT JOIN device d ON d.device_id=t.device_id WHERE t.ticket_id=?
                """, ticketId);
    }

    public long insert(long ticketId, int score, String comment, LocalDateTime respondedAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO survey_response(ticket_id,score,comment,responded_at) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, ticketId);
            statement.setInt(2, score);
            statement.setString(3, comment);
            statement.setTimestamp(4, Timestamp.valueOf(respondedAt));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public String ticketCode(long ticketId) {
        return jdbc.queryForObject("SELECT ticket_code FROM ticket WHERE ticket_id=?", String.class, ticketId);
    }

    public boolean isTicketClosed(long ticketId) {
        return "DA_DONG".equals(jdbc.queryForObject("SELECT status FROM ticket WHERE ticket_id=?", String.class, ticketId));
    }

    public Optional<LocalDateTime> respondedAt(long ticketId) {
        return jdbc.query("SELECT responded_at FROM survey_response WHERE ticket_id=?",
                (rs, row) -> rs.getTimestamp("responded_at").toLocalDateTime(), ticketId).stream().findFirst();
    }
}
