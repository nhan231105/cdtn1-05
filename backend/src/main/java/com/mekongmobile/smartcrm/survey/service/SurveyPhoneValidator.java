
package com.mekongmobile.smartcrm.survey.service;

import java.util.regex.Pattern;

public final class SurveyPhoneValidator {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0[0-9]{9}$");

    private SurveyPhoneValidator() {
        // Utility class
    }

    public static boolean isValid(String phone) {
        return phone != null
                && PHONE_PATTERN.matcher(phone).matches();
    }
}
