package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.survey.gateway.SurveyMessageGateway;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;
import com.mekongmobile.smartcrm.survey.security.SurveyRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SurveyApiLoadIntegrationTest {
    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis() * 1_000L);
    @Autowired JdbcTemplate jdbc;
    @Autowired SurveyInvitationService invitations;
    @Autowired SurveyDeliveryService delivery;
    @Autowired SurveyInvitationRepository repository;
    @Autowired WebApplicationContext context;
    @Autowired SurveyRateLimitFilter rateLimit;
    @MockitoBean SurveyMessageGateway gateway;

    private List<String> prepare(int count) {
        var tokens = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            long id = IDS.incrementAndGet();
            jdbc.update("INSERT INTO customer(customer_id,phone) VALUES(?,?)", id, String.format("0%09d", id % 1_000_000_000L));
            jdbc.update("INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES(?,?,?,101,301,501,'DA_DONG',?)",
                    id, "L" + id, id, LocalDateTime.now());
            var created = invitations.createInvitation(id);
            delivery.send(created.invitationId(), "SMS");
            tokens.add(repository.findByTicketId(id).orElseThrow().getToken());
        }
        return tokens;
    }

    @Test
    void formApiRespondsToOneHundredConcurrentClientsUnderTwoSecondsAtLocalServletLayer() throws Exception {
        List<String> tokens = prepare(100);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(rateLimit).build();
        var samples = new long[tokens.size()];
        int[] statuses = concurrent(mvc, tokens, false, samples);
        assertEquals(100, Arrays.stream(statuses).filter(status -> status == 200).count());
        long p95 = percentile95(samples);
        System.out.printf("NFR01 local servlet baseline: clients=100 p95_ms=%d max_ms=%d network=MockMvc-not-4G%n",
                p95, Arrays.stream(samples).max().orElse(0));
        assertTrue(p95 <= 2_000, "Form API local p95 was " + p95 + " ms");
    }

    @Test
    void responseApiAcceptsFiftyConcurrentUniqueTicketsUnderFiveHundredMillisecondsAtLocalServletLayer() throws Exception {
        List<String> tokens = prepare(50);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(rateLimit).build();
        var samples = new long[tokens.size()];
        int[] statuses = concurrent(mvc, tokens, true, samples);
        assertEquals(50, Arrays.stream(statuses).filter(status -> status == 201).count());
        long p95 = percentile95(samples);
        System.out.printf("NFR02 local servlet baseline: clients=50 p95_ms=%d max_ms=%d network=MockMvc-not-4G%n",
                p95, Arrays.stream(samples).max().orElse(0));
        assertTrue(p95 <= 500, "Response API local p95 was " + p95 + " ms");
    }

    private static int[] concurrent(MockMvc mvc, List<String> tokens, boolean submit, long[] samples) throws Exception {
        int[] statuses = new int[tokens.size()];
        try (var executor = Executors.newFixedThreadPool(tokens.size())) {
            var start = new CountDownLatch(1);
            var tasks = new ArrayList<Future<Sample>>();
            for (int i = 0; i < tokens.size(); i++) {
                final int index = i;
                tasks.add(executor.submit(() -> {
                    start.await();
                    long began = System.nanoTime();
                    var builder = submit
                            ? post("/api/surveys/{token}/responses", tokens.get(index)).contentType(MediaType.APPLICATION_JSON).content("{\"score\":5}")
                            : get("/api/surveys/{token}", tokens.get(index));
                    var result = mvc.perform(builder.with(request -> {
                        request.setRemoteAddr("198.51.100." + (index + 1));
                        return request;
                    })).andReturn();
                    return new Sample(index, result.getResponse().getStatus(), (System.nanoTime() - began) / 1_000_000);
                }));
            }
            start.countDown();
            for (var task : tasks) {
                Sample sample = task.get();
                statuses[sample.index()] = sample.status();
                samples[sample.index()] = sample.elapsedMs();
            }
        }
        return statuses;
    }

    private static long percentile95(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[(int) Math.ceil(sorted.length * 0.95) - 1];
    }

    private record Sample(int index, int status, long elapsedMs) {}
}
