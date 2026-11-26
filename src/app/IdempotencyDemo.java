package app;

import api.ApiResponse;
import api.TaskApi;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.TaskRepository;
import service.TaskService;

public class IdempotencyDemo {

    public static void main(String[] args) {
        TaskRepository repository =
                InMemoryTaskRepository.getInstance();

        TaskService service = new TaskService(repository);
        TaskApi api = new TaskApi(service);

        TaskRequest request = new TaskRequest(
                "Pay rent",
                "Monthly rent payment",
                "false",
                "2026-12-01",
                "HIGH"
        );

        System.out.println("\n1. Create using a new key");
        ApiResponse first = api.createTask(request, "rent-001");
        System.out.println(first);
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n2. Repeat with an identical new request object");
        TaskRequest identicalRequest = new TaskRequest(
                "Pay rent",
                "Monthly rent payment",
                "false",
                "2026-12-01",
                "HIGH"
        );

        ApiResponse replay = api.createTask(
                identicalRequest,
                "rent-001"
        );

        System.out.println(replay);
        System.out.println(
                "Same response content: "
                        + first.toJson().equals(replay.toJson())
        );
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n3. Reuse the key with different input");
        TaskRequest differentRequest = new TaskRequest(
                "Pay electricity bill",
                "Monthly electricity bill",
                "false",
                "2026-12-01",
                "HIGH"
        );

        System.out.println(
                api.createTask(differentRequest, "rent-001")
        );
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n4. Invalid input must not consume a new key");
        TaskRequest invalidRequest = new TaskRequest(
                "",
                "Buy groceries",
                "false",
                null,
                "MEDIUM"
        );

        System.out.println(
                api.createTask(invalidRequest, "fixable-001")
        );
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n5. Correct input and reuse the failed key");
        TaskRequest correctedRequest = new TaskRequest(
                "Buy groceries",
                "Buy groceries",
                "false",
                null,
                "MEDIUM"
        );

        System.out.println(
                api.createTask(correctedRequest, "fixable-001")
        );
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n6. Null key means ordinary creation");
        System.out.println(api.createTask(request, null));
        System.out.println(api.createTask(request, null));
        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n7. Test separator-safe fingerprints");
        TaskRequest separatorFirst = new TaskRequest(
                "a|b", "c", "false", null, null
        );

        TaskRequest separatorDifferent = new TaskRequest(
                "a", "b|c", "false", null, null
        );

        System.out.println(
                api.createTask(separatorFirst, "separator-001")
        );

        System.out.println(
                api.createTask(separatorDifferent, "separator-001")
        );

        System.out.println("Task count: " + repository.findAll().size());

        System.out.println("\n8. Null request does not consume a new key");
        System.out.println(
                api.createTask(null, "null-request-001")
        );

        System.out.println(
                api.createTask(correctedRequest, "null-request-001")
        );

        System.out.println("Final task count: " + repository.findAll().size());
    }
}