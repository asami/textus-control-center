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


## Dot adoption experiment

Dot adoption should initially be evaluated for its value as a long-lived Astra-class context manager / architect, not primarily as a coding worker.

### Hypothesis

If one Dot included within the existing ChatGPT Pro subscription can retain and reconstruct enough development context to act as a persistent project member, it may provide substantial value even when implementation remains delegated to Codex or other workers.

The value hypothesis is:

- maintain long-running understanding of project architecture, design decisions, Phase status, and current priorities;
- follow GitHub notes, journal, Phase, and source changes as durable authority;
- reduce repeated human explanation of historical context;
- identify cross-project impacts and previously established constraints;
- turn human design discussions into durable GitHub design/management artifacts;
- supervise development through Control Center / sm-workflow while delegating bounded implementation work;
- use Astra-class broad/deep reasoning where architectural context management benefits from it.

The economic question is therefore not whether Dot replaces Codex. It is whether an Astra-class persistent project/context manager is practically available within the subscription allowance at useful working volume.

### Initial experiment

Use one project as a bounded trial, with sm-workflow as the initial candidate.

Operate the Dot for approximately one week as the project's semantic management agent / architecture-context owner while keeping GitHub and sm-workflow as authorities.

Implementation remains delegated to Codex/other admitted workers. The experiment should avoid judging Dot primarily by coding throughput.

### Evaluation signals

Evaluate at least:

- reduction in repeated context explanation by the human;
- accuracy when recalling or reconstructing prior design decisions from authoritative artifacts;
- ability to detect relevant cross-project impacts;
- consistency between Dot's working understanding and GitHub notes/journal/Phase state;
- quality of design and planning updates committed to durable artifacts;
- ability to maintain useful project continuity across days and separate interactions;
- quality of supervision/escalation through Control Center and sm-workflow;
- actual subscription/deep-work allowance consumed by this operating style.

No single automatic threshold is defined initially. Record qualitative failures as well as measurable usage.

### Adoption decision

If the experiment demonstrates strong context continuity and design understanding at acceptable subscription usage, expand toward project-specific Dots as additional Dot capacity becomes available.

If persistent context quality or allowance economics are insufficient, retain the same Semantic Management Agent boundary and use OpenClaw with on-demand Sol/Astra reasoning instead.

The experiment must not create a Dot dependency in Control Center, sm-workflow, TEAI, or repository authority.
