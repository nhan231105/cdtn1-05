
package com.mekongmobile.smartcrm.survey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record SendSurveyInvitationResponse(

        @JsonProperty("invitation_id")
        Long invitationId,

        String status,

        String channel,

        @JsonProperty("sent_at")
        OffsetDateTime sentAt,

        @JsonProperty("send_attempts")
        int sendAttempts

) {
}
