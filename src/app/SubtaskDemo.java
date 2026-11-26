package app;

import api.TaskApi;
import model.TaskRequest;
import repository.InMemoryTaskRepository;
import repository.TaskRepository;
import service.TaskService;

public class SubtaskDemo {

    public static void main(String[] args) {
        TaskRepository repository =
                InMemoryTaskRepository.getInstance();

        TaskService service = new TaskService(repository);
        TaskApi api = new TaskApi(service);

        TaskRequest parentRequest = new TaskRequest(
                "Prepare Java project",
                "Finish and submit the task manager",
                "false",
                null,
                "HIGH"
        );

        System.out.println("\n1. Create parent task");
        System.out.println(
                api.createTask(parentRequest, null).toJson()
        );

        System.out.println("\n2. List subtasks before creation");
        System.out.println(
                api.listSubtasks(1).toJson()
        );

        TaskRequest firstSubtask = new TaskRequest(
                "Write tests",
                "Check task creation and validation",
                "false",
                null,
                "HIGH"
        );

        System.out.println("\n3. Create first subtask");
        System.out.println(
                api.createSubtask(1, firstSubtask).toJson()
        );

        TaskRequest secondSubtask = new TaskRequest(
                "Write README",
                "Document setup and API operations",
                "false",
                null,
                "MEDIUM"
        );

        System.out.println("\n4. Create second subtask");
        System.out.println(
                api.createSubtask(1, secondSubtask).toJson()
        );

        System.out.println("\n5. List parent's subtasks");
        System.out.println(
                api.listSubtasks(1).toJson()
        );

        System.out.println("\n6. Reject subtask of a subtask");
        System.out.println(
                api.createSubtask(2, secondSubtask).toJson()
        );

        System.out.println("\n7. Reject unknown parent");
        System.out.println(
                api.createSubtask(999, secondSubtask).toJson()
        );

        TaskRequest invalidSubtask = new TaskRequest(
                "",
                "A title is required",
                "false",
                null,
                "MEDIUM"
        );

        System.out.println("\n8. Reject invalid subtask input");
        System.out.println(
                api.createSubtask(1, invalidSubtask).toJson()
        );

        System.out.println("\n9. Reject listing an unknown parent");
        System.out.println(
                api.listSubtasks(999).toJson()
        );

        System.out.println("\n10. Repository before parent deletion");
        System.out.println(
                api.listTasks().toJson()
        );

        System.out.println("\n11. Delete parent and its subtasks");
        System.out.println(
                api.deleteTask(1).toJson()
        );

        System.out.println("\n12. Repository after parent deletion");
        System.out.println(
                api.listTasks().toJson()
        );

        System.out.println("\n13. Confirm first subtask was deleted");
        System.out.println(
                api.getTask(2).toJson()
        );

        System.out.println("\n14. Confirm second subtask was deleted");
        System.out.println(
                api.getTask(3).toJson()
        );
    }
}