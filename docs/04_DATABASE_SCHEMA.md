# Database Schema

## Engine

**H2**, embedded, file-backed (`darts.mv.db` created in the server's working
directory). Chosen because it's a single `.jar` with zero external setup —
no separate DB server to install or run, which matters since this project
intentionally avoids a build tool. Accessed via plain JDBC from
`Database.java` — no ORM.

## Tables

### `users`

| Column | Type | Notes |
|---|---|---|
| `id` | INT, PK, auto-increment | |
| `username` | VARCHAR(32), UNIQUE, NOT NULL | |
| `password_hash` | VARCHAR(255), NOT NULL | see `05_SECURITY.md` for algorithm |
| `salt` | VARCHAR(64), NOT NULL | |
| `is_admin` | BOOLEAN, DEFAULT FALSE | |
| `created_at` | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP | |
| `last_login` | TIMESTAMP, NULLABLE | |

### `rooms`

| Column | Type | Notes |
|---|---|---|
| `id` | INT, PK, auto-increment | |
| `name` | VARCHAR(64), UNIQUE, NOT NULL | |
| `created_by` | INT, FK → `users.id` | |
| `created_at` | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP | |

A `general` room is seeded on first server startup if it doesn't exist.

### `messages`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT, PK, auto-increment | |
| `room_id` | INT, FK → `rooms.id`, NULLABLE | null for private messages |
| `sender_id` | INT, FK → `users.id` | |
| `recipient_id` | INT, FK → `users.id`, NULLABLE | set only for private messages |
| `body` | VARCHAR(2048), NOT NULL | |
| `sent_at` | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP | |

Exactly one of `room_id` / `recipient_id` should be set per row — enforce
this in `Database.java`, not just by convention.

### `audit_log` (optional, Phase 3+)

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT, PK, auto-increment | |
| `user_id` | INT, FK → `users.id`, NULLABLE | |
| `event` | VARCHAR(64) | e.g. `LOGIN_FAIL`, `KICK`, `REGISTER` |
| `detail` | VARCHAR(255), NULLABLE | |
| `occurred_at` | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP | |

Useful for the security writeup / demo ("here's proof we can detect brute
force attempts") without being required for core functionality.

## Access pattern rules

- **All SQL lives in `Database.java`.** No other class constructs a query
  string. This is the single place that needs review for SQL injection
  risk, and the only place that needs to change if we ever swap engines.
- **Always use `PreparedStatement`** with bound parameters — string
  concatenation into SQL is a hard no, no exceptions, even for
  "trusted" internal values.
- **DB calls never happen on the Selector thread** (see
  `02_ARCHITECTURE.md`) — always dispatched to the worker pool, results
  delivered back via the result queue.

## Message history behavior

On `MSG_JOIN_ROOM`, the server queries the most recent 50 messages for that
room (`ORDER BY sent_at DESC LIMIT 50`, then reversed for display order) and
sends them as a single `MSG_HISTORY` payload before the client starts
receiving live messages.

## Migration strategy

No migration framework for v1 — the schema is small enough that
`Database.java` runs `CREATE TABLE IF NOT EXISTS` statements on startup.
If the schema needs to change after data exists, document the change here
and add a manual `ALTER TABLE` step to that same startup routine, guarded
by a check (e.g. a `schema_version` row) so it only runs once.
