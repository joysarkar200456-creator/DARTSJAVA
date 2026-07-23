# Security Model

## Scope

This covers what we protect against, what we don't, and exactly how the two
security mechanisms (transport encryption, password storage) work. Read
this before touching `AuthManager.java`, `CryptoUtils.java`, or anything
socket-related on the server.

## Threat model

**In scope (v1 must defend against):**
- Passive network eavesdropping (someone on the same WiFi/LAN, or a
  relay hop on the internet path, reading raw traffic).
- Database compromise not immediately yielding usable plaintext passwords.
- Basic brute-force login attempts.
- Malformed/oversized input crashing the server (the exact class of bug
  that hit the C version).

**Explicitly out of scope for v1 (documented, not ignored):**
- A malicious server operator reading message contents — TLS protects the
  wire, not the server's own memory. Defending against this requires
  end-to-end encryption, which is Phase 6 (stretch), not core.
- NAT traversal / firewall evasion — reachability is a deployment decision,
  not a code responsibility.
- Protection against a compromised client machine (keyloggers, etc.) —
  out of scope for any chat app.

## Transport security: TLS via SSLEngine

Because the server uses a non-blocking `Selector` loop, we can't use the
simple blocking `SSLSocket` wrapper — we use `SSLEngine`, which is Java's
non-blocking TLS API. Practical implications for whoever builds this:

- Each `ClientSession` owns its own `SSLEngine` instance and two extra
  buffers (network-facing and application-facing) alongside the normal
  read/write buffers.
- The handshake is driven by repeatedly calling `wrap()`/`unwrap()` and
  reacting to the returned `HandshakeStatus` — this is the fiddliest part
  of the whole project. Budget real time for it and write a focused unit
  test around just the handshake before integrating it into the full
  session loop.
- Self-signed certificates are fine for this project (it's not going in
  front of the public web) — generate one with `keytool`, document the
  exact command in `scripts/`, and load it via a `KeyStore` at server
  startup.

## Password storage

- **Never** store plaintext or reversibly-encrypted passwords.
- Algorithm: PBKDF2 with HMAC-SHA256 (`javax.crypto`, stdlib, no external
  dependency needed), per-user random salt, minimum 100,000 iterations.
  This is deliberately chosen over plain SHA-256 because it's slow *by
  design* — that's what makes brute-forcing a stolen hash expensive.
- On login: fetch salt + hash for the username, recompute the hash from the
  supplied password + stored salt, compare in constant time
  (`MessageDigest.isEqual`, not `==` or `.equals()`).
- Failed login attempts are logged (`audit_log`) and rate-limited: after 5
  failures for a username within 60 seconds, reject further attempts for
  that username for 60 seconds, independent of source IP.

## Input validation rules (server-side, non-negotiable)

- Every field in an incoming `Message` has a maximum length, enforced
  before it touches any other code — usernames ≤32 chars, message bodies
  ≤2048 chars, room names ≤64 chars. Reject with `MSG_ERROR`, don't crash,
  don't silently truncate.
- The length-prefix on the wire protocol has a sane upper bound (e.g. 64KB)
  — refuse to allocate a buffer for a claimed length beyond that, since an
  attacker can otherwise claim a 2GB payload and exhaust memory.

## Admin privileges

- `is_admin` is a DB flag, set manually (no self-service admin signup).
- Admin-only message types (`MSG_ADMIN_KICK`, etc.) are checked against the
  session's authenticated user on every request — never trust a client-sent
  "I'm an admin" claim.

## Stretch: end-to-end encrypted DMs (Phase 6)

If pursued: each client generates an RSA keypair on first run, publishes
its public key to the server (stored in `users`, new column), and private
messages are encrypted client-side with a per-conversation AES key wrapped
by the recipient's RSA public key. The server then only ever handles
ciphertext for `MSG_PRIVATE` bodies. This is a genuinely separate piece of
work from TLS and should not block the Phase 3 security milestone.
