# AI-Integrated Development and Operations Strategy

status = architectural target
date = 2026-10-08
authority = textus-control-center

## Goal

Textus Control Center provides one integrated control plane for development and operations while using multiple AI capabilities according to their strengths rather than forcing one agent to perform every role.

The target deployment deliberately allows Dot and OpenClaw to run together.

```text
Human
  |
Slack / Web / Mobile / Watch
  |
Textus Control Center
  |
  +-- OpenClaw + Local LLM
  |     continuous operation / scheduling / EAI / low-cost semantic work
  |
  +-- Dot + Astra-class reasoning
  |     research / architecture / specification / broad semantic review
  |
  +-- sm-workflow
  |     deterministic development orchestration
  |        |
  |        +-- Codex execution contexts
  |              implementation / engineering review
  |
  +-- Integrated Admission
        Human-in-the-Loop / provider authority
```

## Capability placement

### OpenClaw

OpenClaw is primarily a continuously available operational/EAI agent.

The default economic model is OpenClaw backed by a local LLM where practical. Suitable work includes scheduling, event-driven handling, information collection/classification, routine external-service interaction, bounded non-deterministic EAI work and low-cost overnight operation.

OpenClaw is not the normal software-programming worker. In particular, the default architecture does not route programming through OpenClaw merely to invoke Codex behind it. If software-engineering work is required, OpenClaw should submit/trigger the appropriate Control Center/sm-workflow capability and let sm-workflow bind an admitted Codex worker.

OpenClaw may also implement the Semantic Management Agent role when Dot is unavailable. That replaceability does not imply that OpenClaw should perform Codex's implementation role itself.

### Dot

Dot is the preferred high-value semantic management candidate when its persistent context and Astra-class reasoning are economically useful.

Suitable work includes:

- cross-repository and cross-source research;
- architecture and specification work;
- Phase/Plan review and candidate Pull Requests;
- broad semantic review;
- project-context continuity;
- Google Drive/GitHub/Slack source synthesis;
- BoK/knowledge editorial or semantic-GC candidate generation;
- interpretation of operational feedback into proposed changes.

Dot is not an authority. Durable design/knowledge remains in repositories and other authoritative stores; Workflow/Admission state remains in the owning systems.

### Codex

Codex is the software-engineering execution environment for implementation, code-oriented investigation, tests, repair and independent engineering review.

A Codex chat/session may bind to sm-workflow as a worker execution context. Work should then be assigned through typed sm-workflow WorkOrder/Continuation/Result/Evidence contracts rather than requiring an outer agent or human to manually reproduce workflow state in every chat.

The Codex execution context is replaceable. sm-workflow remains the development-process authority.

### Deterministic runtime

CNCF Workflow/StateMachine, sm-workflow, TEAI and other deterministic services retain control-flow, durable-state and provider/action authority appropriate to their domains.

AI agents should not reimplement deterministic scheduling, workflow state, admission state or integration state merely because they can reason about it.

## Human interface separation

Slack, Web, Mobile and Watch are Human Interaction Surfaces, not agent identities.

A Slack request may route to OpenClaw, Dot, sm-workflow/Codex, or an Admission provider according to the requested capability. Do not equate "Slack bot" with OpenClaw.

Examples:

- implementation request -> sm-workflow -> Codex;
- routine operational/EAI request -> TEAI/OpenClaw/local LLM;
- architecture/specification investigation -> Dot/Astra;
- approval -> Integrated Admission -> owning provider.

## Routing principle

Route by semantic capability and responsibility, not by a fixed chain of agents.

Preferred default:

- cheap/continuous operational intelligence -> OpenClaw + local LLM;
- broad/deep semantic context and coordination -> Dot/Astra;
- software engineering -> sm-workflow + Codex;
- deterministic processing -> CNCF/TEAI/workflows;
- authority-sensitive change -> Candidate/Admission, with Human-in-the-Loop when required.

Dot and OpenClaw may exchange work indirectly through Control Center capabilities/events, but must not form a hidden authoritative workflow between themselves.

## Feedback loop

The architecture supports:

```text
Operation / Usage
  -> Evidence
  -> OpenClaw routine processing and/or Dot semantic interpretation
  -> Candidate
  -> Integrated Admission
  -> sm-workflow / TEAI / Git provider
  -> Apply
  -> Operation / Usage
```

The same loop applies to software development, operational improvement and knowledge maintenance.

## Replaceability

Dot, OpenClaw, local LLMs and commercial reasoning models are replaceable capability providers.

No canonical project, workflow, admission or operational state exists only in agent memory. A replacement provider reconstructs context from authoritative repositories, Control Center projections and owning application state.

## Development direction

- Phase 6 establishes Integrated Admission Console / Human-in-the-Loop.
- The following Semantic Management Agent integration should support Dot and OpenClaw without making either mandatory.
- Feedback/routing development should then connect operational evidence to candidate generation and admission.
- Codex/sm-workflow integration should favor worker binding and typed WorkOrder exchange over agent-to-agent prompt relaying.

This target architecture is the long-term direction; individual capabilities may be introduced incrementally as upstream CNCF and consumer phases become available.
