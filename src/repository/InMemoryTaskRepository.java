package repository;

import model.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores tasks in application memory.
 *
 * Singleton: the application shares one repository instance.
 * Storage only: business validation belongs in TaskService.
 */
public final class InMemoryTaskRepository implements TaskRepository {

    // Create one shared instance when this class is initialized.
    private static final InMemoryTaskRepository INSTANCE =
            new InMemoryTaskRepository();

    private final Map<Integer, Task> tasks = new LinkedHashMap<>();

    private int nextId = 1;

    private InMemoryTaskRepository() {
    }

    public static InMemoryTaskRepository getInstance() {
        return INSTANCE;
    }


    @Override
    public synchronized Task save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }

        if (task.getId() == 0) {
            // Task is immutable, so create a copy with the assigned ID.
            Task savedTask = task.toBuilder()
                    .id(nextId)
                    .build();

            tasks.put(savedTask.getId(), savedTask);
            nextId++;

            return savedTask;
        }

        // Reject accidental updates to tasks that were never saved here.
        if (!tasks.containsKey(task.getId())) {
            throw new IllegalArgumentException(
                    "Cannot update an unknown task ID: " + task.getId()
            );
        }

        // Replacing a task preserves its existing ID.
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Returns the task when present, otherwise Optional.empty().
     */
    @Override
    public synchronized Optional<Task> findById(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public synchronized List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public synchronized boolean delete(int id) {
        return tasks.remove(id) != null;
    }
}