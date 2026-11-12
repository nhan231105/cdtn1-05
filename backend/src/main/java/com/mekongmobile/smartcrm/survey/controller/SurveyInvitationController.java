
package com.mekongmobile.smartcrm.survey.controller;

import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationRequest;
import com.mekongmobile.smartcrm.survey.dto.CreateSurveyInvitationResponse;
import com.mekongmobile.smartcrm.survey.dto.UpdateSurveyInvitationRequest;
import com.mekongmobile.smartcrm.survey.service.SurveyInvitationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.Map;
import java.util.List;

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

    @GetMapping
    public List<Map<String, Object>> listInvitations() {
        return invitationService.listInvitations();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getInvitation(@PathVariable long id) {
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("invitation_id phải là số nguyên dương");
        return invitationService.getInvitation(id);
    }

    @PatchMapping("/{id}")
    public Map<String, Object> updateInvitation(@PathVariable long id,
            @Valid @RequestBody UpdateSurveyInvitationRequest request) {
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("invitation_id phải là số nguyên dương");
        return invitationService.updateChannel(id, request.channel());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvitation(@PathVariable long id) {
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("invitation_id phải là số nguyên dương");
        invitationService.deleteUnsent(id);
    }
}
