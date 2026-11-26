package parser;

import java.io.IOException;

public class FaultyLlmClient implements LlmClient {

    private final String badReply;
    private final boolean throwing;
    private int calls;

    public FaultyLlmClient(String badReply) {
        this.badReply = badReply;
        this.throwing = false;
    }

    public FaultyLlmClient() {
        this.badReply = null;
        this.throwing = true;
    }

    @Override
    public String send(String prompt) throws IOException {
        calls++;
        if (throwing) throw new IOException("simulated network failure");
        return badReply;
    }

    public int getCalls() { return calls; }
}
