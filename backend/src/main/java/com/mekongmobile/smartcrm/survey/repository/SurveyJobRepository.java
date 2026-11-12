package com.mekongmobile.smartcrm.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SurveyJobRepository {
    private final JdbcTemplate jdbc;
    public SurveyJobRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Delivery(long invitationId, String channel) {}

    public List<Long> closedTicketsWithoutInvitation() {
        return jdbc.queryForList("""
                SELECT t.ticket_id FROM ticket t LEFT JOIN survey_invitation i ON i.ticket_id=t.ticket_id
                WHERE t.status='DA_DONG' AND i.invitation_id IS NULL LIMIT 100
                """, Long.class);
    }

    public List<Delivery> pendingDeliveries(LocalDateTime now) {
        return jdbc.query("""
                SELECT i.invitation_id, i.channel FROM survey_invitation i
                JOIN ticket t ON t.ticket_id=i.ticket_id JOIN customer c ON c.customer_id=t.customer_id
                WHERE i.status IN ('CHO_GUI','GUI_LOI') AND i.send_attempts<3 AND i.expires_at>?
                AND (i.last_attempt_at IS NULL OR i.last_attempt_at<=?)
                AND (i.status='CHO_GUI' OR c.phone REGEXP '^0[0-9]{9}$') LIMIT 100
                """, (rs, row) -> new Delivery(rs.getLong("invitation_id"), rs.getString("channel")), now, now.minusMinutes(5));
    }

    public List<Long> pendingReminders(LocalDateTime now) {
        return jdbc.queryForList("""
                SELECT invitation_id FROM survey_invitation WHERE status='DA_GUI' AND reminded_at IS NULL
                AND sent_at<=? AND expires_at>? AND reminder_attempts<3
                AND (last_reminder_attempt_at IS NULL OR last_reminder_attempt_at<=?) LIMIT 100
                """, Long.class, now.minusHours(72), now, now.minusMinutes(5));
    }
}
