package com.mekongmobile.smartcrm.reporting;

import com.mekongmobile.smartcrm.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportingLoadIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthService auth;
    @LocalServerPort int port;

    @Test
    void reportingAndLookupApiP95StayBelowTwoSecondsWithTenThousandResponses() throws Exception {
        long firstId = System.currentTimeMillis() * 1_000L;
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).withNano(0);
        LocalDate from = now.toLocalDate().withDayOfMonth(1);
        jdbc.batchUpdate("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES(?,?,201,101,301,501,'DA_DONG',?)",
                new BatchPreparedStatementSetter() {
                    @Override public void setValues(java.sql.PreparedStatement ps, int index) throws java.sql.SQLException {
                        long id = firstId + index;
                        ps.setLong(1, id);
                        ps.setString(2, "N" + String.format("%019d", id % 1_000_000_000_000_000_000L));
                        ps.setObject(3, now.minusDays(2));
                    }
                    @Override public int getBatchSize() { return 10_000; }
                });
        jdbc.batchUpdate("INSERT INTO survey_invitation(ticket_id,token,status,channel,send_attempts,created_at,sent_at,expires_at) VALUES(?,?,'DA_PHAN_HOI','SMS',1,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override public void setValues(java.sql.PreparedStatement ps, int index) throws java.sql.SQLException {
                        ps.setLong(1, firstId + index);
                        ps.setString(2, String.format("%043d", index + 1));
                        ps.setObject(3, now.minusDays(2));
                        ps.setObject(4, now.minusDays(1));
                        ps.setObject(5, now.plusDays(6));
                    }
                    @Override public int getBatchSize() { return 10_000; }
                });
        jdbc.batchUpdate("INSERT INTO survey_response(ticket_id,score,comment,responded_at) VALUES(?,?,?,?)",
                new BatchPreparedStatementSetter() {
                    @Override public void setValues(java.sql.PreparedStatement ps, int index) throws java.sql.SQLException {
                        ps.setLong(1, firstId + index);
                        ps.setInt(2, index % 5 + 1);
                        ps.setString(3, "Load fixture " + index);
                        ps.setObject(4, now.minusHours(1));
                    }
                    @Override public int getBatchSize() { return 10_000; }
                });

        String manager = "Bearer " + auth.login("manager101", "test-only-password-000000").get("access_token");
        String director = "Bearer " + auth.login("director", "test-only-password-000000").get("access_token");
        String marketing = "Bearer " + auth.login("marketing", "test-only-password-000000").get("access_token");
        String period = "from=" + from + "&to=" + now.toLocalDate();
        var client = HttpClient.newHttpClient();
        var targets = new String[][]{
                {"/api/survey-stats/by-technician?center_id=101&" + period, manager},
                {"/api/survey-stats/by-center?group_by=month&" + period, director},
                {"/api/survey-responses?center_id=101&page=1&size=20&" + period, marketing}
        };
        for (var target : targets) {
            long[] samples = new long[20];
            for (int i = 0; i < samples.length; i++) {
                var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + target[0]))
                        .header("Authorization", target[1]).GET().build();
                long started = System.nanoTime();
                var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                samples[i] = (System.nanoTime() - started) / 1_000_000;
                assertEquals(200, response.statusCode(), target[0]);
            }
            Arrays.sort(samples);
            long p95 = samples[18];
            System.out.printf("NFR03 API=%s rows=10000 samples=20 p95_ms=%d max_ms=%d machine=local-loopback/mysql-test%n",
                    target[0].split("\\?")[0], p95, samples[19]);
            assertTrue(p95 <= 2_000, target[0] + " p95 was " + p95 + " ms");
        }
    }
}
