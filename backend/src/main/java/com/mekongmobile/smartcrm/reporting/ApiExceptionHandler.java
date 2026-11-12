package com.mekongmobile.smartcrm.reporting;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> handle(ApiException exception) {
        int status = switch (exception.getCode()) {
            case "VALIDATION_FAILED" -> 400;
            case "UNAUTHENTICATED" -> 401;
            case "FORBIDDEN_SCOPE" -> 403;
            case "TICKET_NOT_FOUND", "INVITATION_NOT_FOUND", "SURVEY_NOT_FOUND", "CENTER_NOT_FOUND", "TEMPLATE_NOT_FOUND" -> 404;
            case "ALREADY_RESPONDED", "INVITATION_CLOSED", "INVITATION_ALREADY_EXISTS", "INVITATION_CANCELLED",
                    "ACTIVE_TEMPLATE_REQUIRED", "ACTIVE_TEMPLATE_DELETE", "TEMPLATE_NAME_EXISTS" -> 409;
            case "SURVEY_EXPIRED" -> 410;
            case "INVALID_PHONE", "EXPORT_TOO_LARGE" -> 422;
            case "RATE_LIMITED" -> 429;
            case "SMS_GATEWAY_ERROR" -> 502;
            case "SERVICE_UNAVAILABLE" -> 503;
            default -> 500;
        };
        return ResponseEntity.status(status).body(Map.of("error", Map.of(
                "code", exception.getCode(), "message", exception.getMessage(), "fields", exception.getFields())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidParameter(MethodArgumentTypeMismatchException exception) {
        return handle(ApiException.validation("Tham số không hợp lệ: " + exception.getName()));
    }
}
