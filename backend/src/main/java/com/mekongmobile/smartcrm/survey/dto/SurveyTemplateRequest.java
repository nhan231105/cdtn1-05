package com.mekongmobile.smartcrm.survey.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SurveyTemplateRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 120) String title,
        @JsonProperty("intro_text") @NotBlank @Size(max = 1000) String introText,
        @JsonProperty("is_active") @NotNull Boolean active
) {}
