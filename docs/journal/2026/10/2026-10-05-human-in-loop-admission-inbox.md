# Human-in-the-Loop Admission Inbox

Date: 2026-10-05
Status: consumer design principle
Related: CNCF Phase 102 Integrated Admission Management

Textus Control Center's Admission Inbox is the common Human-in-the-Loop surface across development, knowledge and operations.

The Inbox does not own application Workflow semantics. It consumes provider-neutral Admission items/actions, presents the evidence/context needed for a human decision, and submits the selected typed action back to the owning provider.

Slack, Web, mobile and Watch are replaceable interaction/presentation adapters over the same model. Adding a Watch approval UI, for example, must not require changing the underlying Workflow or admission semantics.

Continuation remains below this surface as the runtime IoC mechanism used by an owning Workflow when an external result is required. Control Center should not reproduce or infer the Workflow's internal post-decision state machine.

This allows Human-in-the-Loop participation to be added or removed inside application workflows while the Control Center integration remains stable.
