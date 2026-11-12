package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reference.repository.CustomerRepository;
import com.mekongmobile.smartcrm.reference.repository.TicketRepository;
import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.survey.dto.SendSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import com.mekongmobile.smartcrm.survey.gateway.SurveyMessageGateway;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SurveyDeliveryService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SurveyDeliveryService.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SurveyInvitationRepository invitations;
    private final TicketRepository tickets;
    private final CustomerRepository customers;
    private final SurveyMessageGateway gateway;
    private final String baseUrl;

    public SurveyDeliveryService(SurveyInvitationRepository invitations, TicketRepository tickets,
            CustomerRepository customers, SurveyMessageGateway gateway,
            @Value("${app.survey.public-base-url}") String baseUrl) {
        this.invitations = invitations;
        this.tickets = tickets;
        this.customers = customers;
        this.gateway = gateway;
        this.baseUrl = baseUrl;
    }

    // Business errors must commit GUI_LOI/attempt metadata before returning HTTP errors.
    @Transactional(noRollbackFor = ApiException.class)
    public SendSurveyInvitationResponse send(Long id, String channel) {
        if (!"SMS".equals(channel) && !"ZALO".equals(channel))
            throw ApiException.validation("Kênh gửi chỉ được là SMS hoặc ZALO");
        SurveyInvitation invitation = invitations.lockById(id)
                .orElseThrow(() -> new ApiException("INVITATION_NOT_FOUND", "Không tìm thấy lời mời"));
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!invitation.getExpiresAt().isAfter(now) || "DA_PHAN_HOI".equals(invitation.getStatus()))
            throw closed();
        // Idempotent delivery: repeated requests cannot send a second successful message.
        if ("DA_GUI".equals(invitation.getStatus())) return response(invitation);
        if (invitation.getSendAttempts() >= 3) throw closed();
        if (invitation.getLastAttemptAt() != null && invitation.getLastAttemptAt().plusMinutes(5).isAfter(now))
            throw new ApiException("INVITATION_CLOSED", "Vui lòng chờ 5 phút trước khi thử gửi lại");
        String phone = phone(invitation);
        invitation.recordAttemptAt(now);
        if (!SurveyPhoneValidator.isValid(phone)) {
            invitation.markSendFailed(channel);
            // No automatic retry for invalid reference data.
            throw new ApiException("INVALID_PHONE", "Số điện thoại không đúng định dạng chuẩn");
        }
        try {
            gateway.send(channel, phone, baseUrl + "/" + invitation.getToken());
        } catch (RuntimeException failure) {
            invitation.markSendFailed(channel);
            throw new ApiException("SMS_GATEWAY_ERROR", "Cổng gửi tin lỗi; tối đa 3 lần, cách nhau 5 phút");
        }
        invitation.markSent(channel, now);
        return response(invitation);
    }

    @Transactional
    public void remind(Long id) {
        SurveyInvitation invitation = invitations.lockById(id).orElseThrow();
        LocalDateTime now = LocalDateTime.now(ZONE);
        if (!"DA_GUI".equals(invitation.getStatus()) || invitation.getRemindedAt() != null
                || invitation.getSentAt() == null || invitation.getSentAt().plusHours(72).isAfter(now)
                || !invitation.getExpiresAt().isAfter(now)) return;
        if (invitation.getReminderAttempts() >= 3 || (invitation.getLastReminderAttemptAt() != null
                && invitation.getLastReminderAttemptAt().plusMinutes(5).isAfter(now))) return;
        invitation.recordReminderAttempt(now);
        String phone = phone(invitation);
        if (!SurveyPhoneValidator.isValid(phone)) return;
        try {
            gateway.send(invitation.getChannel(), phone, baseUrl + "/" + invitation.getToken());
            invitation.markReminded(now);
        }
        catch (RuntimeException failure) {
            log.warn("Reminder gateway failed for invitation {}", id);
            // Keep original successful delivery status and response-rate denominator intact.
        }
    }

    private String phone(SurveyInvitation invitation) {
        var ticket = tickets.findById(invitation.getTicketId()).orElseThrow();
        return customers.findById(ticket.getCustomerId()).orElseThrow().getPhone();
    }
    private ApiException closed() { return new ApiException("INVITATION_CLOSED", "Lời mời không cho phép gửi nữa"); }
    private SendSurveyInvitationResponse response(SurveyInvitation i) {
        return new SendSurveyInvitationResponse(i.getInvitationId(), i.getStatus(), i.getChannel(),
                i.getSentAt() == null ? null : i.getSentAt().atZone(ZONE).toOffsetDateTime(), i.getSendAttempts());
    }
}
