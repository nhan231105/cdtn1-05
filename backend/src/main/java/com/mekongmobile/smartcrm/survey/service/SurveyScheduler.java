package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.mekongmobile.smartcrm.survey.repository.SurveyJobRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name = "app.survey.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SurveyScheduler {
    private static final Logger log = LoggerFactory.getLogger(SurveyScheduler.class);
    private final SurveyJobRepository jobs;
    private final SurveyInvitationService invitations;
    private final SurveyDeliveryService delivery;
    public SurveyScheduler(SurveyJobRepository jobs, SurveyInvitationService invitations, SurveyDeliveryService delivery) {
        this.jobs = jobs;
        this.invitations = invitations;
        this.delivery = delivery;
    }

    @Scheduled(fixedDelayString = "${app.survey.poll-ms:30000}")
    public void tick() {
        // L2/L4 is read-only; polling closed tickets is the local adapter for its close event.
        for (Long id : jobs.closedTicketsWithoutInvitation()) {
            try { invitations.createInvitation(id); }
            catch (SurveyBusinessException conflict) { log.debug("Close event already processed: {}", id); }
        }
        var now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (var job : jobs.pendingDeliveries(now)) {
            try { delivery.send(job.invitationId(), job.channel()); }
            catch (ApiException failure) { log.warn("Delivery {}: {}", job.invitationId(), failure.getCode()); }
        }
        for (Long id : jobs.pendingReminders(now)) delivery.remind(id);
    }
}
