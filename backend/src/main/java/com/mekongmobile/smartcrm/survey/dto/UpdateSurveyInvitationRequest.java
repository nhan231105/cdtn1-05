package com.mekongmobile.smartcrm.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateSurveyInvitationRequest(
        @NotBlank @Pattern(regexp = "SMS|ZALO") String channel
) {}
