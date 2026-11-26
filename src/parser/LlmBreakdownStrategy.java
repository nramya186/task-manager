package parser;

import exception.BadGatewayException;
import model.Task;
import model.TaskRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LlmBreakdownStrategy implements BreakdownStrategy {

    private static final int MAX_ITEMS = 10;

    private static final String PROMPT = """
            Break the task below into 3 to 5 small, concrete subtasks.
            Reply with ONLY the subtasks, one per line, in exactly this format:
            title|description
            No numbering, no bullets, no blank lines, no explanation, no code fences.

            Example for "Plan a birthday party":
            Choose a date|Pick a date that suits the guests
            Send invitations|Invite guests by message
            Order a cake|Order the cake from a bakery

            Task title: {title}
            Task description: {description}
            """;

    private final LlmClient client;

    public LlmBreakdownStrategy(LlmClient client) {
        this.client = client;
    }

    @Override
    public List<TaskRequest> breakDown(Task parent) {
        String prompt = PROMPT
                .replace("{title}", parent.getTitle())
                .replace("{description}", parent.getDescription());

        String reply;
        try {
            reply = client.send(prompt);
        } catch (IOException e) {
            throw new BadGatewayException("The model call failed or timed out");
        }
        if (reply == null) throw new BadGatewayException("The model returned no text");

        List<TaskRequest> requests = new ArrayList<>();
        for (String line : reply.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("```")) continue;

            int bar = trimmed.indexOf('|');
            if (bar < 0) {
                throw new BadGatewayException("A breakdown line was not in title|description format");
            }
            String title = trimmed.substring(0, bar).trim();
            String description = trimmed.substring(bar + 1).trim();
            requests.add(new TaskRequest(title, description, "false", null, null));

            if (requests.size() > MAX_ITEMS) {
                throw new BadGatewayException("The model returned too many subtasks");
            }
        }
        return requests;
    }
}
