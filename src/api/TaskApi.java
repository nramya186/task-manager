package api;

import exception.ApiException;
import model.Task;
import model.TaskRequest;
import service.TaskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import repository.IdempotencyStore;
public class TaskApi {

    private final TaskService service;

    private final IdempotencyStore idempotencyStore =
            IdempotencyStore.getInstance();

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
            if (idempotencyKey == null) {
                Task savedTask = service.createTask(request);

                return new ApiResponse(201, savedTask.toMap());
            }

            String fingerprint = fingerprintOfCreateTask(request);

            // Keep check, creation, and storage together under one lock.
            synchronized (idempotencyStore) {
                ApiResponse previousResponse = idempotencyStore.find(
                        idempotencyKey,
                        fingerprint
                );

                if (previousResponse != null) {
                    return previousResponse;
                }

                Task savedTask = service.createTask(request);

                ApiResponse response = new ApiResponse(
                        201,
                        savedTask.toMap()
                );

                idempotencyStore.save(
                        idempotencyKey,
                        fingerprint,
                        response
                );

                return response;
            }

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
    public ApiResponse createSubtask(
            int parentId,
            TaskRequest request
    ) {
        try {
            Task subtask = service.createSubtask(parentId, request);

            return new ApiResponse(201, subtask.toMap());

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listSubtasks(int parentId) {
        try {
            List<Map<String, Object>> body = new ArrayList<>();

            for (Task subtask : service.listSubtasks(parentId)) {
                body.add(subtask.toMap());
            }

            return new ApiResponse(200, body);

        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);

        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    private String fingerprintOfCreateTask(TaskRequest request) {
        StringBuilder result = new StringBuilder("createTask;");

        if (request == null) {
            return result.append("request:null;").toString();
        }

        result.append("request:present;");

        appendFingerprintValue(result, request.getTitle());
        appendFingerprintValue(result, request.getDescription());
        appendFingerprintValue(result, request.getCompleted());
        appendFingerprintValue(result, request.getDueDate());
        appendFingerprintValue(result, request.getPriority());

        return result.toString();
    }

    private void appendFingerprintValue(
            StringBuilder result,
            String value
    ) {
        if (value == null) {
            result.append("-1:;");
            return;
        }

        result.append(value.length())
                .append(':')
                .append(value)
                .append(';');
    }
}