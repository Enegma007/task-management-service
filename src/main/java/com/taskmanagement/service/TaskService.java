package com.taskmanagement.service;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.PagedTaskResponse;
import com.taskmanagement.dto.response.TaskResponse;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    PagedTaskResponse findAll(Boolean completed, String assignedTo, Pageable pageable);

    TaskResponse findById(Integer id);

    TaskResponse create(TaskCreateRequest request);

    TaskResponse update(Integer id, TaskUpdateRequest request);

    void deleteById(Integer id);
}
