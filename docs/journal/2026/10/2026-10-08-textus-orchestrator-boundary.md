# Textus Orchestrator Boundary

Date: 2026-10-08
Status: architecture alignment
Authority: textus-orchestrator architecture

The cross-agent/participant routing responsibility is moved out of Textus Control Center into the dedicated Textus Orchestrator subsystem.

Control Center remains the integrated management/presentation plane:

- operational and development status;
- Admission Inbox;
- Human interaction through Web/Slack and future mobile/watch surfaces;
- observation/control projections.

Textus Orchestrator owns participant/capability routing across Human, Dot/Astra, OpenClaw/local LLM, sm-workflow and TEAI.

For Human-in-the-Loop, Control Center presents the pending typed request/Admission and returns the user's selected typed action. Orchestrator handles participant routing/correlation; the owning Workflow/Admission provider remains authority.

The existing AI-integrated development/operations strategy remains valid, but its orchestration role is implemented by Textus Orchestrator rather than by adding cross-agent routing logic directly to Control Center.
