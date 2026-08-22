# Backend (Future)

Milestone 1 works offline with local persistence. A future backend will synchronise curated metadata, the emulator registry, builds, GPU drivers, configurations, observations, benchmark results, evidence and recommendation metadata.

## Proposed low-cost architecture

- A typed HTTP/OpenAPI contract as the public protocol (the client is native Kotlin, so a TypeScript-only RPC system must not become the public protocol).
- Serverless functions + a relational store (Postgres) behind it.
- An internal admin/research application may use Next.js, TypeScript, React, Tailwind and Postgres.

## Not built now

User accounts, subscriptions, telemetry upload, release monitoring, and the admin app are all deferred. Repositories are architected so a synchronisation layer can be added without changing domain contracts.
