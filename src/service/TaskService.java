package service;

import event.TaskEventListener;
import exception.BadGatewayException;
import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.ParsedTask;
import model.Priority;
import model.Task;
import model.TaskRequest;
import parser.BreakdownStrategy;
import parser.RuleBasedBreakdownStrategy;
import parser.RuleBasedTaskParser;
import parser.TaskParser;
import repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class TaskService {

    private static final Set<String> RECURRENCES =
            Set.of("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY");
    private static final int MAX_PAGE_SIZE = 100;
    private final TaskRepository repository;
    private final TaskParser parser;
    private final BreakdownStrategy breakdownStrategy;
    private final List<TaskEventListener> listeners = new CopyOnWriteArrayList<>();


    public TaskService(TaskRepository repository) {
        this(repository, new RuleBasedTaskParser(), new RuleBasedBreakdownStrategy());
    }

    public TaskService(TaskRepository repository, TaskParser parser, BreakdownStrategy breakdownStrategy) {
        if (repository == null || parser == null || breakdownStrategy == null) {
            throw new IllegalArgumentException("repository, parser and breakdownStrategy must not be null");
        }
        this.repository = repository;
        this.parser = parser;
        this.breakdownStrategy = breakdownStrategy;
    }
    // ---------- observer ----------

    public void addListener(TaskEventListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(TaskEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String eventType, Task task) {
        for (TaskEventListener listener : listeners) {
            try {
                listener.onEvent(eventType, task);
            } catch (RuntimeException ignored) {
                // one broken listener must not break task operations
            }
        }
    }

    public Task createTask(TaskRequest request) {
        validateRequest(request);
        Task saved = repository.save(toTask(request, null, "NONE"));
        notifyListeners(TaskEventListener.CREATED, saved);
        return saved;
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

    public Task updateTask(int id, TaskRequest request) {
        Task existing = getTask(id);
        validateRequest(request);

        Task updated = existing.toBuilder()
                .title(request.getTitle())
                .description(request.getDescription())
                .completed(Boolean.parseBoolean(request.getCompleted()))
                .dueDate(request.getDueDate())
                .priority(toPriority(request.getPriority()))
                .build();

        Task saved = repository.save(updated);
        notifyListeners(TaskEventListener.UPDATED, saved);
        if (!existing.isCompleted() && saved.isCompleted()) {
            notifyListeners(TaskEventListener.COMPLETED, saved);
        }
        return saved;
    }

    public void deleteTask(int id) {
        Task target = getTask(id);
        List<Task> children = listSubtasks(id);

        for (Task child : children) repository.delete(child.getId());
        repository.delete(id);

        for (Task child : children) notifyListeners(TaskEventListener.DELETED, child);
        notifyListeners(TaskEventListener.DELETED, target);
    }
    // ---------- subtasks ----------

    public Task createSubtask(int parentId, TaskRequest request) {
        Task parent = getTask(parentId);
        if (parent.getParentId() != null) {
            throw new ValidationException("parentId", "a subtask cannot have subtasks");
        }
        validateRequest(request);

        Task saved = repository.save(toTask(request, parentId, "NONE"));
        notifyListeners(TaskEventListener.CREATED, saved);
        return saved;
    }

    public List<Task> listSubtasks(int parentId) {
        getTask(parentId);
        List<Task> subtasks = new ArrayList<>();
        for (Task task : repository.findAll()) {
            if (Integer.valueOf(parentId).equals(task.getParentId())) subtasks.add(task);
        }
        return subtasks;
    }

    // ---------- AI creation ----------

    public Task createTaskFromText(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("text", "must not be empty");
        }

        ParsedTask parsed = parser.parse(text.trim(), DateHelper.today());
        if (parsed == null) throw new BadGatewayException("The parser returned nothing");

        String description = parsed.getDescription();
        if (description == null || description.isBlank()) description = parsed.getTitle();

        String dueDate = parsed.getDueDate();
        if (dueDate != null && dueDate.isBlank()) dueDate = null;

        TaskRequest request = new TaskRequest(parsed.getTitle(), description, "false", dueDate, null);

        try {
            validateRequest(request);
        } catch (ValidationException e) {
            throw new BadGatewayException("Model output failed validation: " + describe(e));
        }

        String recurrence = parsed.getRecurrence();
        if (recurrence == null || recurrence.isBlank()) recurrence = "NONE";
        recurrence = recurrence.trim().toUpperCase();
        if (!RECURRENCES.contains(recurrence)) {
            throw new BadGatewayException("Model output failed validation: recurrence is not recognised");
        }

        Task saved = repository.save(toTask(request, null, recurrence));
        notifyListeners(TaskEventListener.CREATED, saved);
        return saved;
    }
// ---------- AI breakdown ----------

    public List<Task> breakDownTask(int id) {
        Task parent = getTask(id);
        if (parent.getParentId() != null) {
            throw new ValidationException("id", "a subtask cannot be broken down");
        }

        List<TaskRequest> requests = breakdownStrategy.breakDown(parent);
        if (requests == null || requests.isEmpty()) {
            throw new BadGatewayException("Breakdown produced no subtasks");
        }

        for (int i = 0; i < requests.size(); i++) {
            try {
                validateRequest(requests.get(i));
            } catch (ValidationException e) {
                throw new BadGatewayException(
                        "Breakdown item " + (i + 1) + " failed validation: " + describe(e));
            }
        }

        List<Task> created = new ArrayList<>();
        for (TaskRequest request : requests) {
            created.add(repository.save(toTask(request, id, "NONE")));
        }
        for (Task task : created) notifyListeners(TaskEventListener.CREATED, task);
        return created;
    }

    // ---------- queries ----------

    public List<Task> listByPriority(String level) {
        Priority priority = parsePriorityOrNull(level);
        if (priority == null) {
            throw new ValidationException("priority", "must be LOW, MEDIUM, or HIGH");
        }
        List<Task> result = new ArrayList<>();
        TaskFilter filter = new PriorityFilter(priority);
        for (Task task : repository.findAll()) {
            if (filter.matches(task)) result.add(task);
        }
        return result;
    }

    public TaskPage queryTasks(TaskQuery query) {
        TaskQuery q = query == null ? TaskQuery.builder().build() : query;
        validateQuery(q);

        List<TaskFilter> filters = new ArrayList<>();
        if (q.getCompleted() != null) filters.add(new CompletedFilter(Boolean.parseBoolean(q.getCompleted())));
        if (q.getPriority() != null) filters.add(new PriorityFilter(Priority.valueOf(q.getPriority())));
        if (q.getSearch() != null && !q.getSearch().isBlank()) filters.add(new SearchFilter(q.getSearch()));
        filters.addAll(q.getExtraFilters());

        List<Task> matches = new ArrayList<>();
        for (Task task : repository.findAll()) {
            boolean keep = true;
            for (TaskFilter filter : filters) {
                if (!filter.matches(task)) {
                    keep = false;
                    break;
                }
            }
            if (keep) matches.add(task);
        }

        matches.sort(chooseSort(q).comparator());

        int page = q.getPage() == null ? 1 : q.getPage();
        int pageSize = q.getPageSize() == null ? 10 : q.getPageSize();
        int total = matches.size();
        int totalPages = (total + pageSize - 1) / pageSize;

        long from = (long) (page - 1) * pageSize;
        List<Task> items = new ArrayList<>();
        if (from < total) {
            int start = (int) from;
            items = new ArrayList<>(matches.subList(start, Math.min(start + pageSize, total)));
        }
        return new TaskPage(items, page, pageSize, total, totalPages);
    }

    private SortStrategy chooseSort(TaskQuery q) {
        if (q.getSortStrategy() != null) return q.getSortStrategy();
        boolean ascending = q.getOrder() == null || q.getOrder().equalsIgnoreCase("asc");
        if ("dueDate".equals(q.getSortBy())) return new DueDateSort(ascending);
        return new CreatedAtSort(ascending);
    }

    private void validateQuery(TaskQuery q) {
        List<FieldIssue> issues = new ArrayList<>();

        if (q.getCompleted() != null
                && !"true".equals(q.getCompleted()) && !"false".equals(q.getCompleted())) {
            issues.add(new FieldIssue("completed", "must be exactly \"true\" or \"false\""));
        }
        if (q.getPriority() != null && parsePriorityOrNull(q.getPriority()) == null) {
            issues.add(new FieldIssue("priority", "must be LOW, MEDIUM, or HIGH"));
        }
        if (q.getSortBy() != null
                && !"createdAt".equals(q.getSortBy()) && !"dueDate".equals(q.getSortBy())) {
            issues.add(new FieldIssue("sortBy", "must be createdAt or dueDate"));
        }
        if (q.getOrder() != null
                && !q.getOrder().equalsIgnoreCase("asc") && !q.getOrder().equalsIgnoreCase("desc")) {
            issues.add(new FieldIssue("order", "must be asc or desc"));
        }
        if (q.getPage() != null && q.getPage() < 1) {
            issues.add(new FieldIssue("page", "must be 1 or more"));
        }
        if (q.getPageSize() != null && (q.getPageSize() < 1 || q.getPageSize() > MAX_PAGE_SIZE)) {
            issues.add(new FieldIssue("pageSize", "must be between 1 and " + MAX_PAGE_SIZE));
        }
        if (!issues.isEmpty()) throw new ValidationException(issues);
    }

    // ---------- helpers ----------

    private Task toTask(TaskRequest r, Integer parentId, String recurrence) {
        return Task.builder(r.getTitle(), r.getDescription(), Boolean.parseBoolean(r.getCompleted()))
                .dueDate(r.getDueDate())
                .priority(toPriority(r.getPriority()))
                .parentId(parentId)
                .recurrence(recurrence)
                .build();
    }

    private Priority toPriority(String value) {
        return value == null ? Priority.MEDIUM : Priority.valueOf(value);
    }

    private Priority parsePriorityOrNull(String value) {
        if (value == null) return null;
        try {
            return Priority.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String describe(ValidationException e) {
        StringBuilder sb = new StringBuilder();
        for (FieldIssue issue : e.getIssues()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(issue.getField()).append(' ').append(issue.getIssue());
        }
        return sb.toString();
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

}