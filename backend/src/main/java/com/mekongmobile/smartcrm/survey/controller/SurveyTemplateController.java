package com.mekongmobile.smartcrm.survey.controller;

import com.mekongmobile.smartcrm.auth.AuthService;
import com.mekongmobile.smartcrm.survey.dto.SurveyTemplateRequest;
import com.mekongmobile.smartcrm.survey.service.SurveyTemplateService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/survey-templates")
public class SurveyTemplateController {
    private final AuthService auth;
    private final SurveyTemplateService templates;

    public SurveyTemplateController(AuthService auth, SurveyTemplateService templates) {
        this.auth = auth;
        this.templates = templates;
    }

    @GetMapping
    public Map<String, Object> list(HttpServletRequest request) {
        auth.require(request.getHeader("Authorization"), "MARKETING");
        return Map.of("items", templates.list().stream().map(SurveyTemplateServiceView::of).toList());
    }

    @GetMapping("/{id}")
    public SurveyTemplateServiceView get(HttpServletRequest request, @PathVariable long id) {
        auth.require(request.getHeader("Authorization"), "MARKETING");
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("template_id phải là số nguyên dương");
        return SurveyTemplateServiceView.of(templates.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurveyTemplateServiceView create(HttpServletRequest request, @Valid @RequestBody SurveyTemplateRequest body) {
        auth.require(request.getHeader("Authorization"), "MARKETING");
        return SurveyTemplateServiceView.of(templates.create(body));
    }

    @PutMapping("/{id}")
    public SurveyTemplateServiceView update(HttpServletRequest request, @PathVariable long id,
            @Valid @RequestBody SurveyTemplateRequest body) {
        auth.require(request.getHeader("Authorization"), "MARKETING");
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("template_id phải là số nguyên dương");
        return SurveyTemplateServiceView.of(templates.update(id, body));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(HttpServletRequest request, @PathVariable long id) {
        auth.require(request.getHeader("Authorization"), "MARKETING");
        if (id <= 0) throw com.mekongmobile.smartcrm.reporting.ApiException.validation("template_id phải là số nguyên dương");
        templates.delete(id);
    }

    public record SurveyTemplateServiceView(@JsonProperty("template_id") long templateId, String name, String title,
                                            @JsonProperty("intro_text") String introText,
                                            @JsonProperty("is_active") boolean active,
                                            @JsonProperty("created_at") String createdAt,
                                            @JsonProperty("updated_at") String updatedAt) {
        public static SurveyTemplateServiceView of(com.mekongmobile.smartcrm.survey.repository.SurveyTemplateRepository.Template template) {
            return new SurveyTemplateServiceView(template.templateId(), template.name(), template.title(),
                    template.introText(), template.active(), template.createdAt(), template.updatedAt());
        }
    }
}
