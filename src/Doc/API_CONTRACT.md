# API_CONTRACT.md

This document describes the public operations exposed by `TaskApi`.

Every `TaskApi` method returns an `ApiResponse`, which contains:

- A numeric `status`
- A JSON-style `body`

Success bodies contain either a task object, a list of task objects, or a small confirmation object.

Every failure body uses the same error shape described in **Errors**.

`idempotencyKey` is optional and may be `null`. Its behavior is described for each operation that accepts it.

## Conventions

- IDs are integers.
- Dates use the `YYYY-MM-DD` format.
- Priority values are `LOW`, `MEDIUM`, or `HIGH`.
- The default priority is `MEDIUM`.
- A successful creation returns status `201`.
- A successful read, update, completion, deletion, or listing returns status `200`.
- Validation failures return status `400`.
- Unknown task or parent IDs return status `404`.
- Reusing an idempotency key with a different request returns status `422`.
- LLM failures or invalid model output return status `502`.
- A missing `LLM_API_KEY` returns status `503`.
- Unexpected internal failures return status `500`.

## Operations

### createTask

| Item | Value |
|---|---|
| Java method | `ApiResponse createTask(TaskRequest request, String idempotencyKey)` |
| Purpose | Create one task from structured input |
| Success | `201` — the created task |
| Errors | `400 VALIDATION_ERROR`, `422 CONFLICT`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `title` | String | Yes | Not empty or blank |
| `description` | String | Yes | Not empty or blank |
| `completed` | String | Yes | Exactly `"true"` or `"false"` |
| `dueDate` | String | No | A real calendar date in `YYYY-MM-DD` format; `2026-02-30` is rejected |
| `priority` | String | No | `LOW`, `MEDIUM`, or `HIGH`; defaults to `MEDIUM` |
| `idempotencyKey` | String | No | Same key and same request returns the original response; nothing new is created |

#### Example request

```text
title = "Pay rent"
description = "Monthly rent"
completed = "false"
dueDate = "2026-12-01"
priority = "HIGH"
idempotencyKey = "rent-001"
```

#### Example success response

```json
{
  "status": 201,
  "body": {
    "id": 1,
    "title": "Pay rent",
    "description": "Monthly rent",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1788000000000
  }
}
```

#### Example error response

Request with `title = ""`:

```json
{
  "status": 400,
  "body": {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": [
        {
          "field": "title",
          "issue": "must not be empty"
        }
      ]
    }
  }
}
```

### getTask

| Item | Value |
|---|---|
| Java method | `ApiResponse getTask(int id)` |
| Purpose | Retrieve one task by its ID |
| Success | `200` — the requested task |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `id` | int | Yes | Must identify an existing task |

#### Example request

```text
id = 1
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "id": 1,
    "title": "Pay rent",
    "description": "Monthly rent",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1788000000000
  }
}
```

#### Example error response

Request with `id = 999`, where no task has that ID:

```json
{
  "status": 404,
  "body": {
    "error": {
      "code": "NOT_FOUND",
      "message": "Task not found",
      "details": []
    }
  }
}
```

### listTasks

| Item | Value |
|---|---|
| Java method | `ApiResponse listTasks(TaskQuery query)` |
| Purpose | Filter, search, sort, and paginate tasks |
| Success | `200` — a page of matching tasks |
| Errors | `400 VALIDATION_ERROR`, `500 INTERNAL_ERROR` |

#### Parameters

All `TaskQuery` fields are optional.

| Name | Type | Required | Rules |
|---|---|---|---|
| `completed` | String | No | Exactly `"true"` or `"false"` |
| `priority` | String | No | `LOW`, `MEDIUM`, or `HIGH` |
| `search` | String | No | Matches task title or description text |
| `dueBefore` | String | No | A real calendar date in `YYYY-MM-DD` format |
| `dueAfter` | String | No | A real calendar date in `YYYY-MM-DD` format |
| `sortBy` | String | No | Supported values: `title`, `dueDate`, `priority`, `createdAt` |
| `sortDirection` | String | No | `asc` or `desc`; defaults to `asc` |
| `page` | String | No | Positive integer; defaults to `1` |
| `pageSize` | String | No | Positive integer; defaults to `10` |

