# API_CONTRACT.md

This document describes the public operations of `TaskApi`. Every method returns an `ApiResponse` with a numeric `status` and a JSON-style `body`.

## Conventions

- Dates use `YYYY-MM-DD`. IDs are integers.
- `priority` is `LOW`, `MEDIUM` or `HIGH`; the default is `MEDIUM`.
- Every task body contains: `id`, `title`, `description`, `completed`, `dueDate`, `priority`, `parentId`, `recurrence` (`NONE`, `DAILY`, `WEEKLY`, `MONTHLY` or `YEARLY`) and `createdAt`.
- `idempotencyKey` is optional (may be `null`). It is accepted by `createTask`, `createTaskFromText` and `breakDownTask`.
    - Same key and same request: the original response is returned and nothing new is created. The model is not called again.
    - Same key and a different request: `422 CONFLICT`.
    - Only successful responses are remembered, so a failed request can be fixed and retried with the same key.
- Every error uses the single shape described under **Errors**.

## Operations

### createTask

| Item | Value |
|---|---|
| Java method | `ApiResponse createTask(TaskRequest request, String idempotencyKey)` |
| Purpose | Create one task from structured input. |
| Success | `201`: the created task |
| Errors | `400 VALIDATION_ERROR`, `422 CONFLICT`, `500 INTERNAL_ERROR` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| title | String | yes | Not empty or blank |
| description | String | yes | Not empty or blank |
| completed | String | yes | Exactly `"true"` or `"false"` |
| dueDate | String | no | A real `YYYY-MM-DD` date (`2026-02-30` is rejected) |
| priority | String | no | `LOW`, `MEDIUM` or `HIGH` (case-sensitive) |
| idempotencyKey | String | no | See Conventions |

**Example request**

```text
title = "Pay rent"   description = "Monthly rent"   completed = "false"
dueDate = "2026-12-01"   priority = "HIGH"   idempotencyKey = "rent-001"
```

**Example success response**

```json
{"status":201,"body":{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795767903039}}
```

