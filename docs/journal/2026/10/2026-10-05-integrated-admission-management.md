# Integrated Admission Management

Date: 2026-10-05
Status: consumer direction
Producer: goldenport-cncf Phase 102

Textus Control Center will use the CNCF Integrated Admission Management model as the common logical contract for admission work across development and knowledge-management providers.

The Control Center provides a unified Admission Inbox/Queue while preserving each provider's authority. Initial items include Phase/Plan GitHub Pull Requests (including Dot-generated planning candidates), BoK editorial/Semantic-GC Pull Requests, and sm-workflow Candidate-Admission decisions.

GitHub remains authoritative for Git Pull Requests. sm-workflow remains authoritative for Workflow state. Control Center is an integrated projection/control surface, not a replacement review store.

Slack is the initial human interaction surface; mobile/watch may later expose the same bounded actions. Human interaction resolves to typed provider actions with stale-state/authorization checks. Slack text and agent memory are never authority.

Dot/OpenClaw may discover problems and create candidates but do not gain admission authority. The Admission Inbox must remain usable when the management agent is replaced.
