# Contributing Guide

## Branching model

- `main` — always compiles, always runs. Nothing broken gets merged here.
- `dev` — integration branch, where finished track work lands before `main`.
- Feature branches: `track-<letter>/<short-description>`, e.g.
  `track-d/tls-handshake`, `track-a/selector-loop`.

## Workflow

1. Branch off `dev`.
2. Work in your track's owned files (see `07_ROADMAP_AND_TASKS.md`) — if
   you need to touch a file outside your track, ping the owner first.
3. Before opening a PR: compile clean, run the manual test checklist
   relevant to your change, run any existing JUnit tests.
4. Open PR into `dev`, tag the track's reviewer.
5. Reviewer checks against `06_CODING_STANDARDS.md`'s review checklist.
6. Periodically, `dev` gets merged into `main` once a phase's exit criteria
   (from `07_ROADMAP_AND_TASKS.md`) are met — this is a team decision, not
   an individual merge.

## Commit hygiene

- Small, focused commits over one giant "finished phase 3" commit — makes
  review and later debugging much easier.
- Don't commit compiled `.class` files or the `out/` directory — add a
  `.gitignore` in Phase 0 covering `out/`, `*.class`, `*.mv.db`
  (the H2 database file — don't commit real data either).

## When you're blocked

- Blocked on another track's unfinished file? Work against the interface
  described in `02_ARCHITECTURE.md` / `03_PROTOCOL.md` and mock it locally
  rather than waiting — the doc set exists precisely so tracks can proceed
  in parallel without the real dependency being done yet.
- Found a gap or contradiction in one of the design docs? Fix the doc in
  the same PR as the code, don't let them drift apart. Docs that don't
  match the code are worse than no docs.

## Definition of done (per task)

A task from the roadmap is done when:
- [ ] It compiles with no warnings
- [ ] It meets its phase's exit criteria (not just "seems to work")
- [ ] It's been reviewed by someone other than the author
- [ ] Any relevant doc (`03_PROTOCOL.md`, `04_DATABASE_SCHEMA.md`, etc.) is
      updated if behavior changed from what was originally specified
