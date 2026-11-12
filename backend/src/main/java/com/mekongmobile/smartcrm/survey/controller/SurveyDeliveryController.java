package com.mekongmobile.smartcrm.survey.controller;

import com.mekongmobile.smartcrm.survey.dto.SendSurveyInvitationRequest;
import com.mekongmobile.smartcrm.survey.dto.SendSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.service.SurveyDeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/survey-invitations")
public class SurveyDeliveryController {
    private final SurveyDeliveryService service;
    public SurveyDeliveryController(SurveyDeliveryService service) { this.service = service; }

    @PostMapping("/{id}/send")
    public SendSurveyInvitationResponse send(@PathVariable Long id,
            @Valid @RequestBody(required = false) SendSurveyInvitationRequest request) {
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("invitation_id phải là số nguyên dương");
        return service.send(id, request == null ? "SMS" : request.effectiveChannel());
    }
}
