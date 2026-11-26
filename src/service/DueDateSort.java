package service;

import model.Task;

import java.util.Comparator;

public class DueDateSort implements SortStrategy {

    private final boolean ascending;

    public DueDateSort(boolean ascending) { this.ascending = ascending; }

    @Override
    public Comparator<Task> comparator() {
        Comparator<String> dates = ascending
                ? Comparator.nullsLast(Comparator.<String>naturalOrder())
                : Comparator.nullsLast(Comparator.<String>reverseOrder());
        return Comparator.comparing(Task::getDueDate, dates).thenComparingInt(Task::getId);
    }
}