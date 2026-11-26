package parser;

import model.ParsedTask;

import java.time.LocalDate;

public class RuleBasedTaskParser implements TaskParser {

    private static final String PREFIX = "remind me to ";
    private static final String[] CUT_MARKERS = {" on the ", " every ", " tomorrow", " today"};

    @Override
    public ParsedTask parse(String text, String today) {
        String original = text.trim();
        String lower = original.toLowerCase();

        String working = lower.startsWith(PREFIX)
                ? original.substring(PREFIX.length()).trim()
                : original;

        String workingLower = working.toLowerCase();
        int cut = working.length();
        for (String marker : CUT_MARKERS) {
            int index = workingLower.indexOf(marker);
            if (index > 0 && index < cut) cut = index;
        }

        String title = working.substring(0, cut).trim();
        if (title.isEmpty()) title = working;
        if (!title.isEmpty()) title = title.substring(0, 1).toUpperCase() + title.substring(1);

        String recurrence = "NONE";
        if (lower.contains("every day") || lower.contains("daily")) recurrence = "DAILY";
        else if (lower.contains("every week") || lower.contains("weekly")) recurrence = "WEEKLY";
        else if (lower.contains("every month") || lower.contains("monthly")) recurrence = "MONTHLY";
        else if (lower.contains("every year") || lower.contains("yearly")) recurrence = "YEARLY";

        String dueDate = null;
        LocalDate base = LocalDate.parse(today);
        if (lower.contains("tomorrow")) dueDate = base.plusDays(1).toString();
        else if (lower.contains("today")) dueDate = base.toString();
        else if (lower.contains("1st")) dueDate = base.withDayOfMonth(1).plusMonths(1).toString();

        return new ParsedTask(title, original, dueDate, recurrence);
    }
}