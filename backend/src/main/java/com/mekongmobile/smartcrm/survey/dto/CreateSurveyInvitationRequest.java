
package com.mekongmobile.smartcrm.survey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSurveyInvitationRequest(

        @JsonProperty("ticket_id")
        @NotNull(message = "ticket_id không được để trống")
        @Positive(message = "ticket_id phải là số nguyên dương")
        Long ticketId

) {
}
