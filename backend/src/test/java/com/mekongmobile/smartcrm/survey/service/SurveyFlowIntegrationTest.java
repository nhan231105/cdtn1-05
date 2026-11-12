package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.survey.gateway.SurveyMessageGateway;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import com.mekongmobile.smartcrm.survey.security.SurveyRateLimitFilter;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SurveyFlowIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis());
    @Autowired JdbcTemplate jdbc;
    @Autowired SurveyInvitationService invitations;
    @Autowired SurveyDeliveryService delivery;
    @Autowired SurveyResponseService responses;
    @Autowired SurveyInvitationRepository repository;
    @Autowired WebApplicationContext context;
    @Autowired SurveyRateLimitFilter rateLimit;
    @MockitoBean SurveyMessageGateway gateway;
    @LocalServerPort int port;

    private record Fixture(long ticketId, long invitationId, String token) {}
    private Fixture fixture(String phone) {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO customer(customer_id,phone) VALUES(?,?)", id, phone == null ? Long.toString(id) : phone);
        jdbc.update("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES(?,?,?,101,301,501,'DA_DONG',?)",
                id, "T" + id, id, LocalDateTime.now());
        var created = invitations.createInvitation(id);
        return new Fixture(id, created.invitationId(), repository.findById(created.invitationId()).orElseThrow().getToken());
    }
    private Fixture validFixture() {
        long suffix = IDS.incrementAndGet() % 1000000000;
        return fixture(String.format("0%09d", suffix));
    }

    private HttpResponse<String> send(String method, String path, String body, String token, String serviceKey) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (serviceKey != null) builder.header("X-Service-Key", serviceKey);
        if (body != null) builder.header("Content-Type", "application/json");
        var publisher = body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body);
        return HttpClient.newHttpClient().send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String marketingToken() throws Exception {
        var login = send("POST", "/api/auth/login", "{\"username\":\"marketing\",\"password\":\"test-only-password-000000\"}", null, null);
        assertEquals(200, login.statusCode(), login.body());
        return jsonValue(login.body(), "access_token");
    }

    private static String jsonValue(String json, String name) {
        var matcher = java.util.regex.Pattern.compile("\\\"" + name + "\\\":\\\"([^\\\"]*)\\\"").matcher(json);
        assertTrue(matcher.find(), "Missing JSON field " + name + ": " + json);
        return matcher.group(1);
    }

    private static long jsonLong(String json, String name) {
        var matcher = java.util.regex.Pattern.compile("\\\"" + name + "\\\":([0-9]+)").matcher(json);
        assertTrue(matcher.find(), "Missing JSON field " + name + ": " + json);
        return Long.parseLong(matcher.group(1));
    }

    @Test void sendDefaultsAndSuccessAreIdempotent() {
        var f = validFixture();
        assertEquals("DA_GUI", delivery.send(f.invitationId, "ZALO").status());
        assertEquals(1, delivery.send(f.invitationId, "ZALO").sendAttempts());
        verify(gateway, times(1)).send(eq("ZALO"), anyString(), endsWith(f.token));
        assertEquals("DA_GUI", repository.findById(f.invitationId).orElseThrow().getStatus());
    }

    @Test void invalidPhonePersistsFailureWithoutCallingGateway() {
        var f = fixture("bad" + IDS.incrementAndGet());
        ApiException failure = assertThrows(ApiException.class, () -> delivery.send(f.invitationId, "SMS"));
        assertEquals("INVALID_PHONE", failure.getCode());
        var saved = repository.findById(f.invitationId).orElseThrow();
        assertEquals("GUI_LOI", saved.getStatus());
        assertEquals(1, saved.getSendAttempts());
        verifyNoInteractions(gateway);
    }

    @Test void gatewayFailureCommitsAndEnforcesFiveMinutesAndThreeAttempts() {
        var f = validFixture();
        doThrow(new IllegalStateException("offline")).when(gateway).send(anyString(), anyString(), anyString());
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertEquals("SMS_GATEWAY_ERROR", assertThrows(ApiException.class,
                    () -> delivery.send(f.invitationId, "SMS")).getCode());
            assertEquals(attempt, repository.findById(f.invitationId).orElseThrow().getSendAttempts());
            assertEquals("INVITATION_CLOSED", assertThrows(ApiException.class,
                    () -> delivery.send(f.invitationId, "SMS")).getCode());
            jdbc.update("UPDATE survey_invitation SET last_attempt_at=? WHERE invitation_id=?",
                    LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(6), f.invitationId);
        }
        verify(gateway, times(3)).send(anyString(), anyString(), anyString());
    }

    @Test void submissionSanitizesAndIsImmutable() {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        jdbc.update("UPDATE ticket SET device_id=999999999 WHERE ticket_id=?", f.ticketId);
        assertNotNull(responses.form(f.token).get("center_name"));
        assertEquals("Thiết bị #999999999", responses.form(f.token).get("device_name"));
        var result = responses.submit(f.token, 5, " <b>Tốt</b> ");
        assertEquals("Tốt", result.get("comment"));
        assertEquals("DA_PHAN_HOI", repository.findById(f.invitationId).orElseThrow().getStatus());
        assertEquals("ALREADY_RESPONDED", assertThrows(ApiException.class,
                () -> responses.submit(f.token, 1, "changed")).getCode());
        assertEquals(5, jdbc.queryForObject("SELECT score FROM survey_response WHERE ticket_id=?", Integer.class, f.ticketId));
    }

    @Test void rejectsInvalidValuesAndExpiredOrUnknownTokens() {
        var f = validFixture();
        for (Object score : List.of(0, 6, 2.5, "4"))
            assertEquals("VALIDATION_FAILED", assertThrows(ApiException.class,
                    () -> responses.submit(f.token, score, null)).getCode());
        assertThrows(ApiException.class, () -> responses.submit(f.token, 3, "a".repeat(1001)));
        assertEquals("SURVEY_NOT_FOUND", assertThrows(ApiException.class, () -> responses.form("bad")).getCode());
        jdbc.update("UPDATE survey_invitation SET expires_at=? WHERE invitation_id=?", LocalDateTime.now().minusDays(1), f.invitationId);
        assertEquals("SURVEY_EXPIRED", assertThrows(ApiException.class, () -> responses.form(f.token)).getCode());
        assertEquals("INVITATION_CLOSED", assertThrows(ApiException.class, () -> delivery.send(f.invitationId, "SMS")).getCode());
    }

    @Test void twoConcurrentSubmissionsSaveExactlyOneResponse() throws Exception {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            Callable<String> task = () -> {
                start.await();
                try { responses.submit(f.token, 4, null); return "CREATED"; }
                catch (ApiException failure) { return failure.getCode(); }
            };
            var first = executor.submit(task);
            var second = executor.submit(task);
            start.countDown();
            var results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertTrue(results.contains("CREATED"));
            assertTrue(results.contains("ALREADY_RESPONDED"));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM survey_response WHERE ticket_id=?", Integer.class, f.ticketId));
        }
    }

    @Test void oneHundredConcurrentSubmissionsCreateExactlyOneResponse() throws Exception {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(rateLimit).build();
        try (var executor = Executors.newFixedThreadPool(100)) {
            var start = new CountDownLatch(1);
            var tasks = new java.util.ArrayList<Future<Integer>>();
            for (int i = 0; i < 100; i++) {
                final int client = i;
                tasks.add(executor.submit(() -> {
                    start.await();
                    return mvc.perform(post("/api/surveys/{token}/responses", f.token)
                            .with(request -> { request.setRemoteAddr("192.0.2." + (client + 1)); return request; })
                            .contentType(MediaType.APPLICATION_JSON).content("{\"score\":4}"))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            var results = new java.util.ArrayList<Integer>();
            for (var task : tasks) results.add(task.get(60, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(status -> status == 201).count());
            assertEquals(99, results.stream().filter(status -> status == 409).count());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM survey_response WHERE ticket_id=?", Integer.class, f.ticketId));
        }
    }

    @Test void databaseFailureRollsBackResponseAndInvitationTogether() {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        jdbc.execute("CREATE TRIGGER test_fail_invitation BEFORE UPDATE ON survey_invitation FOR EACH ROW BEGIN IF NEW.ticket_id="
                + f.ticketId + " AND NEW.status='DA_PHAN_HOI' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test failure'; END IF; END");
        try {
            assertThrows(RuntimeException.class, () -> responses.submit(f.token, 4, null));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM survey_response WHERE ticket_id=?", Integer.class, f.ticketId));
            assertEquals("DA_GUI", repository.findById(f.invitationId).orElseThrow().getStatus());
        } finally { jdbc.execute("DROP TRIGGER test_fail_invitation"); }
        assertNotNull(responses.submit(f.token, 4, null).get("response_id"));
    }

    @Test void reminderUsesSameTokenOnlyOnceAndKeepsOriginalSentTime() {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        delivery.remind(f.invitationId);
        assertNull(repository.findById(f.invitationId).orElseThrow().getRemindedAt());
        LocalDateTime sent = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(73).withNano(0);
        jdbc.update("UPDATE survey_invitation SET sent_at=? WHERE invitation_id=?", sent, f.invitationId);
        delivery.remind(f.invitationId);
        delivery.remind(f.invitationId);
        var saved = repository.findById(f.invitationId).orElseThrow();
        assertEquals(sent, saved.getSentAt());
        assertNotNull(saved.getRemindedAt());
        verify(gateway, times(2)).send(eq("SMS"), anyString(), endsWith(f.token));
    }

    @Test void reminderFailureRetriesAfterFiveMinutesAndStopsAfterThreeAttempts() {
        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        jdbc.update("UPDATE survey_invitation SET sent_at=? WHERE invitation_id=?",
                LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusHours(73), f.invitationId);
        doThrow(new IllegalStateException("offline")).when(gateway).send(anyString(), anyString(), anyString());
        for (int attempt = 1; attempt <= 3; attempt++) {
            delivery.remind(f.invitationId);
            delivery.remind(f.invitationId);
            var saved = repository.findById(f.invitationId).orElseThrow();
            assertEquals(attempt, saved.getReminderAttempts());
            assertNull(saved.getRemindedAt());
            assertEquals("DA_GUI", saved.getStatus());
            jdbc.update("UPDATE survey_invitation SET last_reminder_attempt_at=? WHERE invitation_id=?",
                    LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(6), f.invitationId);
        }
        delivery.remind(f.invitationId);
        verify(gateway, times(4)).send(anyString(), anyString(), anyString());
    }

    @Test void publicHttpRateLimitAndServiceAuthentication() throws Exception {
        var client = HttpClient.newHttpClient();
        String base = "http://localhost:" + port;
        var unauthenticated = client.send(HttpRequest.newBuilder(URI.create(base + "/api/survey-invitations"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"ticket_id\":401}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, unauthenticated.statusCode());
        for (int request = 1; request <= 11; request++) {
            var result = client.send(HttpRequest.newBuilder(URI.create(base + "/api/surveys/bad")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(request <= 10 ? 404 : 429, result.statusCode());
        }
    }

    @Test void unsentInvitationCannotOpenOrSubmitAndConcurrentCloseEventsAreUnique() throws Exception {
        var f = validFixture();
        assertEquals("SURVEY_NOT_FOUND", assertThrows(ApiException.class, () -> responses.form(f.token)).getCode());
        assertEquals("SURVEY_NOT_FOUND", assertThrows(ApiException.class, () -> responses.submit(f.token, 4, null)).getCode());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> assertThrows(com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException.class,
                    () -> invitations.createInvitation(f.ticketId)));
            var second = executor.submit(() -> assertThrows(com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException.class,
                    () -> invitations.createInvitation(f.ticketId)));
            assertEquals("INVITATION_ALREADY_EXISTS", first.get().getCode().name());
            assertEquals("INVITATION_ALREADY_EXISTS", second.get().getCode().name());
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM survey_invitation WHERE ticket_id=?", Integer.class, f.ticketId));
        }
    }

    @Test void schedulerProcessesClosedTicketsTwiceWithoutDuplicateInvitationsOrSends() {
        long id = IDS.incrementAndGet();
        jdbc.update("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES(?,?,201,101,301,501,'DA_DONG',?)",
                id, "J" + id, LocalDateTime.now());
        var scheduler = new SurveyScheduler(new com.mekongmobile.smartcrm.survey.repository.SurveyJobRepository(jdbc), invitations, delivery);
        scheduler.tick();
        var first = repository.findByTicketId(id).orElseThrow();
        assertEquals("DA_GUI", first.getStatus());
        scheduler.tick();
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM survey_invitation WHERE ticket_id=?", Integer.class, id));
        assertEquals(1, repository.findByTicketId(id).orElseThrow().getSendAttempts());
        verify(gateway, times(1)).send(eq("SMS"), anyString(), endsWith(first.getToken()));
    }

    @Test void invitationCrudOnlyAllowsSafePreDeliveryChangesAndKeepsCancellationTombstone() throws Exception {
        var f = validFixture();
        String path = "/api/survey-invitations/" + f.invitationId;
        assertEquals(401, send("GET", "/api/survey-invitations", null, null, null).statusCode());
        assertEquals(401, send("GET", path, null, null, null).statusCode());
        String key = "test-only-service-key-00000000000000000000";
        var list = send("GET", "/api/survey-invitations", null, null, key);
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains("\"invitation_id\":" + f.invitationId), list.body());
        var detail = send("GET", path, null, null, key);
        assertEquals(200, detail.statusCode());
        assertEquals("CHO_GUI", jsonValue(detail.body(), "status"));
        var update = send("PATCH", path, "{\"channel\":\"ZALO\"}", null, key);
        assertEquals(200, update.statusCode(), update.body());
        assertEquals("ZALO", jsonValue(update.body(), "channel"));
        assertEquals(204, send("DELETE", path, null, null, key).statusCode());
        assertTrue(send("GET", path, null, null, key).body().contains("\"code\":\"INVITATION_NOT_FOUND\""));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM cancelled_survey_invitation WHERE ticket_id=?", Integer.class, f.ticketId));
        assertEquals("INVITATION_CANCELLED", assertThrows(com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException.class,
                () -> invitations.createInvitation(f.ticketId)).getCode().name());

        var sent = validFixture();
        delivery.send(sent.invitationId, "SMS");
        String sentPath = "/api/survey-invitations/" + sent.invitationId;
        assertEquals(409, send("PATCH", sentPath, "{\"channel\":\"ZALO\"}", null, key).statusCode());
        assertEquals(409, send("DELETE", sentPath, null, null, key).statusCode());
        assertEquals("DA_GUI", repository.findById(sent.invitationId).orElseThrow().getStatus());
    }

    @Test void marketingCanCrudTemplatesAndIssuedInvitationsKeepTheirSnapshot() throws Exception {
        assertEquals(401, send("GET", "/api/survey-templates", null, null, null).statusCode());
        var managerLogin = send("POST", "/api/auth/login", "{\"username\":\"manager101\",\"password\":\"test-only-password-000000\"}", null, null);
        String managerToken = jsonValue(managerLogin.body(), "access_token");
        assertEquals(403, send("GET", "/api/survey-templates", null, managerToken, null).statusCode());

        String marketing = marketingToken();
        String path = "/api/survey-templates";
        var invalid = send("POST", path, "{\"name\":\" \",\"title\":\"x\",\"intro_text\":\"y\",\"is_active\":false}", marketing, null);
        assertEquals(400, invalid.statusCode());
        var created = send("POST", path, "{\"name\":\"CRUD-" + IDS.incrementAndGet() + "\",\"title\":\"Tiêu đề mới\",\"intro_text\":\"Mô tả mẫu\",\"is_active\":false}", marketing, null);
        assertEquals(201, created.statusCode(), created.body());
        long templateId = jsonLong(created.body(), "template_id");
        String itemPath = path + "/" + templateId;
        String templateName = jsonValue(created.body(), "name");
        assertEquals(409, send("POST", path, "{\"name\":\"" + templateName + "\",\"title\":\"Trùng\",\"intro_text\":\"Mô tả\",\"is_active\":false}", marketing, null).statusCode());
        var activated = send("PUT", itemPath, "{\"name\":\"" + templateName
                + "\",\"title\":\"Tiêu đề mới\",\"intro_text\":\"Mô tả mẫu\",\"is_active\":true}", marketing, null);
        assertEquals(200, activated.statusCode(), activated.body());
        assertEquals(409, send("DELETE", itemPath, null, marketing, null).statusCode());

        var f = validFixture();
        delivery.send(f.invitationId, "SMS");
        assertEquals("Tiêu đề mới", responses.form(f.token).get("survey_title"));
        var edited = send("PUT", itemPath, "{\"name\":\"" + templateName
                + "\",\"title\":\"Bản cập nhật\",\"intro_text\":\"Mô tả đã sửa\",\"is_active\":true}", marketing, null);
        assertEquals(200, edited.statusCode(), edited.body());
        assertEquals("Tiêu đề mới", responses.form(f.token).get("survey_title"));

        var defaultUpdated = send("PUT", path + "/1", "{\"name\":\"CSAT sau bảo hành\",\"title\":\"Trải nghiệm của bạn thế nào?\",\"intro_text\":\"Hãy chia sẻ cảm nhận về dịch vụ bảo hành của Mekong Mobile.\",\"is_active\":true}", marketing, null);
        assertEquals(200, defaultUpdated.statusCode(), defaultUpdated.body());
        assertEquals(409, send("PUT", path + "/1", "{\"name\":\"CSAT sau bảo hành\",\"title\":\"Trải nghiệm của bạn thế nào?\",\"intro_text\":\"Hãy chia sẻ cảm nhận về dịch vụ bảo hành của Mekong Mobile.\",\"is_active\":false}", marketing, null).statusCode());
        assertEquals(204, send("DELETE", itemPath, null, marketing, null).statusCode());
        assertEquals(404, send("GET", itemPath, null, marketing, null).statusCode());
        assertEquals(409, send("DELETE", path + "/1", null, marketing, null).statusCode());
        String list = send("GET", path, null, marketing, null).body();
        assertEquals(1, list.split("\\\"template_id\\\"", -1).length - 1, list);
    }
}
