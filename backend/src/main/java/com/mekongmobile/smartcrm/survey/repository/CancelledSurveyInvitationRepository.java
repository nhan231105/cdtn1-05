package com.mekongmobile.smartcrm.survey.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CancelledSurveyInvitationRepository {
    private final JdbcTemplate jdbc;

    public CancelledSurveyInvitationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean existsByTicketId(long ticketId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM cancelled_survey_invitation WHERE ticket_id=?", Integer.class, ticketId);
        return count != null && count > 0;
    }

    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM cancelled_survey_invitation", Integer.class);
        return count == null ? 0 : count;
    }

    public void save(long ticketId, long invitationId) {
        jdbc.update("INSERT INTO cancelled_survey_invitation(ticket_id,invitation_id,reason) VALUES(?,?,'Cancelled before first delivery')",
                ticketId, invitationId);
    }
}
