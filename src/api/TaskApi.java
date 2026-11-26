package api;

import exception.ApiException;
import model.Task;
import model.TaskRequest;
import repository.IdempotencyStore;
import service.TaskPage;
import service.TaskQuery;
import service.TaskService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TaskApi {

    private final TaskService service;
    private final IdempotencyStore idempotencyStore = IdempotencyStore.getInstance();

    public TaskApi(TaskService service) {
        if (service == null) throw new IllegalArgumentException("service must not be null");
        this.service = service;
    }

    public ApiResponse createTask(TaskRequest request, String idempotencyKey) {
        try {
            String[] values = request == null
                    ? new String[0]
                    : new String[]{
                    request.getTitle(),
                    request.getDescription(),
                    request.getCompleted(),
                    request.getDueDate(),
                    request.getPriority()
            };

            return withIdempotency(idempotencyKey, fingerprint("createTask", values),
                    () -> new ApiResponse(201, service.createTask(request).toMap()));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse getTask(int id) {
        try {
            return new ApiResponse(200, service.getTask(id).toMap());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listTasks() {
        try {
            return new ApiResponse(200, toMaps(service.listTasks()));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listTasks(TaskQuery query) {
        try {
            TaskPage page = service.queryTasks(query);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("items", toMaps(page.items()));
            body.put("page", page.page());
            body.put("pageSize", page.pageSize());
            body.put("totalItems", page.totalItems());
            body.put("totalPages", page.totalPages());
            return new ApiResponse(200, body);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listByPriority(String level) {
        try {
            return new ApiResponse(200, toMaps(service.listByPriority(level)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse updateTask(int id, TaskRequest request) {
        try {
            return new ApiResponse(200, service.updateTask(id, request).toMap());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse deleteTask(int id) {
        try {
            service.deleteTask(id);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Task deleted");
            body.put("deletedTaskId", id);
            return new ApiResponse(200, body);
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse createSubtask(int parentId, TaskRequest request) {
        try {
            return new ApiResponse(201, service.createSubtask(parentId, request).toMap());
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse listSubtasks(int parentId) {
        try {
            return new ApiResponse(200, toMaps(service.listSubtasks(parentId)));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse createTaskFromText(String text, String idempotencyKey) {
        try {
            return withIdempotency(idempotencyKey, fingerprint("createTaskFromText", text),
                    () -> new ApiResponse(201, service.createTaskFromText(text).toMap()));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    public ApiResponse breakDownTask(int id, String idempotencyKey) {
        try {
            return withIdempotency(idempotencyKey, fingerprint("breakDownTask", String.valueOf(id)),
                    () -> new ApiResponse(201, toMaps(service.breakDownTask(id))));
        } catch (ApiException e) {
            return ErrorResponseFactory.from(e);
        } catch (Exception e) {
            return ErrorResponseFactory.fromUnexpected(e);
        }
    }

    private ApiResponse withIdempotency(String key, String fingerprint, Supplier<ApiResponse> work) {
        if (key == null || key.isBlank()) return work.get();

        synchronized (idempotencyStore) {
            ApiResponse previous = idempotencyStore.find(key, fingerprint);
            if (previous != null) return previous;

            ApiResponse response = work.get();
            idempotencyStore.save(key, fingerprint, response);
            return response;
        }
    }

    private static String fingerprint(String operation, String... values) {
        StringBuilder sb = new StringBuilder(operation).append(';');
        for (String value : values) {
            if (value == null) sb.append("-1:;");
            else sb.append(value.length()).append(':').append(value).append(';');
        }
        return sb.toString();
    }

    private static List<Map<String, Object>> toMaps(List<Task> tasks) {
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Task task : tasks) maps.add(task.toMap());
        return maps;
    }
}