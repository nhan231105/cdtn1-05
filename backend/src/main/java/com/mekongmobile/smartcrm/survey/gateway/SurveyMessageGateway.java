
package com.mekongmobile.smartcrm.survey.gateway;

public interface SurveyMessageGateway {

    void send(String channel, String phone, String surveyUrl);

}
