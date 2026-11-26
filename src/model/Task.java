package model;
import service.DateHelper;

import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public class Task {
    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String dueDate;
    private final Priority priority;
    private final Integer parentId;
    private final long createdAt;
    private final String recurrence ;

    private Task(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.completed = builder.completed;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority;
        this.parentId = builder.parentId;
        this.recurrence=builder.recurrence;
        this.createdAt = builder.createdAt;
    }
    public static Builder builder(
            String title,
            String description,
            boolean completed
    ) {
        return new Builder(title, description, completed);
    }

    public String getRecurrence() { return recurrence; }
   // public Builder recurrence(String recurrence) { this.recurrence = recurrence; return this; }


    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getDueDate() {
        return dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public Integer getParentId() {
        return parentId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Builder toBuilder() {
        return builder(title, description, completed)
                .id(id)
                .dueDate(dueDate)
                .priority(priority)
                .parentId(parentId)
                .recurrence(recurrence)
                .createdAt(createdAt);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", id);
        result.put("title", title);
        result.put("description", description);
        result.put("completed", completed);
        result.put("dueDate", dueDate);
        result.put("priority", priority.name());
        result.put("parentId", parentId);
        result.put("recurrence", recurrence);
        result.put("createdAt", createdAt);

        return result;
    }

    public static final class Builder {

        private int id;
        private String title;
        private String description;
        private boolean completed;

        private String dueDate;
        private Priority priority = Priority.MEDIUM;
        private Integer parentId;
        private String recurrence = "NONE";
        private long createdAt = System.currentTimeMillis();

        private Builder(
                String title,
                String description,
                boolean completed
        ) {
            this.title = title;
            this.description = description;
            this.completed = completed;
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder parentId(Integer parentId) {
            this.parentId = parentId;
            return this;
        }
        public Builder recurrence(String recurrence) { this.recurrence = recurrence; return this; }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Task build() {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException(
                        "title must not be empty"
                );
            }

            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException(
                        "description must not be empty"
                );
            }

            if (dueDate != null && !DateHelper.isValidDate(dueDate)) {
                throw new IllegalArgumentException(
                        "dueDate must be a real YYYY-MM-DD date"
                );
            }

            if (priority == null) {
                throw new IllegalArgumentException(
                        "priority must not be null"
                );
            }
            if (recurrence == null) {
                recurrence = "NONE";
            }
            if (id < 0) {
                throw new IllegalArgumentException(
                        "id must not be negative"
                );
            }

            if (parentId != null && parentId <= 0) {
                throw new IllegalArgumentException(
                        "parentId must be positive"
                );
            }

            return new Task(this);
        }
    }
}
