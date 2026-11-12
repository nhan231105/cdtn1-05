
package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.reference.entity.Ticket;
import com.mekongmobile.smartcrm.reference.repository.TicketRepository;
import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import com.mekongmobile.smartcrm.survey.exception.SurveyBusinessException;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;
import com.mekongmobile.smartcrm.survey.repository.CancelledSurveyInvitationRepository;
import com.mekongmobile.smartcrm.survey.repository.SurveyResponseRepository;
import com.mekongmobile.smartcrm.survey.repository.SurveyTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SurveyInvitationService {

    private static final ZoneId ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final TicketRepository ticketRepository;
    private final SurveyInvitationRepository invitationRepository;
    private final CancelledSurveyInvitationRepository cancelledInvitations;
    private final SurveyResponseRepository responseRepository;
    private final SurveyTemplateRepository templates;
    private final String surveyBaseUrl;

    @Value("${app.survey.expiry-days:7}")
    private long expiryDays = 7;

    @Autowired
    public SurveyInvitationService(
            TicketRepository ticketRepository,
            SurveyInvitationRepository invitationRepository,
            CancelledSurveyInvitationRepository cancelledInvitations,
            SurveyResponseRepository responseRepository,
            SurveyTemplateRepository templates,
            @Value("${app.survey.public-base-url}")
            String surveyBaseUrl
    ) {
        this.ticketRepository = ticketRepository;
        this.invitationRepository = invitationRepository;
        this.cancelledInvitations = cancelledInvitations;
        this.responseRepository = responseRepository;
        this.templates = templates;
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
        Ticket ticket = ticketRepository.lockById(ticketId)
                .orElseThrow(
                        SurveyBusinessException::ticketNotFound
                );

        // 3. Only closed tickets are eligible
        if (!"DA_DONG".equals(ticket.getStatus())) {
            throw SurveyBusinessException.ticketNotClosed(
                    ticket.getStatus()
            );
        }

        if (cancelledInvitations.existsByTicketId(ticketId)) {
            throw SurveyBusinessException.invitationCancelled();
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

        LocalDateTime expiresAt = createdAt.plusDays(expiryDays);

        SurveyTemplateRepository.Template template = templates.findActive()
                .orElseThrow(() -> new ApiException("ACTIVE_TEMPLATE_REQUIRED",
                        "Cần có một mẫu khảo sát đang hoạt động"));

        String token = generateSecureToken();

        SurveyInvitation invitation = new SurveyInvitation(
                ticketId,
                token,
                createdAt,
                expiresAt,
                template.title(),
                template.introText()
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

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listInvitations() {
        return invitationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(invitation -> toInvitationView(invitation,
                        ticketRepository.findById(invitation.getTicketId())
                                .orElseThrow(SurveyBusinessException::ticketNotFound)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInvitation(long id) {
        SurveyInvitation invitation = invitationRepository.findById(id)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Không tìm thấy lời mời khảo sát"));
        Ticket ticket = ticketRepository.findById(invitation.getTicketId())
                .orElseThrow(SurveyBusinessException::ticketNotFound);
        return toInvitationView(invitation, ticket);
    }

    private Map<String, Object> toInvitationView(SurveyInvitation invitation, Ticket ticket) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invitation_id", invitation.getInvitationId());
        result.put("ticket_id", invitation.getTicketId());
        result.put("ticket_code", ticket.getTicketCode());
        result.put("survey_url", surveyBaseUrl + "/" + invitation.getToken());
        result.put("status", invitation.getStatus());
        result.put("channel", invitation.getChannel());
        result.put("send_attempts", invitation.getSendAttempts());
        result.put("created_at", invitation.getCreatedAt());
        result.put("sent_at", invitation.getSentAt());
        result.put("expires_at", invitation.getExpiresAt());
        result.put("survey_title", invitation.getSurveyTitle());
        result.put("survey_intro", invitation.getSurveyIntro());
        return result;
    }

    @Transactional
    public Map<String, Object> updateChannel(long id, String channel) {
        SurveyInvitation invitation = invitationRepository.lockById(id)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Không tìm thấy lời mời khảo sát"));
        try {
            invitation.updateChannel(channel);
        } catch (IllegalStateException exception) {
            throw new ApiException("INVITATION_CLOSED", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            throw ApiException.validation(exception.getMessage());
        }
        invitationRepository.flush();
        return getInvitation(id);
    }

    @Transactional
    public void deleteUnsent(long id) {
        SurveyInvitation invitation = invitationRepository.lockById(id)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Không tìm thấy lời mời khảo sát"));
        ticketRepository.lockById(invitation.getTicketId())
                .orElseThrow(SurveyBusinessException::ticketNotFound);
        if (!"CHO_GUI".equals(invitation.getStatus()) || invitation.getSendAttempts() != 0
                || invitation.getSentAt() != null || responseRepository.respondedAt(invitation.getTicketId()).isPresent()) {
            throw new ApiException("INVITATION_CLOSED", "Chỉ có thể xóa lời mời chưa gửi và chưa có phản hồi");
        }
        cancelledInvitations.save(invitation.getTicketId(), invitation.getInvitationId());
        invitationRepository.delete(invitation);
        invitationRepository.flush();
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
