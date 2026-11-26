package service;

import model.Task;

public interface TaskFilter {
    boolean matches(Task task);
}