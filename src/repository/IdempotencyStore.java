package repository;

import api.ApiResponse;
import exception.ConflictException;

import java.util.HashMap;
import java.util.Map;

public final class IdempotencyStore {

    private static final IdempotencyStore INSTANCE =
            new IdempotencyStore();

    private final Map<String, StoredRecord> records = new HashMap<>();

    private IdempotencyStore() {
    }

    public static IdempotencyStore getInstance() {
        return INSTANCE;
    }

    public synchronized ApiResponse find(
            String key,
            String fingerprint
    ) {
        StoredRecord record = records.get(key);

        if (record == null) {
            return null;
        }

        if (!record.fingerprint.equals(fingerprint)) {
            throw new ConflictException(
                    "Idempotency key reused with a different request"
            );
        }

        return record.response;
    }

    public synchronized void save(
            String key,
            String fingerprint,
            ApiResponse response
    ) {
        if (key == null) {
            return;
        }

        // Never remember failed responses.
        if (response.getStatus() < 200
                || response.getStatus() >= 300) {
            return;
        }

        StoredRecord existing = records.get(key);

        if (existing != null) {
            if (!existing.fingerprint.equals(fingerprint)) {
                throw new ConflictException(
                        "Idempotency key reused with a different request"
                );
            }

            return;
        }

        records.put(
                key,
                new StoredRecord(fingerprint, response)
        );
    }

    private static final class StoredRecord {

        private final String fingerprint;
        private final ApiResponse response;

        private StoredRecord(
                String fingerprint,
                ApiResponse response
        ) {
            this.fingerprint = fingerprint;
            this.response = response;
        }
    }
}