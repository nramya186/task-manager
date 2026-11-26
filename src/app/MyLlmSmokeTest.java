package app;

import parser.HttpLlmClient;
import parser.LlmClient;

import java.io.IOException;

public class MyLlmSmokeTest {

    public static void main(String[] args) {
        String apiKey = System.getenv("LLM_API_KEY");
        String provider = System.getenv("LLM_PROVIDER");
        String model = System.getenv("LLM_MODEL");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println(
                    "Missing LLM_API_KEY. Add it to this run configuration."
            );
            return;
        }

        if (provider == null || provider.isBlank()) {
            System.out.println(
                    "Missing LLM_PROVIDER. Set it to gemini for this test."
            );
            return;
        }

        if (model == null || model.isBlank()) {
            System.out.println(
                    "Missing LLM_MODEL. Set it to an available model ID."
            );
            return;
        }

        LlmClient client = new HttpLlmClient(
                apiKey,
                provider,
                model
        );

        try {
            System.out.println("Sending one request...");

            String reply = client.send(
                    "Reply with the single word OK."
            );

            System.out.println("Model replied: " + reply);

        } catch (IOException e) {
            String message = e.getMessage();

            // Remove the exact API key if it appears in an error message.
            if (message != null && apiKey != null && !apiKey.isBlank()) {
                message = message.replace(apiKey, "[REDACTED]");
            }

            System.out.println(
                    "Failure type: " + e.getClass().getSimpleName()
            );

            System.out.println(
                    "Reason: " + (message == null ? "No message available" : message)
            );
        }
    }
}