**Example error response** (`title = ""`, `completed = "maybe"`)

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"title","issue":"must not be empty"},{"field":"completed","issue":"must be exactly \"true\" or \"false\""}]}}}
```

**Example error response** (same key, different request)

```json
{"status":422,"body":{"error":{"code":"CONFLICT","message":"Idempotency key reused with a different request","details":[]}}}
```

### getTask

| Item | Value |
|---|---|
| Java method | `ApiResponse getTask(int id)` |
| Purpose | Fetch one task, including a subtask, by its ID. |
| Success | `200`: the task |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| id | int | yes | Must match an existing task |

**Example request**: `id = 1`

**Example success response**

```json
{"status":200,"body":{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795767903039}}
```

**Example error response** (`id = 99`)

```json
{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task not found","details":[]}}}
```

### listTasks (all tasks)

| Item | Value |
|---|---|
| Java method | `ApiResponse listTasks()` |
| Purpose | Return every stored task, with no filtering or paging. |
| Success | `200`: an array of tasks (empty array when there are none) |
| Errors | `500 INTERNAL_ERROR` |

**Example success response**

```json
{"status":200,"body":[{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795767903039}]}
```

### listTasks (query)

| Item | Value |
|---|---|
| Java method | `ApiResponse listTasks(TaskQuery query)` |
| Purpose | Filter, search, sort and paginate tasks in one call. |
| Success | `200`: a page object |
| Errors | `400 VALIDATION_ERROR`, `500 INTERNAL_ERROR` |

**Parameters** (every field is optional; `null` query means defaults)

| Name | Type | Rules |
|---|---|---|
| completed | String | `"true"` or `"false"` |
| priority | String | `LOW`, `MEDIUM` or `HIGH` |
| search | String | Case-insensitive match in title or description |
| sortBy | String | `createdAt` (default) or `dueDate`; tasks without a due date sort last |
| order | String | `asc` (default) or `desc` |
| page | int | 1 or more; default 1 |
| pageSize | int | 1 to 100; default 10 |

Java callers can also plug in new filters (`addFilter`) or a sort (`sortStrategy`) without changing existing classes.

**Example request**

```text
completed = "false"   search = "a"   sortBy = "dueDate"   order = "asc"   page = 1   pageSize = 3
```

**Example success response** (items shortened to one entry)

```json
{"status":200,"body":{"items":[{"id":2,"title":"Call the dentist","description":"Remind me to call the dentist tomorrow","completed":false,"dueDate":"2026-11-28","priority":"MEDIUM","parentId":null,"recurrence":"NONE","createdAt":1795767903075}],"page":1,"pageSize":3,"totalItems":6,"totalPages":2}}
```

A page past the last one still returns `200` with `"items":[]` and the real `totalItems`.

**Example error response** (`priority = "URGENT"`) *(replace with real output)*

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"priority","issue":"must be LOW, MEDIUM, or HIGH"}]}}}
```

### updateTask

| Item | Value |
|---|---|
| Java method | `ApiResponse updateTask(int id, TaskRequest request)` |
| Purpose | Replace the editable fields of an existing task. `id`, `parentId`, `recurrence` and `createdAt` are kept. |
| Success | `200`: the updated task |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

**Parameters**: `id` (int, must exist), plus the same five fields and rules as `createTask`. A missing `dueDate` clears the date; a missing `priority` becomes `MEDIUM`.

**Example request**

```text
id = 1   title = "Pay rent"   description = "Monthly rent, December"   completed = "true"
dueDate = "2026-12-01"   priority = "HIGH"
```

**Example success response** *(replace with real output)*

```json
{"status":200,"body":{"id":1,"title":"Pay rent","description":"Monthly rent, December","completed":true,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795767903039}}
```

**Example error response** (`title = ""`) *(replace with real output)*

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"title","issue":"must not be empty"}]}}}
```

### deleteTask

| Item | Value |
|---|---|
| Java method | `ApiResponse deleteTask(int id)` |
| Purpose | Delete a task together with its direct subtasks. |
| Success | `200`: a confirmation |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| id | int | yes | Must match an existing task |

**Example request**: `id = 3`

**Example success response**

```json
{"status":200,"body":{"message":"Task deleted","deletedTaskId":3}}
```

**Example error response** (`id = 3` again, already deleted)

```json
{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task not found","details":[]}}}
```

### createTaskFromText

| Item | Value |
|---|---|
| Java method | `ApiResponse createTaskFromText(String text, String idempotencyKey)` |
| Purpose | Turn plain English into a validated task using the configured parser. |
| Success | `201`: the created task |
| Errors | `400`, `422 CONFLICT`, `502 BAD_GATEWAY`, `503 SERVICE_UNAVAILABLE`, `500` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| text | String | yes | Not empty or blank |
| idempotencyKey | String | no | See Conventions |

**Behavior**

- The model's reply is never trusted: the title, date, and recurrence are validated with the same rules as `createTask`.
- A failed call, a timeout, malformed output, or invalid output returns `502` and saves nothing.
- With the real model and no `LLM_API_KEY`, the response is `503`.
- A missing description is replaced by the title.

**Example request**

```text
text = "Remind me to pay rent on the 1st of every month"   idempotencyKey = "real-1"
```

**Example success response**

```json
{"status":201,"body":{"id":1,"title":"Pay rent","description":"Pay the monthly rent","completed":false,"dueDate":"2026-12-01","priority":"MEDIUM","parentId":null,"recurrence":"MONTHLY","createdAt":1795762683200}}
```

**Example error response** (the model replied with something that was not JSON) *(replace with real output)*

```json
{"status":502,"body":{"error":{"code":"BAD_GATEWAY","message":"The model reply was not a valid JSON object","details":[]}}}
```

**Example error response** (no key)

```json
{"status":503,"body":{"error":{"code":"SERVICE_UNAVAILABLE","message":"LLM_API_KEY is not set","details":[]}}}
```

### createSubtask

| Item | Value |
|---|---|
| Java method | `ApiResponse createSubtask(int parentId, TaskRequest request)` |
| Purpose | Create a task under an existing top-level task. |
| Success | `201`: the created subtask, with `parentId` set |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500` |

**Parameters**: `parentId` (int; must exist and must not be a subtask), plus the five fields and rules from `createTask`.

**Example request**

```text
parentId = 1   title = "Write tests"   description = "Check task creation and validation"
completed = "false"   priority = "HIGH"
```

**Example success response**

```json
{"status":201,"body":{"id":2,"title":"Write tests","description":"Check task creation and validation","completed":false,"dueDate":null,"priority":"HIGH","parentId":1,"recurrence":"NONE","createdAt":1795719406354}}
```

**Example error response** (`parentId = 2`, which is already a subtask)

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"parentId","issue":"a subtask cannot have subtasks"}]}}}
```

### listSubtasks

| Item | Value |
|---|---|
| Java method | `ApiResponse listSubtasks(int parentId)` |
| Purpose | List the direct subtasks of a task. |
| Success | `200`: an array (empty if the task has none) |
| Errors | `404 NOT_FOUND`, `500` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| parentId | int | yes | Must match an existing task |

**Example request**: `parentId = 1`

**Example success response** (shortened to one entry)

```json
{"status":200,"body":[{"id":2,"title":"Write tests","description":"Check task creation and validation","completed":false,"dueDate":null,"priority":"HIGH","parentId":1,"recurrence":"NONE","createdAt":1795719406354}]}
```

**Example error response** (`parentId = 999`)

```json
{"status":404,"body":{"error":{"code":"NOT_FOUND","message":"Task not found","details":[]}}}
```

### breakDownTask

| Item | Value |
|---|---|
| Java method | `ApiResponse breakDownTask(int id, String idempotencyKey)` |
| Purpose | Ask the breakdown strategy for subtasks of a task and save them. |
| Success | `201`: an array of the created subtasks |
| Errors | `400 VALIDATION_ERROR`, `404`, `422 CONFLICT`, `502`, `503`, `500` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| id | int | yes | Must match an existing task that is not itself a subtask |
| idempotencyKey | String | no | See Conventions |

**Behavior**

- Every generated item is validated before anything is saved. One bad item, a malformed line, an empty reply, or a failed call means nothing is saved and the response is `502`.
- A subtask cannot be broken down: `400`.
- With the real model and no key: `503`.

**Example request**: `id = 2   idempotencyKey = "real-bd-1"`

**Example success response** (shortened to two entries)

```json
{"status":201,"body":[{"id":3,"title":"Choose a date","description":"Pick a date that suits the guests for Sunday","completed":false,"dueDate":null,"priority":"MEDIUM","parentId":2,"recurrence":"NONE","createdAt":1795762684660},{"id":4,"title":"Send invitations","description":"Invite the guests by message","completed":false,"dueDate":null,"priority":"MEDIUM","parentId":2,"recurrence":"NONE","createdAt":1795762684660}]}
```

**Example error response** (item 2 had an empty title) *(replace with real output)*

```json
{"status":502,"body":{"error":{"code":"BAD_GATEWAY","message":"Breakdown item 2 failed validation: title must not be empty","details":[]}}}
```

**Example error response** (`id` is a subtask) *(replace with real output)*

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"id","issue":"a subtask cannot be broken down"}]}}}
```

