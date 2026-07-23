# Coding Standards

Five people writing Java without a shared style guide produces five
different codebases stapled together. This file exists so that isn't this
project.

## Formatting

- 4-space indentation, no tabs.
- Opening brace on the same line (`if (x) {`), matching standard Java
  convention — not the C project's style, don't carry that over.
- One top-level public class per file, filename matches class name exactly.
- Max line length ~100 chars as a soft guideline, not a hard rule.

## Naming

- Classes: `PascalCase` (`ClientSession`, `AuthManager`).
- Methods/variables: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`, declared `static final`.
- No abbreviations that aren't obvious — `msg` and `conn` are fine, `usr`
  or `pkt` are not (learn from the C version's terse naming — it made the
  audit doc harder to write than it needed to be).

## Package rules

- `common` never imports from `server` or `client`. This is enforced by
  convention, not tooling (no build tool) — code reviewers should reject
  any PR that violates it.
- No class does two jobs. If `Database.java` starts doing input validation,
  or `ClientSession.java` starts constructing SQL, that's a sign the
  responsibility boundary has leaked — split it out.

## Error handling

- No empty catch blocks. Ever. If you're genuinely ignoring an exception,
  write a one-line comment saying why.
- Exceptions crossing the Selector loop must never propagate uncaught —
  one client's bad input should never crash the server for everyone else.
  Catch at the `ClientSession` boundary, log, and either send `MSG_ERROR`
  or disconnect just that client.
- Prefer specific exception types over catching `Exception` broadly, except
  at that top-level Selector-loop safety net.

## Comments & documentation

- Every public class gets a short Javadoc block explaining its
  responsibility (one paragraph, not a novel).
- Non-obvious concurrency assumptions get an inline comment — e.g. "this
  field is only ever touched from the Selector thread" is the kind of
  thing that saves the next person an hour of confusion.
- No commented-out code in commits. Delete it — git remembers it.

## Testing expectations

- New logic in `common/` (protocol parsing, crypto helpers) needs a JUnit
  test before merge — these are pure functions and cheap to test.
- Networking code is harder to unit test; at minimum, write a manual test
  script/checklist (see `07_ROADMAP_AND_TASKS.md`, Phase 7) and note it in
  the PR description.

## Git commit messages

- Imperative mood, short summary line: `Add TLS handshake to ClientSession`,
  not `Added` or `Adding`.
- Reference the phase/task from the roadmap doc if applicable:
  `[Phase 3] Add password hashing to AuthManager`.

## Review checklist (apply to every PR)

- [ ] No blocking calls on the Selector thread
- [ ] No raw SQL outside `Database.java`
- [ ] No unbounded reads (respect max lengths from `05_SECURITY.md`)
- [ ] No empty catch blocks
- [ ] Public classes have a Javadoc summary
- [ ] Follows naming/formatting conventions above
