# Textus AI Operations Architecture

This document records the current component-placement view for Textus AI operations.

## Knowledge Lake Material

The non-Git assets for this architecture are managed as a TKL Material Package:

```text
textus:material:simplemodeling.org:components/textus-control-center/journal/2026/10/2026-10-09-ai-operations-architecture
```

The Material contains the semantic reference diagram and the explanatory infographic. The Material ID is the canonical cross-system reference; Google Drive URLs and folder IDs are provider-specific resolver details and are not recorded here.

The repository keeps the version-controlled architecture model and documentation. The Knowledge Lake Material keeps the corresponding non-Git/derived assets and self-describing provenance.

## Current boundaries

- Human uses Control Center and Slack as the primary interaction surfaces.
- Control Center manages and observes the provided runtime environment.
- textus-orchestrator focuses on work allocation, tracking and coordination.
- Dots uses Astra for high-value planning, design and continuous review.
- OpenClaw primarily uses Local LLMs for low-cost continuous execution and local/external integration.
- TKL and TEAI are controlled by OpenClaw.
- sm-workflow is dedicated to the development workflow and communicates only with textus-orchestrator and Codex.
- sm-workflow does not use Knowledge Lake, TKL or TEAI.
- There is no direct textus-orchestrator–Codex relationship.
- sm-workflow–Codex and orchestrator–agent delegation use Continuation/IoC interaction semantics.

Message Flow grammar is owned by Cozy/CML. The SVG reference should be used when infographic notation is ambiguous.
