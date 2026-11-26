package service;

import model.Task;

import java.util.Comparator;

public class CreatedAtSort implements SortStrategy {

    private final boolean ascending;

    public CreatedAtSort(boolean ascending) { this.ascending = ascending; }

    @Override
    public Comparator<Task> comparator() {
        Comparator<Task> byCreated = Comparator.comparingLong(Task::getCreatedAt)
                .thenComparingInt(Task::getId);
        return ascending ? byCreated : byCreated.reversed();
    }
}