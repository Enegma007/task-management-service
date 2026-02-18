package com.taskmanagement.exception;

import lombok.Getter;

@Getter
public class TaskNotFoundException extends RuntimeException {

    private final Integer taskId;

    public TaskNotFoundException(Integer taskId) {
        super("We couldn't find a task with the given ID. Please check the ID and try again.");
        this.taskId = taskId;
    }
}
