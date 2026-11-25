package api;


import exception.ApiException;
import exception.FieldIssue;
import exception.ValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * INSTRUCTOR-PROVIDED SIGNATURES ONLY (Step 3) - students write the bodies.
 *
 * The one place where the error shape is built. Target shape:
 *
 * {
 *   "error": {
 *     "code": "VALIDATION_ERROR",
 *     "message": "Validation failed",
 *     "details": [ { "field": "title", "issue": "must not be empty" } ]
 *   }
 * }
 *
 * Hints:
 *  - Build the body with LinkedHashMap / ArrayList (see ListMapEnumPrimer).
 *  - "code" and "message" are ALWAYS Strings.
 *  - "details" is only filled for ValidationException (use instanceof);
 *    for every other exception send an empty list.
 *  - The ApiResponse status comes from e.getStatus().
 *
 * When you copy this into your own project, change the package line to: package api;
 */
public class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    /** Known failure (400 / 404 / 422 / 502 / 503). */
    public static ApiResponse from(ApiException e) {
        // TODO (s): build the error body and return new ApiResponse(e.getStatus(), body)
        List<Map<String, Object>> details = new ArrayList<>();

        if (e instanceof ValidationException validationException) {
            for (FieldIssue issue : validationException.getIssues()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("field", issue.getField());
                detail.put("issue", issue.getIssue());

                details.add(detail);
            }
        }

        return buildResponse(
                e.getStatus(),
                e.getCode(),
                e.getMessage(),
                details
        );
    }

    /** Anything unexpected: 500, code "INTERNAL_ERROR", same shape. Never leak a stack trace. */
    public static ApiResponse fromUnexpected(Exception e) {
        // TODO (student)
        return buildResponse(
                500,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                new ArrayList<>()
        );
}
    private static ApiResponse buildResponse(
            int status,
            String code,
            String message,
            List<Map<String, Object>> details
    ) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("details", details);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);

        return new ApiResponse(status, body);
    }

}

