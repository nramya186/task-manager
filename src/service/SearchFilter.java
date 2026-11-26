package service;

import model.Task;

public class SearchFilter implements TaskFilter {

    private final String text;

    public SearchFilter(String text) { this.text = text.trim().toLowerCase(); }

    @Override
    public boolean matches(Task task) {
        return task.getTitle().toLowerCase().contains(text)
                || task.getDescription().toLowerCase().contains(text);
    }
}