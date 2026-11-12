package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.survey.repository.SurveyJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class SurveyDeliveryLoadIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() * 1000L);
    @Autowired JdbcTemplate jdbc;
    @Autowired SurveyInvitationService invitations;
    @Autowired SurveyDeliveryService delivery;

    @Test
    void simulatedGatewaySendsAtLeastNinetyNinePercentOfFiveHundredClosedTicketsWithinFiveMinutes() {
        long firstId = IDS.addAndGet(1_000);
        LocalDateTime closedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusSeconds(1).withNano(0);
        jdbc.batchUpdate("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES(?, ?,201,101,301,501,'DA_DONG',?)",
                new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                    @Override public void setValues(java.sql.PreparedStatement ps, int index) throws java.sql.SQLException {
                        long id = firstId + index;
                        ps.setLong(1, id);
                        ps.setString(2, "L" + id);
                        ps.setObject(3, closedAt);
                    }
                    @Override public int getBatchSize() { return 500; }
                });

        SurveyScheduler scheduler = new SurveyScheduler(new SurveyJobRepository(jdbc), invitations, delivery);
        long started = System.nanoTime();
        int sent;
        int ticks = 0;
        do {
            scheduler.tick();
            ticks++;
            sent = jdbc.queryForObject("SELECT COUNT(*) FROM survey_invitation WHERE ticket_id BETWEEN ? AND ? AND status='DA_GUI'",
                    Integer.class, firstId, firstId + 499);
        } while (sent < 500 && ticks < 10);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        Integer withinFiveMinutes = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ticket t JOIN survey_invitation i ON i.ticket_id=t.ticket_id
                WHERE t.ticket_id BETWEEN ? AND ? AND i.sent_at<=DATE_ADD(t.closed_at, INTERVAL 5 MINUTE)
                """, Integer.class, firstId, firstId + 499);

        System.out.printf("NFR06 benchmark: tickets=500 sent=%d within_5m=%d ticks=%d elapsed_ms=%d gateway=simulated%n",
                sent, withinFiveMinutes, ticks, elapsedMs);
        assertEquals(500, sent);
        assertTrue(withinFiveMinutes >= 495);
    }
}