#### Example request

```text
completed = "false"
priority = "HIGH"
search = "rent"
sortBy = "dueDate"
sortDirection = "asc"
page = "1"
pageSize = "10"
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "page": 1,
    "pageSize": 10,
    "totalTasks": 1,
    "tasks": [
      {
        "id": 1,
        "title": "Pay rent",
        "description": "Monthly rent",
        "completed": false,
        "dueDate": "2026-12-01",
        "priority": "HIGH",
        "parentId": null,
        "createdAt": 1788000000000
      }
    ]
  }
}
```

#### Example error response

Request with `priority = "URGENT"`:

```json
{
  "status": 400,
  "body": {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": [
        {
          "field": "priority",
          "issue": "must be LOW, MEDIUM, or HIGH"
        }
      ]
    }
  }
}
```

### updateTask

| Item | Value |
|---|---|
| Java method | `ApiResponse updateTask(int id, TaskRequest request)` |
| Purpose | Replace the editable fields of an existing task |
| Success | `200` — the updated task |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `id` | int | Yes | Must identify an existing task |
| `title` | String | Yes | Not empty or blank |
| `description` | String | Yes | Not empty or blank |
| `completed` | String | Yes | Exactly `"true"` or `"false"` |
| `dueDate` | String | No | A real calendar date in `YYYY-MM-DD` format |
| `priority` | String | No | `LOW`, `MEDIUM`, or `HIGH`; defaults to `MEDIUM` |

#### Example request

```text
id = 1
title = "Pay rent"
description = "Monthly rent for December"
completed = "false"
dueDate = "2026-12-01"
priority = "HIGH"
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "id": 1,
    "title": "Pay rent",
    "description": "Monthly rent for December",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "HIGH",
    "parentId": null,
    "createdAt": 1788000000000
  }
}
```

#### Example error response

Request with `completed = "yes"`:

```json
{
  "status": 400,
  "body": {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": [
        {
          "field": "completed",
          "issue": "must be exactly true or false"
        }
      ]
    }
  }
}
```

### deleteTask

| Item | Value |
|---|---|
| Java method | `ApiResponse deleteTask(int id)` |
| Purpose | Delete a task and all of its direct subtasks |
| Success | `200` — confirmation that the task was deleted |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `id` | int | Yes | Must identify an existing task |

#### Example request

```text
id = 1
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "message": "Task deleted",
    "deletedTaskId": 1,
    "deletedSubtaskIds":[2][3]
  }
}
```

#### Example error response

Request with `id = 999`:

```json
{
  "status": 404,
  "body": {
    "error": {
      "code": "NOT_FOUND",
      "message": "Task not found",
      "details": []
    }
  }
}
```

### createTaskFromText

| Item | Value |
|---|---|
| Java method | `ApiResponse createTaskFromText(String text, String idempotencyKey)` |
| Purpose | Create a task from plain English using a task parser |
| Success | `201` — the created task |
| Errors | `400 VALIDATION_ERROR`, `422 CONFLICT`, `502 BAD_GATEWAY`, `503 SERVICE_UNAVAILABLE`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `text` | String | Yes | Not empty or blank |
| `idempotencyKey` | String | No | Same key and same request returns the original response; the parser is not called again |

#### Behavior

- If `LLM_API_KEY` is missing or blank, return `503 SERVICE_UNAVAILABLE`.
- If the parser is the LLM parser and the model call fails, times out, returns malformed output, or returns invalid task data, return `502 BAD_GATEWAY`.
- The parsed result is converted into a `TaskRequest` and validated using the same rules as `createTask`.
- No task is saved if validation fails.

