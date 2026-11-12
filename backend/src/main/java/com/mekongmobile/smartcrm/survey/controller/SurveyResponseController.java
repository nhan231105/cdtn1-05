package com.mekongmobile.smartcrm.survey.controller;

import com.mekongmobile.smartcrm.survey.service.SurveyResponseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/surveys")
public class SurveyResponseController {
    private final SurveyResponseService service;
    public SurveyResponseController(SurveyResponseService service) { this.service = service; }
    public record Submission(Object score, Object comment) {}

    @GetMapping("/{token}")
    public Map<String, Object> form(@PathVariable String token) { return service.form(token); }

    @PostMapping("/{token}/responses")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submit(@PathVariable String token, @RequestBody Submission body) {
        return service.submit(token, body.score(), body.comment());
    }
}
