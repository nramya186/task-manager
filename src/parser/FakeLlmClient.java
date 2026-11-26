package parser;

public class FakeLlmClient implements LlmClient {

    public static final String DEFAULT_TASK_REPLY =
            "{\"title\":\"Pay rent\",\"description\":\"Pay the monthly rent\","
                    + "\"dueDate\":\"2026-12-01\",\"recurrence\":\"MONTHLY\"}";

    private final String reply;
    private int calls;

    public FakeLlmClient() { this(DEFAULT_TASK_REPLY); }

    public FakeLlmClient(String reply) { this.reply = reply; }

    @Override
    public String send(String prompt) {
        calls++;
        return reply;
    }

    public int getCalls() { return calls; }
}