#### Example request

```text
text = "Remind me to pay rent on the 1st of every month"
idempotencyKey = "rent-text-001"
```

#### Example success response

```json
{
  "status": 201,
  "body": {
    "id": 4,
    "title": "Pay rent",
    "description": "Pay monthly rent",
    "completed": false,
    "dueDate": "2026-12-01",
    "priority": "MEDIUM",
    "parentId": null,
    "recurrence": "MONTHLY",
    "createdAt": 1788000000000
  }
}
```

#### Example error response

LLM returns an empty title:

```json
{
  "status": 502,
  "body": {
    "error": {
      "code": "BAD_GATEWAY",
      "message": "LLM output failed validation",
      "details": [
        {
          "field": "title",
          "issue": "must not be empty"
        }
      ]
    }
  }
}
```

### createSubtask

| Item | Value |
|---|---|
| Java method | `ApiResponse createSubtask(int parentId, TaskRequest request)` |
| Purpose | Create a task that belongs to an existing parent task |
| Success | `201` — the created subtask |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `parentId` | int | Yes | Must identify an existing task that is not itself a subtask |
| `title` | String | Yes | Not empty or blank |
| `description` | String | Yes | Not empty or blank |
| `completed` | String | Yes | Exactly `"true"` or `"false"` |
| `dueDate` | String | No | A real calendar date in `YYYY-MM-DD` format |
| `priority` | String | No | `LOW`, `MEDIUM`, or `HIGH`; defaults to `MEDIUM` |

#### Example request

```text
parentId = 1
title = "Find bank account details"
description = "Locate the landlord's bank details"
completed = "false"
priority = "HIGH"
```

#### Example success response

```json
{
  "status": 201,
  "body": {
    "id": 5,
    "title": "Find bank account details",
    "description": "Locate the landlord's bank details",
    "completed": false,
    "dueDate": null,
    "priority": "HIGH",
    "parentId": 1,
    "createdAt": 1788000000000
  }
}
```

#### Example error response

Request where `parentId = 5` and task 5 is already a subtask:

```json
{
  "status": 400,
  "body": {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": [
        {
          "field": "parentId",
          "issue": "a subtask cannot have subtasks"
        }
      ]
    }
  }
}
```

### listSubtasks

| Item | Value |
|---|---|
| Java method | `ApiResponse listSubtasks(int parentId)` |
| Purpose | List all direct subtasks of an existing parent task |
| Success | `200` — a list of subtasks |
| Errors | `404 NOT_FOUND`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `parentId` | int | Yes | Must identify an existing task |

#### Example request

```text
parentId = 1
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "parentId": 1,
    "subtasks": [
      {
        "id": 5,
        "title": "Find bank account details",
        "description": "Locate the landlord's bank details",
        "completed": false,
        "dueDate": null,
        "priority": "HIGH",
        "parentId": 1,
        "createdAt": 1788000000000
      }
    ]
  }
}
```

#### Example error response

Request with `parentId = 999`:

```json
{
  "status": 404,
  "body": {
    "error": {
      "code": "NOT_FOUND",
      "message": "Parent task not found",
      "details": []
    }
  }
}
```

### breakDownTask

| Item | Value |
|---|---|
| Java method | `ApiResponse breakDownTask(int id, String idempotencyKey)` |
| Purpose | Break an existing parent task into validated subtasks |
| Success | `201` — the parent task and its created subtasks |
| Errors | `400 VALIDATION_ERROR`, `404 NOT_FOUND`, `422 CONFLICT`, `502 BAD_GATEWAY`, `503 SERVICE_UNAVAILABLE`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `id` | int | Yes | Must identify an existing task that is not itself a subtask |
| `idempotencyKey` | String | No | Same key and same request returns the original response; no additional subtasks are created |

#### Behavior

