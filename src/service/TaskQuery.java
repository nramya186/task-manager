package service;

import java.util.ArrayList;
import java.util.List;

public final class TaskQuery {

    private final String completed;
    private final String priority;
    private final String search;
    private final String sortBy;
    private final String order;
    private final Integer page;
    private final Integer pageSize;
    private final List<TaskFilter> extraFilters;
    private final SortStrategy sortStrategy;

    private TaskQuery(Builder b) {
        this.completed = b.completed;
        this.priority = b.priority;
        this.search = b.search;
        this.sortBy = b.sortBy;
        this.order = b.order;
        this.page = b.page;
        this.pageSize = b.pageSize;
        this.extraFilters = new ArrayList<>(b.extraFilters);
        this.sortStrategy = b.sortStrategy;
    }

    public static Builder builder() { return new Builder(); }

    public String getCompleted() { return completed; }
    public String getPriority() { return priority; }
    public String getSearch() { return search; }
    public String getSortBy() { return sortBy; }
    public String getOrder() { return order; }
    public Integer getPage() { return page; }
    public Integer getPageSize() { return pageSize; }
    public List<TaskFilter> getExtraFilters() { return new ArrayList<>(extraFilters); }
    public SortStrategy getSortStrategy() { return sortStrategy; }

    public static final class Builder {
        private String completed;
        private String priority;
        private String search;
        private String sortBy;
        private String order;
        private Integer page;
        private Integer pageSize;
        private final List<TaskFilter> extraFilters = new ArrayList<>();
        private SortStrategy sortStrategy;

        public Builder completed(String v) { this.completed = v; return this; }
        public Builder priority(String v) { this.priority = v; return this; }
        public Builder search(String v) { this.search = v; return this; }
        public Builder sortBy(String v) { this.sortBy = v; return this; }
        public Builder order(String v) { this.order = v; return this; }
        public Builder page(Integer v) { this.page = v; return this; }
        public Builder pageSize(Integer v) { this.pageSize = v; return this; }
        public Builder addFilter(TaskFilter filter) { this.extraFilters.add(filter); return this; }
        public Builder sortStrategy(SortStrategy strategy) { this.sortStrategy = strategy; return this; }

        public TaskQuery build() { return new TaskQuery(this); }
    }
}