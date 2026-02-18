package com.taskmanagement.service.impl;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.PagedTaskResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.exception.InvalidTaskRequestException;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.repository.TaskSpecification;
import com.taskmanagement.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedTaskResponse findAll(Boolean completed, String assignedTo, Pageable pageable) {
        Page<Task> page = taskRepository.findAll(
                TaskSpecification.withFilters(completed, assignedTo),
                pageable);
        List<TaskResponse> content = page.getContent().stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
        log.debug("findAll: completed={}, assignedTo={}, page={}, total={}", completed, assignedTo, page.getNumber(), page.getTotalElements());
        return new PagedTaskResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(Integer id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        log.debug("findById: id={}", id);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        validateTitle(request.getTitle());
        Task task = taskMapper.toEntity(request);
        task = taskRepository.save(task);
        log.info("Task created: id={}, title={}", task.getId(), task.getTitle());
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse update(Integer id, TaskUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        if (request.getTitle() != null) {
            validateTitle(request.getTitle());
        }
        taskMapper.updateEntity(task, request);
        task = taskRepository.save(task);
        log.info("Task updated: id={}", id);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
        log.info("Task deleted: id={}", id);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidTaskRequestException("Task title is required and cannot be blank. Please provide a title.");
        }
        if (title.length() > 100) {
            throw new InvalidTaskRequestException("Task title must not exceed 100 characters. Please shorten the title.");
        }
    }
}
