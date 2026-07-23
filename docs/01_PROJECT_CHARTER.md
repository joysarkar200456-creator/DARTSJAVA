# Project Charter

## Vision

A terminal-based chat system, written in Java, that a stranger could clone,
compile with `javac`, and run — on a LAN or over the open internet — and get
a secure, persistent, multi-room chat experience. No GUI. No build tool
dependency. Every design decision should be explainable in a report or a
viva: nothing "just works," everything is understood by the team.

## Problem statement

The original DARTS (C) proved the core networking model but exposed real
gaps: memory-unsafe input handling, no persistence, no encryption, no
concept of rooms, and single-machine/LAN-only usability. This project is not
a patch on that codebase — it's a redesign in Java that treats those gaps as
first-class requirements from day one.

## Goals (in priority order)

1. **Correct, non-blocking networking** — one server thread (via `Selector`)
   handles many clients without the safety pitfalls of raw C sockets.
2. **Works beyond LAN** — a client on a different network can connect to a
   publicly reachable server.
3. **Persistent** — users and message history survive a server restart.
4. **Secure** — traffic is encrypted (TLS) and passwords are never stored
   in plaintext.
5. **Usable** — rooms/channels, presence, and a terminal UX that doesn't
   feel like a class demo.

## Non-goals (explicitly out of scope for v1)

- No graphical interface, ever — this is a terminal tool by design.
- No NAT traversal / hole-punching — reachability is a deployment concern
  (port forwarding or cloud hosting), not something the app solves itself.
- No mobile clients.
- End-to-end encrypted DMs (on top of TLS) are a **stretch goal**, not a
  launch requirement — see Phase 6 in the roadmap.

## Success criteria

- Two clients on different physical networks can chat through a
  publicly-hosted server instance.
- Server can be killed and restarted without losing user accounts or room
  history.
- A packet sniffer on the network between client and server cannot read
  message contents.
- Passwords are unrecoverable from the database even with full DB access
  (i.e., properly salted + hashed, not encrypted-and-reversible).
- Any of the 5 team members can read this doc set and independently start
  contributing to their assigned module without a verbal walkthrough.

## Stakeholders

Team of 5 (see README for names). No external stakeholders — this is a
student/portfolio project, which means documentation quality and code
clarity are part of the grade/value, not just the running artifact.