- A task that is already a subtask cannot be broken down; return `400 VALIDATION_ERROR`.
- If `LLM_API_KEY` is missing or blank, return `503 SERVICE_UNAVAILABLE`.
- If the breakdown strategy fails, returns malformed output, or produces any invalid subtask, return `502 BAD_GATEWAY`.
- All subtasks must be valid before any are saved; the operation is all-or-nothing.

#### Example request

```text
id = 1
idempotencyKey = "rent-breakdown-001"
```

#### Example success response

```json
{
  "status": 201,
  "body": {
    "parentTask": {
      "id": 1,
      "title": "Pay rent",
      "description": "Monthly rent",
      "completed": false,
      "dueDate": "2026-12-01",
      "priority": "HIGH",
      "parentId": null,
      "createdAt": 1788000000000
    },
    "subtasks": [
      {
        "id": 6,
        "title": "Check rent amount",
        "description": "Confirm the amount due this month",
        "completed": false,
        "dueDate": null,
        "priority": "MEDIUM",
        "parentId": 1,
        "createdAt": 1788000000000
      },
      {
        "id": 7,
        "title": "Make rent payment",
        "description": "Transfer the rent payment",
        "completed": false,
        "dueDate": null,
        "priority": "MEDIUM",
        "parentId": 1,
        "createdAt": 1788000000000
      }
    ]
  }
}
```

#### Example error response

LLM returns an invalid subtask title:

```json
{
  "status": 502,
  "body": {
    "error": {
      "code": "BAD_GATEWAY",
      "message": "LLM breakdown output failed validation",
      "details": [
        {
          "field": "title",
          "issue": "must not be empty"
        }
      ]
    }
  }
}
```

### listByPriority

| Item | Value |
|---|---|
| Java method | `ApiResponse listByPriority(String priority)` |
| Purpose | List all tasks with a given priority |
| Success | `200` — a list of matching tasks |
| Errors | `400 VALIDATION_ERROR`, `500 INTERNAL_ERROR` |

#### Parameters

| Name | Type | Required | Rules |
|---|---|---|---|
| `priority` | String | Yes | Must be `LOW`, `MEDIUM`, or `HIGH` |

#### Example request

```text
priority = "HIGH"
```

#### Example success response

```json
{
  "status": 200,
  "body": {
    "priority": "HIGH",
    "tasks": [
      {
        "id": 1,
        "title": "Pay rent",
        "description": "Monthly rent",
        "completed": false,
        "dueDate": "2026-12-01",
        "priority": "HIGH",
        "parentId": null,
        "createdAt": 1788000000000
      }
    ]
  }
}
```

#### Example error response

Request with `priority = "URGENT"`:

```json
{
  "status": 400,
  "body": {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Validation failed",
      "details": [
        {
          "field": "priority",
          "issue": "must be LOW, MEDIUM, or HIGH"
        }
      ]
    }
  }
}
```

## Errors

### Error shape

Every error, from every operation, uses this exact shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "title",
        "issue": "must not be empty"
      }
    ]
  }
}
```

| Field | Type | Meaning |
|---|---|---|
| `error.code` | String | Stable, machine-readable identifier |
| `error.message` | String | Short human-readable summary |
| `error.details` | Array | Zero or more `{ "field", "issue" }` entries; empty array when there is nothing field-specific |

### Error codes

| Status | Code | When it happens |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | Input broke a rule; also when breaking down a subtask, giving a subtask a subtask, or using an invalid priority in `listByPriority` |
| 404 | `NOT_FOUND` | The task ID or parent ID does not exist |
| 422 | `CONFLICT` | An idempotency key was reused with a different request |
| 502 | `BAD_GATEWAY` | The model call failed or timed out, or its output failed validation; nothing was saved |
| 503 | `SERVICE_UNAVAILABLE` | `LLM_API_KEY` is not set |
| 500 | `INTERNAL_ERROR` | Any unexpected failure; no stack trace is included in the response body |