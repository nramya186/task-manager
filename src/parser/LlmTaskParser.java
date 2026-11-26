package parser;

import exception.BadGatewayException;
import model.ParsedTask;

import java.io.IOException;
import java.util.Map;

public class LlmTaskParser implements TaskParser {

    private static final String PROMPT = """
            Today's date is {today} (YYYY-MM-DD).
            Turn the user's request into ONE task.
            Reply with ONLY a JSON object, no explanation, no code fences, with exactly these fields:
              "title": short text, the action to do (string, never empty)
              "description": one sentence describing the task (string)
              "dueDate": the due date as YYYY-MM-DD, resolved from today's date, or null if none
              "recurrence": one of "NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
            If a value is unknown use null (recurrence: "NONE").

            Example input: Remind me to pay rent on the 1st of every month
            Example output: {"title":"Pay rent","description":"Pay the monthly rent","dueDate":"2026-12-01","recurrence":"MONTHLY"}

            User request: {text}
            """;

    private final LlmClient client;

    public LlmTaskParser(LlmClient client) {
        this.client = client;
    }

    @Override
    public ParsedTask parse(String text, String today) {
        String prompt = PROMPT.replace("{today}", today).replace("{text}", text);

        String reply;
        try {
            reply = client.send(prompt);
        } catch (IOException e) {
            throw new BadGatewayException("The model call failed or timed out");
        }

        Map<String, String> fields;
        try {
            fields = SimpleJson.parseFlat(reply);
        } catch (IllegalArgumentException e) {
            throw new BadGatewayException("The model reply was not a valid JSON object");
        }

        return new ParsedTask(
                fields.get("title"),
                fields.get("description"),
                fields.get("dueDate"),
                fields.get("recurrence"));
    }
}