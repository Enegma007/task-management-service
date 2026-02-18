package com.taskmanagement.mapper;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Component
public class TaskMapper {

    private static final Function<TaskCreateRequest, Task> TO_ENTITY = req -> {
        Task task = new Task();
        task.setTitle(Optional.ofNullable(req.getTitle()).map(String::trim).orElse(null));
        task.setDescription(Optional.ofNullable(req.getDescription()).map(String::trim).orElse(null));
        task.setIsCompleted(req.getIsCompleted());
        task.setDueDate(req.getDueDate());
        Optional.ofNullable(req.getAssignedTo()).map(String::trim).filter(s -> !s.isBlank())
                .ifPresent(assignee -> {
                    task.setAssignedTo(assignee);
                    task.setAssignedAt(Instant.now());
                });
        return task;
    };

    private static final BiConsumer<Task, TaskUpdateRequest> UPDATE_ENTITY = (task, req) -> {
        Optional.ofNullable(req.getTitle()).map(String::trim).ifPresent(task::setTitle);
        Optional.ofNullable(req.getDescription()).map(String::trim).ifPresent(task::setDescription);
        Optional.ofNullable(req.getIsCompleted()).ifPresent(task::setIsCompleted);
        Optional.ofNullable(req.getDueDate()).ifPresent(task::setDueDate);
        if (req.getAssignedTo() != null) {
            String trimmed = req.getAssignedTo().trim();
            task.setAssignedTo(trimmed.isBlank() ? null : trimmed);
            task.setAssignedAt(trimmed.isBlank() ? null : Instant.now());
        }
    };

    private static final Function<Task, TaskResponse> TO_RESPONSE = task ->
            new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getIsCompleted(),
                    task.getDueDate(),
                    task.getCreatedAt(),
                    task.getUpdatedAt(),
                    task.getCreatedBy(),
                    task.getUpdatedBy(),
                    task.getAssignedTo(),
                    task.getAssignedAt()
            );

    public Task toEntity(TaskCreateRequest request) {
        return TO_ENTITY.apply(request);
    }

    public void updateEntity(Task task, TaskUpdateRequest request) {
        UPDATE_ENTITY.accept(task, request);
    }

    public TaskResponse toResponse(Task task) {
        return TO_RESPONSE.apply(task);
    }
}
