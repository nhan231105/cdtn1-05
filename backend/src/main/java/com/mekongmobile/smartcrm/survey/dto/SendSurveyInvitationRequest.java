
package com.mekongmobile.smartcrm.survey.dto;

import jakarta.validation.constraints.Pattern;

public record SendSurveyInvitationRequest(

        @Pattern(
                regexp = "SMS|ZALO",
                message = "channel phải là SMS hoặc ZALO"
        )
        String channel

) {
    public String effectiveChannel() {
        return channel == null ? "SMS" : channel;
    }
}
