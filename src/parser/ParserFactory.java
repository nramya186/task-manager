package parser;

import exception.ServiceUnavailableException;

public final class ParserFactory {

    private ParserFactory() { }

    public static LlmClient realClient() {
        String key = System.getenv("LLM_API_KEY");
        if (key == null || key.isBlank()) return missingKeyClient();
        return new HttpLlmClient(key, System.getenv("LLM_PROVIDER"), System.getenv("LLM_MODEL"));
    }

    public static LlmClient missingKeyClient() {
        return prompt -> {
            throw new ServiceUnavailableException("LLM_API_KEY is not set");
        };
    }

    public static TaskParser createTaskParser(boolean offline, LlmClient client) {
        return offline ? new RuleBasedTaskParser() : new LlmTaskParser(client);
    }

    public static BreakdownStrategy createBreakdownStrategy(boolean offline, LlmClient client) {
        return offline ? new RuleBasedBreakdownStrategy() : new LlmBreakdownStrategy(client);
    }
}