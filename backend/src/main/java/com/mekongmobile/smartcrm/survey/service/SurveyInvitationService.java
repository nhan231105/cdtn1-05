
package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reference.entity.Ticket;
import com.mekongmobile.smartcrm.reference.repository.TicketRepository;
import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class SurveyInvitationService {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final TicketRepository ticketRepository;
    private final SurveyInvitationRepository invitationRepository;
    private final String surveyBaseUrl;

    public SurveyInvitationService(
            TicketRepository ticketRepository,
            SurveyInvitationRepository invitationRepository,
            @Value("${app.survey.public-base-url}")
            String surveyBaseUrl
    ) {
        this.ticketRepository = ticketRepository;
        this.invitationRepository = invitationRepository;
        this.surveyBaseUrl = surveyBaseUrl;
    }

    @Transactional
    public CreateSurveyInvitationResponse createInvitation(
            Long ticketId
    ) {
        // 1. Validate input
        if (ticketId == null || ticketId <= 0) {
            throw new IllegalArgumentException(
                    "ticketId must be positive"
            );
        }

        // 2. Find warranty ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(
                        SurveyBusinessException::ticketNotFound
                );

        // 3. Only closed tickets are eligible
        if (!"DA_DONG".equals(ticket.getStatus())) {
            throw SurveyBusinessException.ticketNotClosed(
                    ticket.getStatus()
            );
        }

        // 4. Prevent known duplicate invitations
        invitationRepository.findByTicketId(ticketId)
                .ifPresent(existing -> {
                    throw SurveyBusinessException
                            .invitationAlreadyExists(
                                    existing.getInvitationId()
                            );
                });

        // 5. Prepare invitation
        LocalDateTime createdAt = LocalDateTime.now(ZONE)
                .truncatedTo(ChronoUnit.SECONDS);

        LocalDateTime expiresAt = createdAt.plusDays(7);

        String token = generateSecureToken();

        SurveyInvitation invitation = new SurveyInvitation(
                ticketId,
                token,
                createdAt,
                expiresAt
        );

        // 6. Persist and obtain generated ID
        SurveyInvitation saved =
                invitationRepository.saveAndFlush(invitation);

        // 7. Build survey link
        String surveyUrl = surveyBaseUrl + "/" + token;

        // 8. Return API response DTO
        return new CreateSurveyInvitationResponse(
                saved.getInvitationId(),
                saved.getTicketId(),
                ticket.getTicketCode(),
                saved.getStatus(),
                surveyUrl,
                saved.getCreatedAt()
                        .atZone(ZONE)
                        .toOffsetDateTime(),
                saved.getExpiresAt()
                        .atZone(ZONE)
                        .toOffsetDateTime()
        );
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
