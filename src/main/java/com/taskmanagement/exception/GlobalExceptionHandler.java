package com.taskmanagement.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String USER_MESSAGE_NOT_FOUND = "We couldn't find a task with the given ID. Please check the ID and try again.";
    private static final String USER_MESSAGE_VALIDATION = "Some fields in your request are invalid. Please correct them and try again.";
    private static final String USER_MESSAGE_BAD_JSON = "Request body is invalid or contains an invalid value (e.g. date-time). Use ISO-8601 for dates, e.g. 2026-02-18T14:08 or 2026-02-18T14:08:00Z.";
    private static final String USER_MESSAGE_INTERNAL = "Something went wrong on our side. Please try again in a few moments.";

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleTaskNotFound(TaskNotFoundException ex, HttpServletRequest request) {
        log.warn("Task not found: taskId={}, path={}", ex.getTaskId(), request.getRequestURI());
        ApiError error = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                ApiError.CODE_NOT_FOUND,
                "Not Found",
                USER_MESSAGE_NOT_FOUND,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InvalidTaskRequestException.class)
    public ResponseEntity<ApiError> handleInvalidTaskRequest(InvalidTaskRequestException ex, HttpServletRequest request) {
        log.warn("Invalid task request: message={}, path={}", ex.getMessage(), request.getRequestURI());
        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                ApiError.CODE_BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid request body: path={}, message={}", request.getRequestURI(), ex.getMessage());
        ApiError error = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                ApiError.CODE_BAD_REQUEST,
                "Bad Request",
                USER_MESSAGE_BAD_JSON,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldErrorDetail(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        fe.getRejectedValue()
                ))
                .collect(Collectors.toList());
        log.warn("Validation failed: fields={}, path={}", fieldErrors.stream().map(ApiError.FieldErrorDetail::getField).toList(), request.getRequestURI());
        ApiError error = ApiError.validationError(
                HttpStatus.BAD_REQUEST.value(),
                USER_MESSAGE_VALIDATION,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error: path={}", request.getRequestURI(), ex);
        ApiError error = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ApiError.CODE_INTERNAL,
                "Internal Server Error",
                USER_MESSAGE_INTERNAL,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
