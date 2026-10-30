
package com.mekongmobile.smartcrm.survey.controller;

import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationRequest;
import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.service.SurveyInvitationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/survey-invitations")
public class SurveyInvitationController {

    private final SurveyInvitationService invitationService;

    public SurveyInvitationController(
            SurveyInvitationService invitationService
    ) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<CreateSurveyInvitationResponse> createInvitation(
            @Valid @RequestBody CreateSurveyInvitationRequest request
    ) {
        CreateSurveyInvitationResponse response =
                invitationService.createInvitation(
                        request.ticketId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
