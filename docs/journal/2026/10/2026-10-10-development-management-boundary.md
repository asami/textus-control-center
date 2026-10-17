# Development management boundary

Date: 2026-10-10
Status: architecture clarification

Textus Control Center remains an operational management subsystem.

Because Control Center is intended to manage Textus and business-system runtime operation, development-specific concepts such as GitHub Projects, Issues, Pull Requests, AI coding/review queues, and software-development triage do not belong in its core management model.

Textus Development Center is the separate Human-in-the-Loop software-development management surface. Its initial system of record is GitHub Projects/Issues/Pull Requests.

The two products may share CNCF/Textus application infrastructure, Display/UI mechanisms, authorization patterns and Human-in-the-Loop interaction patterns. Shared infrastructure does not imply a shared management domain.

Textus Orchestrator may remain shared across both operational and development execution because participant/capability orchestration is a cross-domain infrastructure concern.
