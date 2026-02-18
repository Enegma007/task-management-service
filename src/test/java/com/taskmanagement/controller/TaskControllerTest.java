package com.taskmanagement.controller;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.PagedTaskResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private static final Instant DUE = Instant.parse("2025-12-31T23:59:59Z");
    private final TaskResponse taskResponse = new TaskResponse(
            1, "Test Task", "Description", false, DUE, null, null, null, null, null, null);

    private static PagedTaskResponse paged(TaskResponse... items) {
        return new PagedTaskResponse(List.of(items), 0, 20, items.length, 1, true, true);
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasks {

        @Test
        void returns200AndPagedTasks() throws Exception {
            when(taskService.findAll(any(), any(), any(Pageable.class))).thenReturn(paged(taskResponse));

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Test Task"))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void withCompletedParam_callsServiceWithFilter() throws Exception {
            when(taskService.findAll(eq(true), any(), any(Pageable.class))).thenReturn(paged(taskResponse));

            mockMvc.perform(get("/api/tasks").param("completed", "true"))
                    .andExpect(status().isOk());
            verify(taskService).findAll(eq(true), eq(null), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskById {

        @Test
        void whenExists_returns200AndTask() throws Exception {
            when(taskService.findById(1)).thenReturn(taskResponse);

            mockMvc.perform(get("/api/tasks/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        void whenNotExists_returns404AndErrorBody() throws Exception {
            when(taskService.findById(99)).thenThrow(new TaskNotFoundException(99));

            mockMvc.perform(get("/api/tasks/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"))
                    .andExpect(jsonPath("$.message", containsString("couldn't find")));
        }
    }

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTask {

        @Test
        void whenValidBody_returns201WithLocationAndBody() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest();
            request.setTitle("New Task");
            request.setDescription("Desc");
            request.setIsCompleted(false);
            when(taskService.create(any(TaskCreateRequest.class))).thenReturn(taskResponse);

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/tasks/1")))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        void whenTitleMissing_returns400ValidationError() throws Exception {
            TaskCreateRequest request = new TaskCreateRequest();
            request.setDescription("Only description");

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test
        void whenValidBody_returns200AndUpdatedTask() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setTitle("Updated Title");
            when(taskService.update(eq(1), any(TaskUpdateRequest.class))).thenReturn(taskResponse);

            mockMvc.perform(put("/api/tasks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        void whenTaskNotExists_returns404() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest();
            request.setTitle("Updated");
            when(taskService.update(eq(99), any(TaskUpdateRequest.class)))
                    .thenThrow(new TaskNotFoundException(99));

            mockMvc.perform(put("/api/tasks/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test
        void whenExists_returns204NoContent() throws Exception {
            mockMvc.perform(delete("/api/tasks/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            verify(taskService).deleteById(1);
        }

        @Test
        void whenNotExists_returns404() throws Exception {
            org.mockito.Mockito.doThrow(new TaskNotFoundException(99)).when(taskService).deleteById(99);

            mockMvc.perform(delete("/api/tasks/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
