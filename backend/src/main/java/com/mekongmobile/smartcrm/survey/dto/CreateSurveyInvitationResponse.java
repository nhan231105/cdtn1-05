
package com.mekongmobile.smartcrm.survey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record CreateSurveyInvitationResponse(

        @JsonProperty("invitation_id")
        Long invitationId,

        @JsonProperty("ticket_id")
        Long ticketId,

        @JsonProperty("ticket_code")
        String ticketCode,

        @JsonProperty("status")
        String status,

        @JsonProperty("survey_url")
        String surveyUrl,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("expires_at")
        OffsetDateTime expiresAt

) {
}
