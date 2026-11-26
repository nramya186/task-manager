package app;

import api.ApiResponse;
import api.TaskApi;
import event.AuditTrailListener;
import event.ConsoleLogListener;
import event.TaskEventListener;
import model.TaskRequest;
import parser.BreakdownStrategy;
import parser.FakeLlmClient;
import parser.FaultyLlmClient;
import parser.LlmBreakdownStrategy;
import parser.LlmTaskParser;
import parser.ParserFactory;
import parser.RuleBasedBreakdownStrategy;
import parser.RuleBasedTaskParser;
import parser.TaskParser;
import repository.IdempotencyStore;
import repository.InMemoryTaskRepository;
import service.TaskQuery;
import service.TaskService;

import java.time.LocalDate;

public class FullTestRunner {

    private static final InMemoryTaskRepository REPO = InMemoryTaskRepository.getInstance();
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        crud();
        validation();
        idempotentCreate();
        subtasks();
        aiCreationWithFake();
        aiCreationWithFaultyModel();
        offlineParser();
        breakdown();
        queries();
        events();

        System.out.println();
        System.out.println("PASSED: " + passed + "   FAILED: " + failed);
        if (failed > 0) System.exit(1);
    }

    static void crud() {
        System.out.println("\n== CRUD ==");
        TaskApi api = freshOffline();

        ApiResponse created = api.createTask(request("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH"), null);
        check("create returns 201", created.getStatus() == 201);
        check("get returns 200", api.getTask(1).getStatus() == 200);
        check("get unknown id returns 404", api.getTask(999).getStatus() == 404);
        check("404 uses error shape", api.getTask(999).toJson().contains("\"code\":\"NOT_FOUND\""));

        ApiResponse updated = api.updateTask(1, request("Pay rent now", "Monthly rent", "true", "2026-12-01", "LOW"));
        check("update returns 200", updated.getStatus() == 200);
        check("update changes title", updated.toJson().contains("Pay rent now"));
        check("update keeps id", updated.toJson().contains("\"id\":1"));
        check("update unknown id returns 404", api.updateTask(999, request("a", "b", "false", null, null)).getStatus() == 404);
        check("invalid update returns 400", api.updateTask(1, request("", "b", "false", null, null)).getStatus() == 400);
        check("invalid update changes nothing", api.getTask(1).toJson().contains("Pay rent now"));

        check("delete returns 200", api.deleteTask(1).getStatus() == 200);
        check("deleted task is gone", api.getTask(1).getStatus() == 404);
        check("delete again returns 404", api.deleteTask(1).getStatus() == 404);
        check("repository empty", REPO.findAll().isEmpty());
    }

    static void validation() {
        System.out.println("\n== Validation ==");
        TaskApi api = freshOffline();

        ApiResponse bad = api.createTask(request("", " ", "yes", "2026-02-30", "URGENT"), null);
        check("five problems reported at once", bad.getStatus() == 400 && count(bad.toJson(), "\"field\"") == 5);
        check("blank title rejected", api.createTask(request("  ", "d", "false", null, null), null).getStatus() == 400);
        check("completed must be true/false", api.createTask(request("t", "d", "TRUE", null, null), null).getStatus() == 400);
        check("2026-02-30 rejected", api.createTask(request("t", "d", "false", "2026-02-30", null), null).getStatus() == 400);
        check("2027-02-29 rejected (not leap)", api.createTask(request("t", "d", "false", "2027-02-29", null), null).getStatus() == 400);
        check("2028-02-29 accepted (leap)", api.createTask(request("t", "d", "false", "2028-02-29", null), null).getStatus() == 201);
        check("bad date format rejected", api.createTask(request("t", "d", "false", "2026-1-5", null), null).getStatus() == 400);
        check("lowercase priority rejected", api.createTask(request("t", "d", "false", null, "high"), null).getStatus() == 400);
        check("null request returns 400", api.createTask(null, null).getStatus() == 400);
        check("priority defaults to MEDIUM", api.createTask(request("t2", "d", "false", null, null), null).toJson().contains("\"priority\":\"MEDIUM\""));
        check("only the 2 valid tasks were saved", REPO.findAll().size() == 2);
    }

    static void idempotentCreate() {
        System.out.println("\n== Idempotency (createTask) ==");
        TaskApi api = freshOffline();
        TaskRequest r = request("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH");

        ApiResponse first = api.createTask(r, "k1");
        ApiResponse replay = api.createTask(request("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH"), "k1");
        check("first call returns 201", first.getStatus() == 201);
        check("replay returns the stored response", replay.toJson().equals(first.toJson()));
        check("replay created nothing", REPO.findAll().size() == 1);

        ApiResponse conflict = api.createTask(request("Other", "Other", "false", null, null), "k1");
        check("same key, different request returns 422", conflict.getStatus() == 422);
        check("422 code is CONFLICT", conflict.toJson().contains("\"code\":\"CONFLICT\""));
        check("conflict created nothing", REPO.findAll().size() == 1);

        check("invalid input returns 400", api.createTask(request("", "d", "false", null, null), "k2").getStatus() == 400);
        check("failed key can be reused after fixing input", api.createTask(request("Fixed", "d", "false", null, null), "k2").getStatus() == 201);

        api.createTask(request("a|b", "c", "false", null, null), "k3");
        check("separator-safe fingerprint", api.createTask(request("a", "b|c", "false", null, null), "k3").getStatus() == 422);

        int before = REPO.findAll().size();
        api.createTask(r, null);
        api.createTask(r, null);
        check("null key means no idempotency", REPO.findAll().size() == before + 2);
    }

    static void subtasks() {
        System.out.println("\n== Subtasks ==");
        TaskApi api = freshOffline();
        api.createTask(request("Parent", "Parent task", "false", null, "HIGH"), null);

        ApiResponse sub = api.createSubtask(1, request("Child 1", "First child", "false", null, null));
        check("create subtask returns 201", sub.getStatus() == 201 && sub.toJson().contains("\"parentId\":1"));
        api.createSubtask(1, request("Child 2", "Second child", "false", null, null));
        check("list subtasks returns both", count(api.listSubtasks(1).toJson(), "\"parentId\":1") == 2);
        check("subtask of a subtask returns 400", api.createSubtask(2, request("x", "y", "false", null, null)).getStatus() == 400);
        check("unknown parent returns 404", api.createSubtask(999, request("x", "y", "false", null, null)).getStatus() == 404);
        check("invalid subtask returns 400", api.createSubtask(1, request("", "y", "false", null, null)).getStatus() == 400);
        check("list unknown parent returns 404", api.listSubtasks(999).getStatus() == 404);

        System.out.println("Before delete: " + REPO.findAll().size() + " tasks");
        api.deleteTask(1);
        System.out.println("After delete:  " + REPO.findAll().size() + " tasks");
        check("deleting parent deletes subtasks", REPO.findAll().isEmpty());
    }

    static void aiCreationWithFake() {
        System.out.println("\n== AI creation (fake client) ==");
        FakeLlmClient fake = new FakeLlmClient();
        TaskApi api = apiWith(new LlmTaskParser(fake), new RuleBasedBreakdownStrategy());

        ApiResponse first = api.createTaskFromText("Remind me to pay rent on the 1st of every month", "ai-1");
        check("fake reply creates a task (201)", first.getStatus() == 201);
        check("title comes from the model", first.toJson().contains("\"title\":\"Pay rent\""));
        check("recurrence is kept", first.toJson().contains("\"recurrence\":\"MONTHLY\""));
        check("client called once", fake.getCalls() == 1);

        ApiResponse replay = api.createTaskFromText("Remind me to pay rent on the 1st of every month", "ai-1");
        check("replay returns the same response", replay.toJson().equals(first.toJson()));
        check("replay did NOT call the client again", fake.getCalls() == 1);
        check("replay created no duplicate", REPO.findAll().size() == 1);

        ApiResponse conflict = api.createTaskFromText("Something different", "ai-1");
        check("same key, different text returns 422", conflict.getStatus() == 422);
        check("conflict did not call the client", fake.getCalls() == 1);

        check("blank text returns 400", api.createTaskFromText("   ", null).getStatus() == 400);
        check("blank text did not call the client", fake.getCalls() == 1);

        String fenced = "```json\n{\"title\":\"Call mom\",\"description\":\"Weekly call\",\"dueDate\":null,\"recurrence\":\"WEEKLY\"}\n```";
        TaskApi fencedApi = apiWith(new LlmTaskParser(new FakeLlmClient(fenced)), new RuleBasedBreakdownStrategy());
        check("code fences are tolerated", fencedApi.createTaskFromText("call mom weekly", null).getStatus() == 201);
    }

    static void aiCreationWithFaultyModel() {
        System.out.println("\n== AI creation (faulty model) ==");
        String[][] cases = {
                {"empty title", "{\"title\":\"\",\"description\":\"d\",\"dueDate\":null,\"recurrence\":\"NONE\"}"},
                {"missing title", "{\"description\":\"d\",\"dueDate\":null}"},
                {"impossible date", "{\"title\":\"t\",\"description\":\"d\",\"dueDate\":\"2026-02-30\",\"recurrence\":\"NONE\"}"},
                {"date as a number", "{\"title\":\"t\",\"description\":\"d\",\"dueDate\":20261201,\"recurrence\":\"NONE\"}"},
                {"bad recurrence", "{\"title\":\"t\",\"description\":\"d\",\"dueDate\":null,\"recurrence\":\"SOMETIMES\"}"},
                {"plain words", "Sure! Here is your task: pay rent"},
                {"truncated JSON", "{\"title\":\"t\","},
        };
        for (String[] c : cases) {
            TaskApi api = apiWith(new LlmTaskParser(new FaultyLlmClient(c[1])), new RuleBasedBreakdownStrategy());
            ApiResponse r = api.createTaskFromText("anything", null);
            check(c[0] + " -> 502", r.getStatus() == 502);
            check(c[0] + " -> nothing saved", REPO.findAll().isEmpty());
        }

        TaskApi throwing = apiWith(new LlmTaskParser(new FaultyLlmClient()), new RuleBasedBreakdownStrategy());
        ApiResponse r = throwing.createTaskFromText("anything", null);
        check("network failure -> 502", r.getStatus() == 502 && r.toJson().contains("\"code\":\"BAD_GATEWAY\""));

        TaskApi noKey = apiWith(new LlmTaskParser(ParserFactory.missingKeyClient()), new RuleBasedBreakdownStrategy());
        ApiResponse missing = noKey.createTaskFromText("anything", null);
        check("missing key -> 503", missing.getStatus() == 503);
        check("503 code is SERVICE_UNAVAILABLE", missing.toJson().contains("\"code\":\"SERVICE_UNAVAILABLE\""));
        check("503 saved nothing", REPO.findAll().isEmpty());

        FaultyLlmClient faulty = new FaultyLlmClient("{\"title\":\"\"}");
        TaskApi keyed = apiWith(new LlmTaskParser(faulty), new RuleBasedBreakdownStrategy());
        keyed.createTaskFromText("anything", "bad-1");
        keyed.createTaskFromText("anything", "bad-1");
        check("failures are not remembered (client called again)", faulty.getCalls() == 2);
    }

    static void offlineParser() {
        System.out.println("\n== Offline parser ==");
        TaskApi api = freshOffline();

        ApiResponse rent = api.createTaskFromText("Remind me to pay rent on the 1st of every month", null);
        String expectedFirst = LocalDate.now().withDayOfMonth(1).plusMonths(1).toString();
        check("offline creates a task", rent.getStatus() == 201);
        check("title is 'Pay rent'", rent.toJson().contains("\"title\":\"Pay rent\""));
        check("recurrence MONTHLY", rent.toJson().contains("\"recurrence\":\"MONTHLY\""));
        check("due date is the next 1st", rent.toJson().contains("\"dueDate\":\"" + expectedFirst + "\""));

        ApiResponse tomorrow = api.createTaskFromText("Call the dentist tomorrow", null);
        String expectedTomorrow = LocalDate.now().plusDays(1).toString();
        check("'tomorrow' resolves", tomorrow.toJson().contains("\"dueDate\":\"" + expectedTomorrow + "\""));
        check("title cut before 'tomorrow'", tomorrow.toJson().contains("\"title\":\"Call the dentist\""));
    }

    static void breakdown() {
        System.out.println("\n== Breakdown ==");
        String good = "Research|Collect the facts\nWrite|Write the first draft\nReview|Check the draft";
        FakeLlmClient fake = new FakeLlmClient(good);
        TaskApi api = apiWith(new LlmTaskParser(new FakeLlmClient()), new LlmBreakdownStrategy(fake));
        api.createTask(request("Write essay", "Essay for class", "false", null, "HIGH"), null);

        ApiResponse first = api.breakDownTask(1, "bd-1");
        check("breakdown returns 201", first.getStatus() == 201);
        check("three subtasks created", REPO.findAll().size() == 4 && count(first.toJson(), "\"parentId\":1") == 3);

        ApiResponse replay = api.breakDownTask(1, "bd-1");
        check("replay returns the same response", replay.toJson().equals(first.toJson()));
        check("replay did NOT call the client again", fake.getCalls() == 1);
        check("replay created no extra children", REPO.findAll().size() == 4);
        check("same key, different task -> 422", api.breakDownTask(2, "bd-1").getStatus() == 422);

        check("breaking down a subtask -> 400", api.breakDownTask(2, null).getStatus() == 400);
        check("unknown id -> 404", api.breakDownTask(999, null).getStatus() == 404);

        String[][] bad = {
                {"line without |", "Research|Facts\nthis line has no bar"},
                {"empty title item", "Research|Facts\n|no title here"},
                {"empty description item", "Research|Facts\nWrite|"},
                {"plain words", "Sure, here are some ideas"},
                {"empty reply", ""},
        };
        for (String[] c : bad) {
            TaskApi badApi = apiWith(new LlmTaskParser(new FakeLlmClient()), new LlmBreakdownStrategy(new FakeLlmClient(c[1])));
            badApi.createTask(request("Parent", "Parent task", "false", null, null), null);
            ApiResponse r = badApi.breakDownTask(1, null);
            check(c[0] + " -> 502", r.getStatus() == 502);
            check(c[0] + " -> nothing saved", REPO.findAll().size() == 1);
        }

        TaskApi down = apiWith(new LlmTaskParser(new FakeLlmClient()), new LlmBreakdownStrategy(new FaultyLlmClient()));
        down.createTask(request("Parent", "Parent task", "false", null, null), null);
        check("network failure -> 502", down.breakDownTask(1, null).getStatus() == 502);

        TaskApi noKey = apiWith(new LlmTaskParser(new FakeLlmClient()), new LlmBreakdownStrategy(ParserFactory.missingKeyClient()));
        noKey.createTask(request("Parent", "Parent task", "false", null, null), null);
        check("missing key -> 503", noKey.breakDownTask(1, null).getStatus() == 503);
        check("503 saved nothing", REPO.findAll().size() == 1);

        TaskApi offline = freshOffline();
        offline.createTask(request("Buy milk, eggs and bread", "Shopping", "false", null, null), null);
        ApiResponse rule = offline.breakDownTask(1, null);
        check("rule-based breakdown splits into 3", rule.getStatus() == 201 && count(rule.toJson(), "\"parentId\":1") == 3);
    }

    static void queries() {
        System.out.println("\n== Filter, search, sort, pagination ==");
        TaskApi api = freshOffline();
        api.createTask(request("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH"), null);
        api.createTask(request("Buy milk", "Groceries for rent party", "false", "2026-11-30", "LOW"), null);
        api.createTask(request("Write report", "Quarterly", "true", "2026-12-15", "HIGH"), null);
        api.createTask(request("Call mom", "Weekly call", "false", null, "MEDIUM"), null);
        api.createTask(request("Pay rent late fee", "rent", "false", "2026-12-05", "HIGH"), null);

        TaskQuery combined = TaskQuery.builder()
                .completed("false").priority("HIGH").search("RENT")
                .sortBy("dueDate").order("desc").page(1).pageSize(1).build();
        ApiResponse page1 = api.listTasks(combined);
        check("combined query returns 200", page1.getStatus() == 200);
        check("page 1 holds the latest due date", page1.toJson().contains("Pay rent late fee") && !page1.toJson().contains("\"title\":\"Pay rent\""));
        check("totals are right", page1.toJson().contains("\"totalItems\":2") && page1.toJson().contains("\"totalPages\":2"));

        TaskQuery second = TaskQuery.builder()
                .completed("false").priority("HIGH").search("RENT")
                .sortBy("dueDate").order("desc").page(2).pageSize(1).build();
        check("page 2 holds the other task", api.listTasks(second).toJson().contains("\"title\":\"Pay rent\""));

        ApiResponse empty = api.listTasks(TaskQuery.builder().page(99).pageSize(10).build());
        check("page beyond the last returns empty items", empty.getStatus() == 200 && empty.toJson().contains("\"items\":[]"));
        check("empty page still reports totalItems", empty.toJson().contains("\"totalItems\":5"));

        check("completed=true filter", api.listTasks(TaskQuery.builder().completed("true").build()).toJson().contains("\"totalItems\":1"));
        check("search is case-insensitive", api.listTasks(TaskQuery.builder().search("GROCERIES").build()).toJson().contains("\"totalItems\":1"));

        ApiResponse byDue = api.listTasks(TaskQuery.builder().sortBy("dueDate").order("asc").build());
        check("no-date task sorts last", byDue.toJson().indexOf("Call mom") > byDue.toJson().indexOf("Write report"));

        ApiResponse defaults = api.listTasks(TaskQuery.builder().build());
        check("defaults: page 1, size 10", defaults.toJson().contains("\"page\":1") && defaults.toJson().contains("\"pageSize\":10"));
        check("null query works", api.listTasks((TaskQuery) null).getStatus() == 200);

        check("invalid priority -> 400", api.listTasks(TaskQuery.builder().priority("URGENT").build()).getStatus() == 400);
        check("invalid completed -> 400", api.listTasks(TaskQuery.builder().completed("maybe").build()).getStatus() == 400);
        check("invalid sortBy -> 400", api.listTasks(TaskQuery.builder().sortBy("title").build()).getStatus() == 400);
        check("page 0 -> 400", api.listTasks(TaskQuery.builder().page(0).build()).getStatus() == 400);
        check("pageSize 1000 -> 400", api.listTasks(TaskQuery.builder().pageSize(1000).build()).getStatus() == 400);

        TaskQuery custom = TaskQuery.builder().addFilter(t -> t.getTitle().startsWith("Call")).build();
        check("new filter plugs in with no edits", api.listTasks(custom).toJson().contains("\"totalItems\":1"));

        ApiResponse high = api.listByPriority("HIGH");
        check("listByPriority HIGH -> 3 tasks", high.getStatus() == 200 && count(high.toJson(), "\"priority\":\"HIGH\"") == 3);
        check("listByPriority invalid -> 400", api.listByPriority("URGENT").getStatus() == 400);
        check("listByPriority null -> 400", api.listByPriority(null).getStatus() == 400);
    }

    static void events() {
        System.out.println("\n== Events (Observer) ==");
        REPO.clear();
        IdempotencyStore.getInstance().clear();
        TaskService service = new TaskService(REPO, new RuleBasedTaskParser(), new RuleBasedBreakdownStrategy());
        TaskApi api = new TaskApi(service);
        AuditTrailListener audit = new AuditTrailListener();
        service.addListener(new ConsoleLogListener());
        service.addListener(audit);

        api.createTask(request("Pay rent", "Monthly rent", "false", null, null), null);
        check("created counted", audit.getCount(TaskEventListener.CREATED) == 1);

        api.updateTask(1, request("Pay rent", "Monthly rent", "false", null, "HIGH"));
        check("updated counted", audit.getCount(TaskEventListener.UPDATED) == 1);
        check("not completed yet", audit.getCount(TaskEventListener.COMPLETED) == 0);

        api.updateTask(1, request("Pay rent", "Monthly rent", "true", null, "HIGH"));
        check("completed counted", audit.getCount(TaskEventListener.COMPLETED) == 1);

        api.deleteTask(1);
        check("deleted counted", audit.getCount(TaskEventListener.DELETED) == 1);

        service.removeListener(audit);
        api.createTask(request("Another", "Another task", "false", null, null), null);
        check("removed listener stops receiving events", audit.getCount(TaskEventListener.CREATED) == 1);

        TaskEventListener broken = (type, task) -> { throw new RuntimeException("boom"); };
        service.addListener(broken);
        check("a failing listener does not break the API", api.createTask(request("Safe", "Safe task", "false", null, null), null).getStatus() == 201);
    }

    private static TaskApi freshOffline() {
        return apiWith(new RuleBasedTaskParser(), new RuleBasedBreakdownStrategy());
    }

    private static TaskApi apiWith(TaskParser parser, BreakdownStrategy breakdown) {
        REPO.clear();
        IdempotencyStore.getInstance().clear();
        return new TaskApi(new TaskService(REPO, parser, breakdown));
    }

    private static TaskRequest request(String title, String description, String completed, String dueDate, String priority) {
        return new TaskRequest(title, description, completed, dueDate, priority);
    }

    private static int count(String text, String part) {
        int n = 0;
        int i = text.indexOf(part);
        while (i >= 0) {
            n++;
            i = text.indexOf(part, i + part.length());
        }
        return n;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) passed++; else failed++;
    }
}