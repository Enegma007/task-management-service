package com.taskmanagement.service;

import com.taskmanagement.dto.request.TaskCreateRequest;
import com.taskmanagement.dto.request.TaskUpdateRequest;
import com.taskmanagement.dto.response.PagedTaskResponse;
import com.taskmanagement.dto.response.TaskResponse;
import com.taskmanagement.entity.Task;
import com.taskmanagement.exception.InvalidTaskRequestException;
import com.taskmanagement.exception.TaskNotFoundException;
import com.taskmanagement.mapper.TaskMapper;
import com.taskmanagement.repository.TaskRepository;
import com.taskmanagement.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task task;
    private TaskResponse taskResponse;
    private TaskCreateRequest createRequest;
    private TaskUpdateRequest updateRequest;
    private static final Instant DUE = Instant.parse("2025-12-31T23:59:59Z");

    @BeforeEach
    void setUp() {
        task = new Task("Test Task", "Description", false, DUE);
        task.setId(1);
        taskResponse = new TaskResponse(1, "Test Task", "Description", false, DUE, null, null, null, null, null, null);
        createRequest = new TaskCreateRequest();
        createRequest.setTitle("New Task");
        createRequest.setDescription("Desc");
        createRequest.setIsCompleted(false);
        createRequest.setDueDate(Instant.now());
        updateRequest = new TaskUpdateRequest();
        updateRequest.setTitle("Updated Title");
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        void whenNoFilter_returnsPagedTasks() {
            Pageable pageable = PageRequest.of(0, 20);
            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(task), pageable, 1));
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            PagedTaskResponse result = taskService.findAll(null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
        }

        @Test
        void whenCompletedFilter_usesSpecification() {
            Pageable pageable = PageRequest.of(0, 20);
            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(task), pageable, 1));
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            PagedTaskResponse result = taskService.findAll(true, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(taskRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void whenTaskExists_returnsTaskResponse() {
            when(taskRepository.findById(1)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            TaskResponse result = taskService.findById(1);

            assertThat(result.getId()).isEqualTo(1);
            assertThat(result.getTitle()).isEqualTo("Test Task");
        }

        @Test
        void whenTaskNotExists_throwsTaskNotFoundException() {
            when(taskRepository.findById(99)).thenReturn(Optional.empty());

            TaskNotFoundException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> taskService.findById(99), TaskNotFoundException.class);
            assertThat(ex).isNotNull();
            assertThat(ex.getTaskId()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void whenValidRequest_createsAndReturnsTask() {
            when(taskMapper.toEntity(createRequest)).thenReturn(task);
            when(taskRepository.save(any(Task.class))).thenReturn(task);
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            TaskResponse result = taskService.create(createRequest);

            assertThat(result.getId()).isEqualTo(1);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        void whenTitleBlank_throwsInvalidTaskRequestException() {
            createRequest.setTitle("   ");

            assertThatThrownBy(() -> taskService.create(createRequest))
                    .isInstanceOf(InvalidTaskRequestException.class)
                    .hasMessageContaining("title");
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        void whenTaskExists_updatesAndReturnsTask() {
            when(taskRepository.findById(1)).thenReturn(Optional.of(task));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskMapper.toResponse(task)).thenReturn(taskResponse);

            TaskResponse result = taskService.update(1, updateRequest);

            verify(taskMapper).updateEntity(eq(task), eq(updateRequest));
            verify(taskRepository).save(task);
            assertThat(result.getId()).isEqualTo(1);
        }

        @Test
        void whenTaskNotExists_throwsTaskNotFoundException() {
            when(taskRepository.findById(99)).thenReturn(Optional.empty());

            TaskNotFoundException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> taskService.update(99, updateRequest), TaskNotFoundException.class);
            assertThat(ex).isNotNull();
            assertThat(ex.getTaskId()).isEqualTo(99);
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        void whenTaskExists_deletesTask() {
            when(taskRepository.existsById(1)).thenReturn(true);

            taskService.deleteById(1);

            verify(taskRepository).deleteById(1);
        }

        @Test
        void whenTaskNotExists_throwsTaskNotFoundException() {
            when(taskRepository.existsById(99)).thenReturn(false);

            TaskNotFoundException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
                    () -> taskService.deleteById(99), TaskNotFoundException.class);
            assertThat(ex).isNotNull();
            assertThat(ex.getTaskId()).isEqualTo(99);
            verify(taskRepository, never()).deleteById(any());
        }
    }
}
