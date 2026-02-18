package com.taskmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class TaskCreateRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 100, message = "Task title must not exceed 100 characters")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    private Boolean isCompleted = false;

    private Instant dueDate;

    @Size(max = 100, message = "Assigned-to identifier must not exceed 100 characters")
    private String assignedTo;

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted != null ? isCompleted : false;
    }
}
