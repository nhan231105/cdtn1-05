
package com.mekongmobile.smartcrm.survey.exception;

import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class SurveyExceptionHandler {

    @ExceptionHandler(SurveyBusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(
            SurveyBusinessException exception
    ) {
        HttpStatus status = switch (exception.getCode()) {
            case TICKET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TICKET_NOT_CLOSED -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INVITATION_ALREADY_EXISTS -> HttpStatus.CONFLICT;
        };

        return buildError(
                status,
                exception.getCode().name(),
                exception.getMessage(),
                exception.getFields()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, Object> fields = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError -> {
                    String field = fieldError.getField();

                    if ("ticketId".equals(field)) {
                        field = "ticket_id";
                    }

                    fields.putIfAbsent(
                            field,
                            Objects.toString(
                                    fieldError.getDefaultMessage(),
                                    "Giá trị không hợp lệ"
                            )
                    );
                });

        return buildError(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Dữ liệu không hợp lệ",
                fields
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(
            HttpMessageNotReadableException exception
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "JSON không hợp lệ hoặc sai kiểu dữ liệu",
                Map.of()
        );
    }

    private ResponseEntity<Map<String, Object>> buildError(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> fields
    ) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("fields", fields);

        Map<String, Object> body = Map.of("error", error);

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException exception
    ) {
        Throwable rootCause = exception.getMostSpecificCause();

        if (rootCause instanceof SQLException sqlException
                && sqlException.getErrorCode() == 1062
                && sqlException.getMessage() != null
                && sqlException.getMessage()
                .contains("survey_invitation.ticket_id")) {

            return buildError(
                    HttpStatus.CONFLICT,
                    "INVITATION_ALREADY_EXISTS",
                    "Phiếu bảo hành đã có lời mời khảo sát",
                    Map.of()
            );
        }

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "DATABASE_ERROR",
                "Không thể lưu dữ liệu do lỗi ràng buộc cơ sở dữ liệu",
                Map.of()
        );
    }

}
