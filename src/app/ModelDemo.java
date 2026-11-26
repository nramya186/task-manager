package app;

import api.ApiResponse;
import model.Priority;
import model.Task;
import model.TaskRequest;

public class ModelDemo {

    public static void main(String[] args) {
        TaskRequest request = new TaskRequest(
                "Pay rent",
                "Pay the monthly rent",
                "false",
                "2026-12-01",
                "HIGH"
        );

        System.out.println(
                "Raw completed input: " + request.getCompleted()
        );

        Task task = Task.builder(
                        "Pay rent",
                        "Pay the monthly rent",
                        false
                )
                .dueDate("2026-12-01")
                .priority(Priority.HIGH)
                .build();

        System.out.println("Original task:");
        System.out.println(new ApiResponse(200, task.toMap()));

        Task completedTask = task.toBuilder()
                .completed(true)
                .build();

        System.out.println(
                "Original completed: " + task.isCompleted()
        );

        System.out.println(
                "New copy completed: " + completedTask.isCompleted()
        );

        Task defaultTask = Task.builder(
                "Read Java notes",
                "Revise constructors and classes",
                false
        ).build();

        System.out.println(
                "Default priority: " + defaultTask.getPriority()
        );

        try {
            Task.builder(
                    " ",
                    "This task has a blank title",
                    false
            ).build();

            System.out.println("FAIL: blank title was accepted");

        } catch (IllegalArgumentException e) {
            System.out.println("PASS: blank title was rejected");
        }

        try {
            Task.builder(
                            "Check date",
                            "This task has an impossible date",
                            false
                    )
                    .dueDate("2026-02-30")
                    .build();

            System.out.println("FAIL: impossible date was accepted");

        } catch (IllegalArgumentException e) {
            System.out.println("PASS: impossible date was rejected");
        }
    }
}