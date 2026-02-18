package com.taskmanagement.exception;

public class InvalidTaskRequestException extends RuntimeException {

    public InvalidTaskRequestException(String message) {
        super(message);
    }

    public InvalidTaskRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
