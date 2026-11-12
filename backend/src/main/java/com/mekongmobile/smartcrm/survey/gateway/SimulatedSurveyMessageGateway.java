
package com.mekongmobile.smartcrm.survey.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimulatedSurveyMessageGateway
        implements SurveyMessageGateway {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SimulatedSurveyMessageGateway.class
            );

    private final boolean simulateFailure;

    public SimulatedSurveyMessageGateway(
            @Value("${app.gateway.simulate-failure:false}")
            boolean simulateFailure
    ) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public void send(
            String channel,
            String phone,
            String surveyUrl
    ) {
        if (simulateFailure) {
            log.warn(
                    "Simulated gateway failure; channel={}",
                    channel
            );

            throw new IllegalStateException(
                    "SIMULATED_GATEWAY_FAILURE"
            );
        }

        log.info(
                "Simulated message accepted; channel={}",
                channel
        );
    }
}
