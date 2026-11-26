package app;

import api.TaskApi;
import model.TaskRequest;
import parser.LlmClient;
import parser.ParserFactory;
import repository.InMemoryTaskRepository;
import service.TaskService;

public class RealAiDemo {

    public static void main(String[] args) {
        LlmClient client = ParserFactory.realClient();

        TaskService service = new TaskService(
                InMemoryTaskRepository.getInstance(),
                ParserFactory.createTaskParser(false, client),
                ParserFactory.createBreakdownStrategy(false, client));
        TaskApi api = new TaskApi(service);

        System.out.println("1. English -> task");
        System.out.println(api.createTaskFromText("Remind me to pay rent on the 1st of every month", "real-1"));

        System.out.println("\n2. Same key again (must not call the model again)");
        System.out.println(api.createTaskFromText("Remind me to pay rent on the 1st of every month", "real-1"));

        api.createTask(new TaskRequest("Plan a birthday party", "Party for Sunday", "false", null, "HIGH"), null);

        System.out.println("\n3. Break task 2 into subtasks");
        System.out.println(api.breakDownTask(2, "real-bd-1"));

        System.out.println("\n4. All tasks");
        System.out.println(api.listTasks());
    }
}