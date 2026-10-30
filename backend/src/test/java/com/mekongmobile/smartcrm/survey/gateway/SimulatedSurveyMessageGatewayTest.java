
package com.mekongmobile.smartcrm.survey.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulatedSurveyMessageGatewayTest {

    @Test
    void shouldAcceptSimulatedMessage() {
        var gateway =
                new SimulatedSurveyMessageGateway(false);

        assertDoesNotThrow(() ->
                gateway.send(
                        "SMS",
                        "0900000001",
                        "http://localhost:5173/khao-sat/test"
                )
        );
    }

    @Test
    void shouldSimulateGatewayFailure() {
        var gateway =
                new SimulatedSurveyMessageGateway(true);

        var exception = assertThrows(
                IllegalStateException.class,
                () -> gateway.send(
                        "SMS",
                        "0900000001",
                        "http://localhost:5173/khao-sat/test"
                )
        );

        assertEquals(
                "SIMULATED_GATEWAY_FAILURE",
                exception.getMessage()
        );
    }
}
