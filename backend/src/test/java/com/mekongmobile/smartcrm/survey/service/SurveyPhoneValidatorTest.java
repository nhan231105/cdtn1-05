
package com.mekongmobile.smartcrm.survey.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SurveyPhoneValidatorTest {

    @Test
    void shouldAcceptValidPhone() {
        assertTrue(
                SurveyPhoneValidator.isValid("0900000001")
        );

        assertTrue(
                SurveyPhoneValidator.isValid("0123456789")
        );
    }

    @Test
    void shouldRejectInvalidPhone() {
        assertFalse(
                SurveyPhoneValidator.isValid("900000001")
        );

        assertFalse(
                SurveyPhoneValidator.isValid("09000000012")
        );

        assertFalse(
                SurveyPhoneValidator.isValid("09000abc01")
        );

        assertFalse(
                SurveyPhoneValidator.isValid("090 000 0001")
        );
    }

    @Test
    void shouldRejectMissingPhone() {
        assertFalse(
                SurveyPhoneValidator.isValid(null)
        );

        assertFalse(
                SurveyPhoneValidator.isValid("")
        );
    }
}
