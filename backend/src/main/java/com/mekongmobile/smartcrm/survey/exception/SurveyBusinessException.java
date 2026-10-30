
package com.mekongmobile.smartcrm.survey.exception;

import java.util.Map;
import java.util.Objects;

public class SurveyBusinessException extends RuntimeException {

    public enum Code {
        TICKET_NOT_FOUND,
        TICKET_NOT_CLOSED,
        INVITATION_ALREADY_EXISTS
    }

    private final Code code;
    private final Map<String, Object> fields;

    private SurveyBusinessException(
            Code code,
            String message,
            Map<String, Object> fields
    ) {
        super(message);
        this.code = Objects.requireNonNull(code);
        this.fields = Map.copyOf(fields);
    }

    public Code getCode() {
        return code;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public static SurveyBusinessException ticketNotFound() {
        return new SurveyBusinessException(
                Code.TICKET_NOT_FOUND,
                "Không tìm thấy phiếu bảo hành",
                Map.of()
        );
    }

    public static SurveyBusinessException ticketNotClosed(
            String currentStatus
    ) {
        return new SurveyBusinessException(
                Code.TICKET_NOT_CLOSED,
                "Chỉ gửi khảo sát cho phiếu ở trạng thái ĐÃ ĐÓNG",
                Map.of("status", currentStatus)
        );
    }

    public static SurveyBusinessException invitationAlreadyExists(
            Long existingInvitationId
    ) {
        return new SurveyBusinessException(
                Code.INVITATION_ALREADY_EXISTS,
                "Phiếu bảo hành đã có lời mời khảo sát",
                Map.of("existing_invitation_id", existingInvitationId)
        );
    }
}