### listByPriority

| Item | Value |
|---|---|
| Java method | `ApiResponse listByPriority(String level)` |
| Purpose | List all tasks with one priority level. |
| Success | `200`: an array |
| Errors | `400 VALIDATION_ERROR`, `500` |

**Parameters**

| Name | Type | Required | Rules |
|---|---|---|---|
| level | String | yes | `LOW`, `MEDIUM` or `HIGH`; `null` is rejected |

**Example request**: `level = "HIGH"`

**Example success response** *(replace with real output)*

```json
{"status":200,"body":[{"id":1,"title":"Pay rent","description":"Monthly rent","completed":false,"dueDate":"2026-12-01","priority":"HIGH","parentId":null,"recurrence":"NONE","createdAt":1795767903039}]}
```

**Example error response** (`level = "URGENT"`) *(replace with real output)*

```json
{"status":400,"body":{"error":{"code":"VALIDATION_ERROR","message":"Validation failed","details":[{"field":"priority","issue":"must be LOW, MEDIUM, or HIGH"}]}}}
```

## Errors

### Error shape

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [ { "field": "title", "issue": "must not be empty" } ]
  }
}
```

| Field | Type | Meaning |
|---|---|---|
| error.code | string | Stable identifier from the table below |
| error.message | string | Short human-readable summary |
| error.details | array | `{ "field", "issue" }` entries for validation errors; empty list otherwise |

### Error codes

| Status | Code | When it happens |
|---:|---|---|
| 400 | VALIDATION_ERROR | Input broke a rule; also breaking down a subtask, giving a subtask a subtask, a bad priority level, or a bad query value |
| 404 | NOT_FOUND | The task or parent ID does not exist |
| 422 | CONFLICT | An idempotency key was reused with a different request |
| 502 | BAD_GATEWAY | The model call failed or timed out, or its output failed validation. Nothing was saved |
| 503 | SERVICE_UNAVAILABLE | `LLM_API_KEY` is not set (real-model mode) |
| 500 | INTERNAL_ERROR | Any unexpected failure. No stack trace in the body |