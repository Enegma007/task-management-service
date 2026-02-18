package com.taskmanagement.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class TaskUpdateRequest {

    @Size(max = 100, message = "Task title must not exceed 100 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Boolean isCompleted;

    private Instant dueDate;

    @Size(max = 100, message = "Assigned-to identifier must not exceed 100 characters")
    private String assignedTo;
}
