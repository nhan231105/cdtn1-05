package com.mekongmobile.smartcrm.reporting;

import com.mekongmobile.smartcrm.auth.AuthService;
import com.mekongmobile.smartcrm.auth.UserIdentity;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class ReportingIntegrationTest {
    @Autowired ReportingService reports;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;

    @BeforeEach
    void fixture() {
        jdbc.update("INSERT INTO service_center VALUES (990101, 'Integration center A'), (990102, 'Integration center B')");
        jdbc.update("INSERT INTO technician (technician_id, center_id, technician_name) VALUES (990301,990101,'Tech A'),(990302,990102,'Tech B'),(990303,990101,'Tech empty')");
        jdbc.update("INSERT INTO customer VALUES (990201,'0999999991')");
        ticket(990401, 990101, 990301); ticket(990402, 990101, 990301);
        ticket(990403, 990102, 990302); ticket(990404, 990101, 990301);
        invitation(990401, "a", "DA_PHAN_HOI"); invitation(990402, "b", "DA_PHAN_HOI");
        invitation(990403, "c", "DA_PHAN_HOI"); invitation(990404, "d", "DA_GUI");
        response(990401, 5, "2026-09-01 00:00:00");
        response(990402, 2, "2026-09-30 23:59:59");
        response(990403, 1, "2026-09-10 12:00:00");
    }

    @Test
    void technicianAggregationIncludesZeroResponseTechniciansAndNullCsat() {
        ticket(990405, 990102, 990301);
        invitation(990405, "e", "DA_PHAN_HOI");
        response(990405, 1, "2026-09-10 12:00:00");
        var result = reports.byTechnician(filter(990101L, null, 1, 5));
        var rows = items(result);
        assertEquals(2, rows.size());
        assertEquals(990301L, rows.getFirst().get("technician_id"));
        assertEquals(3L, rows.getFirst().get("invitations_sent"));
        assertEquals(2L, rows.getFirst().get("responses"));
        assertEquals(new BigDecimal("3.50"), rows.getFirst().get("csat_avg"));
        assertEquals(new BigDecimal("0.50"), rows.getFirst().get("satisfied_rate"));
        assertEquals(0L, rows.getLast().get("responses"));
        assertNull(rows.getLast().get("csat_avg"));
        assertNull(rows.getLast().get("satisfied_rate"));
    }

    @Test
    void listsPaginateFilterMaskAndRespectInclusiveDates() {
        var filter = reports.scoped(new UserIdentity("test", "QL_TRUNG_TAM", 990101L), filter(null, null, 1, 5));
        var result = reports.responses(filter, 1, 1);
        assertEquals(2L, result.get("total"));
        assertEquals(1, items(result).size());
        assertEquals(2, items(result).getFirst().get("score"));
        assertEquals("099****991", items(result).getFirst().get("customer_phone"));
        assertEquals(5, items(reports.responses(filter, 2, 1)).getFirst().get("score"));
        assertEquals(1L, reports.responses(filter(990101L, 990301L, 1, 2), 1, 20).get("total"));
        assertEquals(0, items(reports.responses(filter, 3, 1)).size());
        assertThrows(ApiException.class, () -> reports.responses(filter, 0, 20));
        assertThrows(ApiException.class, () -> reports.responses(filter, 1, 101));
    }

    @Test
    void rejectsOtherCenterAndMismatchedTechnician() {
        var manager = new UserIdentity("test", "QL_TRUNG_TAM", 990101L);
        assertEquals("FORBIDDEN_SCOPE", assertThrows(ApiException.class,
                () -> reports.scoped(manager, filter(990102L, null, 1, 5))).getCode());
        assertEquals("VALIDATION_FAILED", assertThrows(ApiException.class,
                () -> reports.scoped(manager, filter(990101L, 990302L, 1, 5))).getCode());
        assertEquals("CENTER_NOT_FOUND", assertThrows(ApiException.class,
                () -> reports.scoped(new UserIdentity("test", "MARKETING", null), filter(9999999L, null, 1, 5))).getCode());
    }

    @Test
    void centerDistributionResponseRateAndWorkbookReflectSameDatabase() {
        var filter = filter(990101L, null, 1, 5);
        var row = items(reports.byCenter(filter)).getFirst();
        assertEquals(2L, row.get("responses"));
        assertEquals(new BigDecimal("3.50"), row.get("csat_avg"));
        assertEquals(Map.of("1", 0L, "2", 1L, "3", 0L, "4", 0L, "5", 1L), row.get("distribution"));
        var rate = items(reports.responseRate(filter)).getFirst();
        assertEquals(3L, rate.get("invitations_sent"));
        assertEquals(2L, rate.get("responses"));
        assertEquals(new BigDecimal("0.67"), rate.get("response_rate"));
        byte[] workbook = reports.export(filter);
        assertEquals('P', workbook[0]); assertEquals('K', workbook[1]);
    }

    @Test
    void jwtLoginRejectsTamperingExpiryWrongRoleAndDisabledAccount() throws Exception {
        var login = auth.login("manager101", "test-only-password-000000");
        String token = "Bearer " + login.get("access_token");
        assertEquals(101L, auth.require(token, "QL_TRUNG_TAM").centerId());
        assertEquals("FORBIDDEN_SCOPE", assertThrows(ApiException.class, () -> auth.require(token, "MARKETING")).getCode());
        assertEquals("UNAUTHENTICATED", assertThrows(ApiException.class, () -> auth.login("manager101", "wrong-password")).getCode());
        assertThrows(ApiException.class, () -> auth.require(token + "tampered"));
        var expired = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder().issuer("smartcrm-l8")
                .subject("manager101").claim("role", "QL_TRUNG_TAM").claim("center_id", 101L)
                .expirationTime(Date.from(Instant.now().minusSeconds(1))).build());
        expired.sign(new MACSigner("test-only-jwt-secret-0000000000000000000000"));
        assertThrows(ApiException.class, () -> auth.require("Bearer " + expired.serialize()));
        jdbc.update("UPDATE app_user SET enabled = FALSE WHERE username = 'manager101'");
        assertThrows(ApiException.class, () -> auth.require(token));
    }

    @Test
    void reportingEndpointsEnforceCompleteContractRoleMatrixAndScope() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();
        String[] paths = {"/api/survey-stats/by-technician", "/api/survey-stats/by-center", "/api/survey-responses",
                "/api/survey-responses/export", "/api/survey-stats/response-rate"};
        String[] usernames = {"manager101", "marketing", "director"};
        int[][] expected = {{200, 403, 200, 403, 200}, {403, 403, 200, 200, 403}, {403, 200, 403, 403, 403}};
        for (String path : paths) mvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (int user = 0; user < usernames.length; user++) {
            String bearer = "Bearer " + auth.login(usernames[user], "test-only-password-000000").get("access_token");
            for (int endpoint = 0; endpoint < paths.length; endpoint++) {
                mvc.perform(get(paths[endpoint]).header("Authorization", bearer)).andExpect(status().is(expected[user][endpoint]));
            }
        }
        String manager = "Bearer " + auth.login("manager101", "test-only-password-000000").get("access_token");
        for (String path : List.of(paths[0], paths[2], paths[4], "/api/reference/technicians")) {
            mvc.perform(get(path).param("center_id", "102").header("Authorization", manager)).andExpect(status().isForbidden());
        }
        mvc.perform(get(paths[0]).param("from", "2026-10-02").param("to", "2026-10-01")
                .header("Authorization", manager)).andExpect(status().isBadRequest());
        mvc.perform(get(paths[2]).param("page", "0").header("Authorization", manager)).andExpect(status().isBadRequest());
        mvc.perform(get(paths[2]).param("score_min", "5").param("score_max", "2")
                .header("Authorization", manager)).andExpect(status().isBadRequest());
    }

    private void ticket(long id, long center, long tech) {
        jdbc.update("INSERT INTO ticket (ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES (?, ?,990201,?,?,501,'DA_DONG','2026-09-01 00:00:00')", id, "IT-" + id, center, tech);
    }

    private void invitation(long ticketId, String token, String status) {
        jdbc.update("INSERT INTO survey_invitation (ticket_id,token,status,created_at,sent_at,expires_at,send_attempts) VALUES (?,?,?,'2026-09-01 00:00:00','2026-09-01 00:00:00','2026-09-08 00:00:00',1)", ticketId, token.repeat(43), status);
    }

    private void response(long ticketId, int score, String at) {
        jdbc.update("INSERT INTO survey_response (ticket_id,score,comment,responded_at) VALUES (?,?,'Test comment',?)", ticketId, score, at);
    }

    private static ReportFilter filter(Long centerId, Long technicianId, int min, int max) {
        return ReportFilter.of(centerId, technicianId, LocalDate.of(2026,9,1), LocalDate.of(2026,9,30), min, max);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("items");
    }
}
