# Task Manager Service

A task manager in plain Java 17 with no framework. It turns plain English such as
"Remind me to pay rent on the 1st of every month" into a validated task using an LLM,
breaks large tasks into subtasks, and supports safe retries (idempotency), filtering,
sorting and pagination. Each operation is a Java method on `TaskApi` that returns an
`ApiResponse` (a status code plus a JSON-style body), so the same service classes could
later sit behind real HTTP endpoints.

Full operation details and error codes: **[API contract](API_CONTRACT.md)**.

## Package structure

```text
src/
  api/         TaskApi, ApiResponse, ErrorResponseFactory
  app/         Main, TestRunner, demos (RealAiDemo, SubtaskDemo, IdempotencyDemo, LlmSmokeTest)
  event/       TaskEventListener, ConsoleLogListener, AuditTrailListener
  exception/   ApiException and its subclasses, FieldIssue
  model/       Task (immutable, Builder), TaskRequest, ParsedTask, Priority
  parser/      TaskParser, LlmTaskParser, RuleBasedTaskParser, BreakdownStrategy,
               LlmBreakdownStrategy, RuleBasedBreakdownStrategy, ParserFactory,
               LlmClient, HttpLlmClient, SimpleJson, FakeLlmClient, FaultyLlmClient
  repository/  TaskRepository, InMemoryTaskRepository, IdempotencyStore
  service/     TaskService, TaskQuery, TaskFilter, SortStrategy, filters and sorts, DateHelper
```

## Setup

**Requirements:** JDK 17 or newer. No external libraries.

**Compile and run** (from the project root, in Git Bash):

```bash
javac -d out $(find src -name "*.java")
java -cp out app.Main
java -cp out app.TestRunner
```

In IntelliJ, run the `main` method of each class instead.

**LLM key** (only needed for the real model; tests and `Main` work offline):

```bash
export LLM_API_KEY=your-key          # Git Bash / macOS / Linux
$env:LLM_API_KEY="your-key"          # PowerShell
```

Optional: `LLM_PROVIDER` (`anthropic`, `openai` or `gemini`) and `LLM_MODEL`
(set it if the default model name is rejected). In IntelliJ, add these under
Run, Edit Configurations, Environment variables for each class you run.
Never commit a key or a `.env` file; both are in `.gitignore`.

**Programs you can run**

| Class | Function                                          | key? |
|---|---------------------------------------------------|---|
| `Main` | Shows every operation, replay, delete, listeners  | No |
| `TestRunner` | Offline checks, prints PASS or FAIL               | No |
| `LlmSmokeTest` | Sends one prompt to the real model                | Yes |
| `RealAiDemo` | English to task and breakdown with the real model | Yes |

## Operations

| Operation | Parameters | Success | Errors |
|---|---|---|---|
| `createTask` | `TaskRequest`, `idempotencyKey` | 201 | 400, 422, 500 |
| `getTask` | `id` | 200 | 404, 500 |
| `listTasks()` | none | 200 | 500 |
| `listTasks(query)` | `TaskQuery` | 200 | 400, 500 |
| `updateTask` | `id`, `TaskRequest` | 200 | 400, 404, 500 |
| `deleteTask` | `id` | 200 | 404, 500 |
| `createTaskFromText` | `text`, `idempotencyKey` | 201 | 400, 422, 502, 503, 500 |
| `createSubtask` | `parentId`, `TaskRequest` | 201 | 400, 404, 500 |
| `listSubtasks` | `parentId` | 200 | 404, 500 |
| `breakDownTask` | `id`, `idempotencyKey` | 201 | 400, 404, 422, 502, 503, 500 |
| `listByPriority` | `level` | 200 | 400, 500 |

### Examples 

**createTask**

```java
api.createTask(new TaskRequest("Pay rent", "Monthly rent", "false", "2026-12-01", "HIGH"), "rent-001");
```
```json
{"status":201,"body":{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795771252165}}
```

**Validation error**

```java
api.createTask(new TaskRequest("", "x", "maybe", null, null), null);
```
```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"title","issue":"must not be empty"},{"field":"completed","issue":"must be exactly \"true\" or \"false\""}]}}}
```

**Idempotent replay.** The same call with the same key returns the same response and creates nothing new.

**getTask**: `api.getTask(1)` returns `200` with the task above; `api.getTask(99)` returns:

```json
{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task not found","details":[]}}}
```

**listTasks(query)** (items shortened to one entry)

