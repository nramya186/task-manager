package parser;

import model.Task;
import model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

public class RuleBasedBreakdownStrategy implements BreakdownStrategy {

    @Override
    public List<TaskRequest> breakDown(Task parent) {
        List<String> parts = split(parent.getTitle());
        if (parts.size() < 2) parts = split(parent.getDescription());

        List<TaskRequest> requests = new ArrayList<>();
        for (String part : parts) {
            requests.add(new TaskRequest(part, "Part of: " + parent.getTitle(), "false", null, null));
        }
        return requests;
    }

    private List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        if (text == null) return parts;
        for (String piece : text.split(",|\\band\\b")) {
            String trimmed = piece.trim();
            if (!trimmed.isEmpty()) parts.add(trimmed);
        }
        return parts;
    }
}
