/*
package app;

import api.ApiResponse;
import exception.FieldIssue;
import exception.NotFoundException;
import exception.ValidationException;
import model.Task;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.TaskRepository;
import service.TaskService;

public class Main {

    public static void main(String[] args) {
        System.out.println("Task Manager started");

        TaskRepository repository =
                InMemoryTaskRepository.getInstance();

        TaskService service = new TaskService(repository);

        TaskRequest validRequest = new TaskRequest(
                "Pay rent",
                "Pay the monthly rent",
                "false",
                "2026-12-01",
                "HIGH"
        );

        Task savedTask = service.createTask(validRequest);

        System.out.println("Created task:");
        System.out.println(
                new ApiResponse(201, savedTask.toMap())
        );

        Task foundTask = service.getTask(savedTask.getId());

        System.out.println(
                "Found task: " + foundTask.getTitle()
        );

        System.out.println(
                "Task count: " + service.listTasks().size()
        );

        TaskRequest invalidRequest = new TaskRequest(
                "",
                " ",
                "yes",
                "2026-02-30",
                "URGENT"
        );

        try {
            service.createTask(invalidRequest);
            System.out.println("FAIL: invalid request was accepted");

        } catch (ValidationException e) {
            System.out.println(
                    "Validation issues: " + e.getIssues().size()
            );

            for (FieldIssue issue : e.getIssues()) {
                System.out.println(
                        issue.getField() + ": " + issue.getIssue()
                );
            }
        }

        System.out.println(
                "Task count after invalid request: "
                        + service.listTasks().size()
        );

        try {
            service.getTask(999);
            System.out.println("FAIL: unknown ID was accepted");

        } catch (NotFoundException e) {
            System.out.println(
                    "Unknown ID rejected: " + e.getMessage()
            );
        }
    }
}*/
package app;

import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.TaskRepository;
import service.TaskQuery;
import service.TaskService;

public class Main {

    public static void main(String[] args) {
        System.out.println("Task Manager started");

        InMemoryTaskRepository repository =  InMemoryTaskRepository.getInstance();

        TaskService service = new TaskService(repository);
        TaskApi api = new TaskApi(service);

        ConsoleLogListener console = new ConsoleLogListener();
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(console);
        service.addListener(audit);

        System.out.println("\n1. Create with an idempotency key");
        TaskRequest rent = new TaskRequest("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH");
        System.out.println(api.createTask(rent, "rent-001"));

        System.out.println("\n2. Repeat the same call: repository must still hold one task");
        System.out.println(api.createTask(rent, "rent-001"));
        System.out.println(api.listTasks());

        System.out.println("\n3. Validation error");
        System.out.println(api.createTask(new TaskRequest("", "x", "maybe", null, null), null));


        System.out.println("\n4. English to task (offline parser)");
        System.out.println(api.createTaskFromText("Remind me to call the dentist tomorrow", "text-001"));

        System.out.println("\n5. Break a task down (offline strategy)");
        api.createTask(new TaskRequest("Buy milk, eggs and bread", "Shopping", "false", null, null), null);
        System.out.println(api.breakDownTask(3, null));

        System.out.println("\n6. Filter, search, sort and paginate together");
        System.out.println(api.listTasks(TaskQuery.builder()
                .completed("false").search("a")
                .sortBy("dueDate").order("asc")
                .page(1).pageSize(3).build()));

        System.out.println("\n7. Delete the parent: repository before and after");
        System.out.println(api.listTasks());
        System.out.println(api.deleteTask(3));
        System.out.println(api.listTasks());

        System.out.println("\n8. Remove a listener");
        System.out.println("Audit counts before: " + audit.getCounts());
        service.removeListener(audit);
        api.createTask(new TaskRequest("After removal", "Not counted", "false", null, null), null);
        System.out.println("Audit counts after:  " + audit.getCounts());
        /*
        TaskRequest validRequest = new TaskRequest(
                "Pay rent",
                "Pay the monthly rent",
                "false",
                "2026-12-01",
                "HIGH"
        );

        System.out.println("\n1. Create a valid task");
        System.out.println(
                api.createTask(validRequest, null).toJson()
        );

        System.out.println("\n2. Get the saved task");
        System.out.println(
                api.getTask(1).toJson()
        );

        System.out.println("\n3. List all tasks");
        System.out.println(
                api.listTasks().toJson()
        );

        TaskRequest invalidRequest = new TaskRequest(
                "",
                " ",
                "yes",
                "2026-02-30",
                "URGENT"
        );

        System.out.println("\n4. Reject an invalid request");
        System.out.println(
                api.createTask(invalidRequest, null).toJson()
        );

        System.out.println("\n5. Reject an unknown ID");
        System.out.println(
                api.getTask(999).toJson()
        );

        System.out.println("\n6. Reject a null request");
        System.out.println(
                api.createTask(null, null).toJson()
        );

        System.out.println("\n7. Verify only the valid task was saved");
        System.out.println(
                api.listTasks().toJson()
        );

        TaskRequest updateRequest = new TaskRequest(
                "Pay December rent",
                "December rent payment completed",
                "true",
                "2026-12-01",
                "HIGH"
        );

        System.out.println("\n8. Update task 1");
        System.out.println(
                api.updateTask(1, updateRequest).toJson()
        );

        System.out.println("\n9. Verify the updated task");
        System.out.println(
                api.getTask(1).toJson()
        );

        TaskRequest invalidUpdate = new TaskRequest(
                "",
                "This update must not be saved",
                "false",
                null,
                "MEDIUM"
        );

        System.out.println("\n10. Reject an invalid update");
        System.out.println(
                api.updateTask(1, invalidUpdate).toJson()
        );

        System.out.println("\n11. Verify invalid update changed nothing");
        System.out.println(
                api.getTask(1).toJson()
        );

        System.out.println("\n12. Reject update of an unknown ID");
        System.out.println(
                api.updateTask(999, updateRequest).toJson()
        );

        System.out.println("\n13. Confirm update created no extra task");
        System.out.println(
                api.listTasks().toJson()
        );

        System.out.println("\n14. Delete task 1");
        System.out.println(
                api.deleteTask(1).toJson()
        );

        System.out.println("\n15. Confirm deleted task cannot be retrieved");
        System.out.println(
                api.getTask(1).toJson()
        );

        System.out.println("\n16. Reject deleting the same task again");
        System.out.println(
                api.deleteTask(1).toJson()
        );

        System.out.println("\n17. Confirm the task list is empty");
        System.out.println(
                api.listTasks().toJson()
        );*/


    }
}