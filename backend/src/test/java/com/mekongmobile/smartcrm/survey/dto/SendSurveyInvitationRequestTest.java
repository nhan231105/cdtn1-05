
package com.mekongmobile.smartcrm.survey.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendSurveyInvitationRequestTest {

    @Test
    void shouldDefaultToSms() {
        var request =
                new SendSurveyInvitationRequest(null);

        assertEquals("SMS", request.effectiveChannel());
    }

    @Test
    void shouldAcceptSmsAndZalo() {
        try (var factory =
                     Validation.buildDefaultValidatorFactory()) {

            var validator = factory.getValidator();

            assertTrue(validator.validate(
                    new SendSurveyInvitationRequest("SMS")
            ).isEmpty());

            assertTrue(validator.validate(
                    new SendSurveyInvitationRequest("ZALO")
            ).isEmpty());
        }
    }

    @Test
    void shouldRejectInvalidChannel() {
        try (var factory =
                     Validation.buildDefaultValidatorFactory()) {

            var validator = factory.getValidator();

            assertFalse(validator.validate(
                    new SendSurveyInvitationRequest("EMAIL")
            ).isEmpty());

            assertFalse(validator.validate(
                    new SendSurveyInvitationRequest("")
            ).isEmpty());
        }
    }
}
