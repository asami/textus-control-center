# Textus AI Operations Architecture

This document records the current component-placement view for Textus AI operations.

## Artifacts

- `textus-ai-operations-architecture.svg` — semantic reference diagram. Use this as the authoritative source for component relationships and Message Flow semantics.
- `textus-ai-operations-architecture-infographic.png` — explanatory infographic. It is an effort-target visualization and may simplify Message Flow notation, but it should preserve the component relationship set.

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
