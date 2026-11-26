package service;

import model.Task;

public class CompletedFilter implements TaskFilter {

    private final boolean completed;

    public CompletedFilter(boolean completed) { this.completed = completed; }

    @Override
    public boolean matches(Task task) { return task.isCompleted() == completed; }
}