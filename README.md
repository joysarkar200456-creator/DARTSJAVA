# DARTS-Java — Project Documentation Index

**DARTS** (Distributed Asynchronous Real-time Talk System) — Java rewrite.
A terminal-based, NIO-driven chat system supporting LAN and internet deployment,
persistent storage, TLS-encrypted transport, and room-based multi-user chat.

This is a rewrite and expansion of the original C implementation. Nothing here
depends on that codebase — this is a from-scratch design, built with lessons
learned from it (buffer safety, thread safety, and protocol clarity are
first-class concerns from day one, not retrofits).

## Reading order for new contributors

1. **[01_PROJECT_CHARTER.md](01_PROJECT_CHARTER.md)** — what we're building and why, scope, non-goals
2. **[02_ARCHITECTURE.md](02_ARCHITECTURE.md)** — module layout, how the pieces fit together
3. **[03_PROTOCOL.md](03_PROTOCOL.md)** — the wire protocol; the contract between client and server
4. **[04_DATABASE_SCHEMA.md](04_DATABASE_SCHEMA.md)** — persistence layer schema and access patterns
5. **[05_SECURITY.md](05_SECURITY.md)** — TLS, auth, password storage, threat model
6. **[06_CODING_STANDARDS.md](06_CODING_STANDARDS.md)** — how we write code so it's consistent across 5 people
7. **[07_ROADMAP_AND_TASKS.md](07_ROADMAP_AND_TASKS.md)** — phases, milestones, task ownership
8. **[08_CONTRIBUTING.md](08_CONTRIBUTING.md)** — git workflow, branching, PR/review process

## Quick facts

| | |
|---|---|
| Language | Java (stdlib only — no build tool, no external deps unless explicitly approved) |
| Networking | NIO (`Selector`, non-blocking channels) |
| Persistence | H2 embedded database (single `.jar`, no server setup) |
| Security | TLS via `SSLEngine`; salted+hashed passwords |
| Interface | Terminal only, both client and server |
| Deployment | LAN or public internet (same code, different reachability config) |

## Team

Sadat Shaharier Sahaf · Tajrian Quazi · Abidur Rahman Dipto · Ahnaf Mushfiq Nafees · Md. Abdur Rahim

Role assignments live in [07_ROADMAP_AND_TASKS.md](07_ROADMAP_AND_TASKS.md) — assign names to tracks there.