```java
api.listTasks(TaskQuery.builder().completed("false").search("a")
        .sortBy("dueDate").order("asc").page(1).pageSize(3).build());
```
```json
{"status":200,"body":{"items":[{"id":2,"title":"Call the dentist","dueDate":"2026-11-28"}],"page":1,"pageSize":3,"totalItems":6,"totalPages":2}}
```

**updateTask**: 
updateTask: 
```json
{"status":200,"body":{"id":1,"title":"Pay rent","description":"Monthly rent, December","completed":true,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795771252165}}
```

**deleteTask**

```java
api.deleteTask(3);
```
```json
{"status":200,"body":{"message":"Task deleted","deletedTaskId":1}}
```

**createTaskFromText** (real model)

```java
api.createTaskFromText("Remind me to pay rent on the 1st of every month", "real-1");
```
```json
{"status":201,"body":{"id":1,"title":"Pay rent","description":"Pay the monthly rent","completed":false,"dueDate":"2026-12-01","priority":"MEDIUM","parentId":null,"recurrence":"MONTHLY","createdAt":1795762683200}}
```

**createSubtask**

```java
api.createSubtask(1, new TaskRequest("Write tests", "Check task creation and validation", "false", null, "HIGH"));
```
```json
{"status":201,"body":{"id":2,"title":"Write tests","description":"Check task creation and validation","completed":false,"dueDate":null,"priority":"HIGH","parentId":1,"recurrence":"NONE","createdAt":1795719406354}}
```

**listSubtasks**: `api.listSubtasks(1)` returns `200` with an array of the subtasks; an unknown parent returns `404`.

**breakDownTask** (real model, shortened to two entries)

```java
api.breakDownTask(2, "real-bd-1");
```
```json
{"status":201,"body":[{"id":3,"title":"Choose a date","parentId":2},{"id":4,"title":"Send invitations","parentId":2}]}
```

**listByPriority**: 
```json
listByPriority: {"status":200,"body":[{"id":1,"title":"Pay rent","description":"Monthly rent, December","completed":true,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795771252165}]}
```

## Design patterns

| Pattern | Place                                                                                                                     |
|---|---------------------------------------------------------------------------------------------------------------------------|
| Builder | `Task.Builder` (required title, description, completed; the rest optional), `TaskQuery.Builder`                           |
| Singleton | `InMemoryTaskRepository`, `IdempotencyStore`                                                                              |
| Factory | `ErrorResponseFactory` (one place builds the error shape), `ParserFactory` (chooses real or offline parser and breakdown) |
| Strategy | `TaskParser`, `BreakdownStrategy`, `LlmClient` (real, fake or faulty), `TaskFilter`, `SortStrategy`                       |
| Observer | `TaskService` notifies `TaskEventListener`s: `ConsoleLogListener`, `AuditTrailListener`                                   |

SOLID: layers depend on interfaces (`TaskService` receives a `TaskRepository`, a `TaskParser`
and a `BreakdownStrategy`), and a new filter or sort is a new class, not an edit to an
existing one.

## Plugging in another parser

```java
public interface TaskParser {
    ParsedTask parse(String text, String today);
}
```

Implement this interface (it returns title, description, due date and recurrence),
then pass your class to `new TaskService(repository, parser, breakdownStrategy)`.
`LlmTaskParser` and `RuleBasedTaskParser` are two examples.

## Prompts

**Task creation** (`LlmTaskParser`). 

Why it is written this way (put this in your own words): it gives today's date so "next Friday"
can be resolved; names every field and its type and says what to do when a value is unknown;
says "ONLY a JSON object, no explanation, no code fences"; and includes one worked example.
The reply is still treated as untrusted and is validated before anything is saved.

**Breakdown** (`LlmBreakdownStrategy`). 

Why: a fixed one-line format (`title|description`) is easy to parse with `String` methods, and the
example shows the exact output expected. Any line that does not match the format makes the whole
breakdown fail with 502 and nothing is saved.

## Tests

`TestRunner` uses `FakeLlmClient` and `FaultyLlmClient`, so it needs no key and no internet.
It checks CRUD, every validation rule (including leap years), idempotency (replay, conflict,
failed key reuse), subtask rules and cascade delete, AI creation and breakdown with good and bad
model output (empty title, impossible date, date as a number, bad recurrence, plain words,
truncated JSON, network failure, missing key), filtering, sorting and pagination together,
and the Observer events. Last run: 128 passed, 0 failed.

