# Roadmap & Task Breakdown

## Suggested tracks (assign 1 primary owner + 1 reviewer per track)

| Track | Owns | Depends on |
|---|---|---|
| **A — Networking core** | `Server.java`, `ClientSession.java`, Selector loop | `common/Protocol.java` |
| **B — Protocol & common** | `Protocol.java`, `Message.java` | nothing — build first |
| **C — Persistence** | `Database.java`, schema, migrations | Track B |
| **D — Security** | `AuthManager.java`, `CryptoUtils.java`, TLS integration | Track A, C |
| **E — Client & UX** | `Client.java`, `ConsoleUI.java`, `ServerConnection.java` | Track B |

With 5 team members this maps roughly 1:1, but pairing is fine — Track D
(security) is the hardest and benefits from two people; Track E (client)
is the most independent and a good fit for one person working ahead.

## Phases

### Phase 0 — Setup (all hands, ~1 session)
- [ ] Create folder structure (see `02_ARCHITECTURE.md`)
- [ ] Write `scripts/compile.sh`, `run-server.sh`, `run-client.sh`
- [ ] Confirm everyone can compile and run a "hello world" client/server
      exchange with zero application logic — just prove the plumbing works
- [ ] Agree on JDK version across all machines

### Phase 1 — Core networking (Tracks A + B)
- [ ] `Protocol.java` message types + JSON framing (Track B)
- [ ] Selector event loop: accept, read, write, disconnect handling (A)
- [ ] Login-less broadcast chat working end-to-end (no auth/DB/TLS yet)
- [ ] Manual test: 3 terminal clients, LAN, chatting via `/all`
- **Exit criteria:** unauthenticated chat works over the network, no crashes
  on malformed input (see Phase 1 completion criteria from the original C
  audit — same bar, higher starting point)

### Phase 2 — Persistence (Track C)
- [ ] H2 setup, schema creation on startup (`04_DATABASE_SCHEMA.md`)
- [ ] `users`/`rooms`/`messages` tables + JDBC access methods
- [ ] Message history replay on room join
- **Exit criteria:** server restart doesn't lose data

### Phase 3 — Security (Track D, depends on A + C)
- [ ] Password hashing + login/register flow
- [ ] TLS via `SSLEngine` wrapped around existing channels
- [ ] Rate limiting on failed logins
- [ ] Input length validation everywhere (see `05_SECURITY.md`)
- **Exit criteria:** a packet capture on the wire shows no readable message
  content; stored passwords are unrecoverable hashes

### Phase 4 — Features (Track A, now unblocked)
- [ ] Rooms: `/join /leave /create /rooms`
- [ ] Presence broadcast (online/offline)
- [ ] Admin commands: `/kick /mute`
- **Exit criteria:** multi-room chat with admin moderation works end-to-end

### Phase 5 — UX polish (Track E)
- [ ] Terminal colors per username (ANSI codes)
- [ ] Command history/autocomplete
- [ ] Reconnect-on-drop
- [ ] Heartbeat ping/pong (protocol already defines this — implement it)

### Phase 6 — Stretch: E2E encrypted DMs (optional, Track D)
- [ ] RSA keypair generation per client
- [ ] Public key exchange via server
- [ ] AES session key derivation for `/pm`

### Phase 7 — Docs & testing (all hands, ongoing + dedicated pass at end)
- [ ] JUnit tests for `common/` (protocol parsing, crypto helpers)
- [ ] Manual test checklist for networking (documented, not just "we tried it")
- [ ] Update this doc set to match what was actually built (docs drift —
      catch it before submission/demo, not after)
- [ ] Architecture diagram double-checked against final code

## Dependency graph

```
Phase 0 (setup)
   ↓
Phase 1 (networking + protocol)
   ↓
Phase 2 (persistence) ─────┐
   ↓                       │
Phase 3 (security) ←───────┘
   ↓
Phase 4 (features)
   ↓
Phase 5 (UX polish) ──── Phase 6 (stretch: E2E) [optional, parallel]
   ↓
Phase 7 (docs & testing) — really ongoing throughout, formalized at the end
```

## Weekly sync suggestion

Short standup covering: what track you're on, what's blocking you, and
whether your track's "exit criteria" above are met yet. Exit criteria exist
specifically so "done" isn't a feeling — it's a checklist.
