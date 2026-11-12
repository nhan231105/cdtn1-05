package com.mekongmobile.smartcrm.survey.service;

import com.mekongmobile.smartcrm.reporting.ApiException;
import com.mekongmobile.smartcrm.survey.entity.SurveyInvitation;
import com.mekongmobile.smartcrm.survey.repository.SurveyInvitationRepository;
import com.mekongmobile.smartcrm.survey.repository.SurveyResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SurveyResponseService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SurveyInvitationRepository invitations;
    private final SurveyResponseRepository responses;
    public SurveyResponseService(SurveyInvitationRepository invitations, SurveyResponseRepository responses) {
        this.invitations = invitations;
        this.responses = responses;
    }

    @Transactional
    public Map<String, Object> form(String token) {
        SurveyInvitation i = available(token);
        Map<String, Object> result = responses.formDetails(i.getTicketId());
        Object closed = result.get("closed_at");
        if (closed instanceof java.sql.Timestamp timestamp) result.put("closed_at", offset(timestamp.toLocalDateTime()));
        result.put("expires_at", offset(i.getExpiresAt()));
        result.put("survey_title", i.getSurveyTitle());
        result.put("survey_intro", i.getSurveyIntro());
        result.put("score_min", 1);
        result.put("score_max", 5);
        result.put("comment_max_length", 1000);
        return result;
    }

    @Transactional
    public Map<String, Object> submit(String token, Object score, Object comment) {
        if (!(score instanceof Integer value) || value < 1 || value > 5)
            throw new ApiException("VALIDATION_FAILED", "Dữ liệu không hợp lệ",
                    Map.of("score", "Vui lòng chọn điểm từ 1 đến 5"));
        if (comment != null && (!(comment instanceof String) || ((String) comment).length() > 1000))
            throw new ApiException("VALIDATION_FAILED", "Dữ liệu không hợp lệ",
                    Map.of("comment", "Nhận xét tối đa 1000 ký tự"));
        SurveyInvitation i = available(token);
        String clean = comment == null ? null : ((String) comment).replaceAll("(?s)<[^>]*>", "").trim();
        LocalDateTime now = LocalDateTime.now(ZONE).withNano(0);
        long responseId = responses.insert(i.getTicketId(), value, clean, now);
        i.markResponded();
        invitations.flush();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("response_id", responseId);
        result.put("ticket_code", responses.ticketCode(i.getTicketId()));
        result.put("score", value);
        result.put("comment", clean);
        result.put("responded_at", offset(now));
        result.put("message", "Cảm ơn bạn đã phản hồi");
        return result;
    }

    private SurveyInvitation available(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw missing();
        SurveyInvitation i = invitations.lockByToken(token).orElseThrow(this::missing);
        var existing = responses.respondedAt(i.getTicketId());
        if (existing.isPresent()) {
            var responded = existing.get();
            throw new ApiException("ALREADY_RESPONDED", "Phiếu đã có phản hồi", Map.of("responded_at", offset(responded)));
        }
        if (!i.getExpiresAt().isAfter(LocalDateTime.now(ZONE)))
            throw new ApiException("SURVEY_EXPIRED", "Liên kết khảo sát đã hết hạn");
        if (!"DA_GUI".equals(i.getStatus()) || !responses.isTicketClosed(i.getTicketId())) throw missing();
        return i;
    }
    private Object offset(LocalDateTime time) { return time.atZone(ZONE).toOffsetDateTime(); }
    private ApiException missing() { return new ApiException("SURVEY_NOT_FOUND", "Liên kết không hợp lệ"); }
}
