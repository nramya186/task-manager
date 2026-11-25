package service;

import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.Priority;
import model.Task;
import model.TaskRequest;
import repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException(
                    "repository must not be null"
            );
        }

        this.repository = repository;
    }

    public Task createTask(TaskRequest request) {
        validateRequest(request);

        Priority priority = request.getPriority() == null
                ? Priority.MEDIUM
                : Priority.valueOf(request.getPriority());

        Task task = Task.builder(
                        request.getTitle(),
                        request.getDescription(),
                        Boolean.parseBoolean(request.getCompleted())
                )
                .dueDate(request.getDueDate())
                .priority(priority)
                .build();

        return repository.save(task);
    }

    public Task getTask(int id) {
        return repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Task not found")
                );
    }

    public List<Task> listTasks() {
        return repository.findAll();
    }

    private void validateRequest(TaskRequest request) {
        if (request == null) {
            throw new ValidationException(
                    "request",
                    "must not be null"
            );
        }

        List<FieldIssue> issues = new ArrayList<>();

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {
            issues.add(new FieldIssue(
                    "title",
                    "must not be empty"
            ));
        }

        if (request.getDescription() == null
                || request.getDescription().isBlank()) {
            issues.add(new FieldIssue(
                    "description",
                    "must not be empty"
            ));
        }

        if (!"true".equals(request.getCompleted())
                && !"false".equals(request.getCompleted())) {
            issues.add(new FieldIssue(
                    "completed",
                    "must be exactly \"true\" or \"false\""
            ));
        }

        if (request.getDueDate() != null
                && !DateHelper.isValidDate(request.getDueDate())) {
            issues.add(new FieldIssue(
                    "dueDate",
                    "must be a real YYYY-MM-DD date"
            ));
        }

        if (request.getPriority() != null) {
            try {
                Priority.valueOf(request.getPriority());
            } catch (IllegalArgumentException e) {
                issues.add(new FieldIssue(
                        "priority",
                        "must be LOW, MEDIUM, or HIGH"
                ));
            }
        }

        // Report all field problems together.
        if (!issues.isEmpty()) {
            throw new ValidationException(issues);
        }
    }

    public Task updateTask(int id, TaskRequest request) {
        Task existingTask = getTask(id);

        validateRequest(request);

        Priority priority = request.getPriority() == null
                ? Priority.MEDIUM
                : Priority.valueOf(request.getPriority());

        Task updatedTask = existingTask.toBuilder()
                .title(request.getTitle())
                .description(request.getDescription())
                .completed(Boolean.parseBoolean(request.getCompleted()))
                .dueDate(request.getDueDate())
                .priority(priority)
                .build();

        return repository.save(updatedTask);
    }

    public void deleteTask(int id) {
        getTask(id);

        // findAll() returns a separate list, so deletion can use this snapshot.
        for (Task task : repository.findAll()) {
            if (Integer.valueOf(id).equals(task.getParentId())) {
                repository.delete(task.getId());
            }
        }

        repository.delete(id);
    }
}