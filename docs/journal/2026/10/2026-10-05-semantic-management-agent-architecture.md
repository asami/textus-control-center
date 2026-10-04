# Semantic Management Agent Architecture

Date: 2026-10-05
Status: architectural direction

## Decision

Textus Control Center is the integrated control plane for development and operations. A long-lived AI agent may provide semantic project management, architectural context management, prioritization, interpretation, and human escalation, but the agent is not an authority.

The management-agent role is product-independent. OpenAI Dot is an attractive default candidate, while OpenClaw MUST remain able to replace Dot and may also combine management and operational/EAI responsibilities.

Conceptually:

```text
Human
  |
Slack / Mobile / Watch
  |
Textus Control Center
  |
Semantic Management Agent boundary
  +-- Dot adapter
  +-- OpenClaw adapter
  +-- future agent adapters
  |
sm-workflow / TEAI / CNCF / Codex / services / observability
```

## Responsibility split

- Dot: preferred candidate for long-lived project context, architecture/design assistance, planning, cross-project impact interpretation, supervision, and human escalation.
- OpenClaw: operational/EAI agent and, when Dot is absent, a valid implementation of the semantic-management role.
- Textus Control Center: integrated factual view and control surface for machines, repositories, workflows, jobs, agents, services, and observability.
- sm-workflow: development-process and continuation/admission authority.
- TEAI/CNCF: integration/execution authority.
- GitHub repositories: durable development/design knowledge authority.
- Slack: primary near-term human interaction channel; mobile/watch interfaces may use the same decision contracts later.

## Replaceability principle

No canonical state may exist only in Dot or OpenClaw memory. Agent conversation/session/prompt state is implementation detail.

A replacement agent reconstructs its working context from authoritative repositories and Control Center/application state. Switching Dot to OpenClaw MUST NOT require changing Workflow semantics or business/application state.

Control Center should expose semantic operations/events rather than product-specific agent controls. Product-specific model names, sessions, prompts, tool-call formats, credentials, and scheduling remain behind adapters.

## Event-driven operation

Service Bus and application events should wake or inform the management agent when semantic attention is required. Routine deterministic activity remains in deterministic systems.

Representative events include PhaseCompleted, ReviewRejected, HumanDecisionRequired, BuildFailed, ServiceDown, DeploymentCompleted, and other admitted domain/application events.

The agent interprets these events and may propose actions, invoke bounded Control Center operations, or request human judgment. It does not become the durable event store or workflow engine.

## Development and operations loop

The target loop is:

Design -> Development -> Review -> Deployment -> Operation -> Incident -> Design.

Dot/OpenClaw provides semantic coordination across the loop. Control Center provides integrated observation/control, while sm-workflow, TEAI, CNCF, repositories, and observability retain their respective authorities.

This architecture deliberately allows a preferred Dot + OpenClaw deployment while preserving an OpenClaw-only deployment.
