package api;

import exception.ApiException;
import model.Task;
import model.TaskRequest;
import service.TaskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskApi {

    private final TaskService service;

    public TaskApi(TaskService service) {
        if (service == null) {
            throw new IllegalArgumentException(
                    "service must not be null"
            );
        }

        this.service = service;
    }

    public ApiResponse createTask(
            TaskRequest request,
            String idempotencyKey
    ) {
        try {
            // Idempotency behavior will be added in a later milestone.
            Task savedTask = service.createTask(request);

            return new ApiResponse(201, savedTask.toMap());

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse getTask(int id) {
        try {
            Task task = service.getTask(id);

            return new ApiResponse(200, task.toMap());

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listTasks() {
        try {
            List<Map<String, Object>> body = new ArrayList<>();

            for (Task task : service.listTasks()) {
                body.add(task.toMap());
            }

            return new ApiResponse(200, body);

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }
    public ApiResponse updateTask(int id, TaskRequest request) {
        try {
            Task updatedTask = service.updateTask(id, request);

            return new ApiResponse(200, updatedTask.toMap());

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse deleteTask(int id) {
        try {
            service.deleteTask(id);

            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("message", "Task deleted");
            body.put("deletedTaskId", id);

            return new ApiResponse(200, body);

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }
}