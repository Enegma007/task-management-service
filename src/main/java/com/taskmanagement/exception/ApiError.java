package com.taskmanagement.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
public class ApiError {

    public static final String CODE_NOT_FOUND = "TASK_NOT_FOUND";
    public static final String CODE_BAD_REQUEST = "INVALID_REQUEST";
    public static final String CODE_VALIDATION = "VALIDATION_FAILED";
    public static final String CODE_INTERNAL = "INTERNAL_ERROR";

    private Instant timestamp = Instant.now();
    private int status;
    private String errorCode;
    private String error;
    private String message;
    private String path;
    private List<FieldErrorDetail> fieldErrors;

    public static ApiError of(int status, String errorCode, String error, String message, String path) {
        ApiError apiError = new ApiError();
        apiError.setStatus(status);
        apiError.setErrorCode(errorCode);
        apiError.setError(error);
        apiError.setMessage(message);
        apiError.setPath(path);
        return apiError;
    }

    public static ApiError validationError(int status, String message, String path, List<FieldErrorDetail> fieldErrors) {
        ApiError apiError = of(status, CODE_VALIDATION, "Validation Failed", message, path);
        apiError.setFieldErrors(fieldErrors);
        return apiError;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDetail {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}
