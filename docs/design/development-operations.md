# Development Operations

Control Center integrates development status into the existing view of development CARs without becoming the sm-workflow development-control implementation.

## Roles

- sm-workflow: development control plane and authoritative current development state.
- CNCF Service Bus Journal: authoritative history of selected development/operational events.
- Control Center: system-wide operational plane and human remote interface.
- Codex/OpenClaw: execution providers behind sm-workflow.

## Development CAR projection

For a development CAR, compose CAR identity/version/runtime information with sm-workflow development status such as phase, workflow/action, worker/provider, elapsed time, last significant event and attention/approval state. Detailed workflow manipulation remains in sm-workflow and may be opened from Control Center.

## Remote mobile/watch scenario

Control Center mobile/watch clients monitor journaled sm-workflow events such as lifecycle progress, failure and ApprovalRequested. Smartphone provides richer detail; watches emphasize glanceable status and simple approval.

Monitoring is event/journal based. Human actions are operations, not fabricated result events. An approval flow is:

1. sm-workflow publishes and commits ApprovalRequested.
2. Control Center receives/catches up from the journal and presents it.
3. authenticated user chooses Approve (watch may also offer Later/Open on Phone).
4. Control Center calls the sm-workflow admission/continuation operation.
5. sm-workflow validates current state, authorization/idempotency and applies the decision.
6. sm-workflow publishes ApprovalAccepted/Rejected as the authoritative outcome.

The Service Bus is not exposed directly to the Internet. Remote clients use a secured Control Center Remote API with authentication, TLS, authorization, expiry and duplicate/stale-decision protection. Push notifications are hints; current authoritative state is re-read before action.

Persistent events are the temporal decoupling boundary: sm-workflow does not know about smartphone/watch clients, and clients do not know Codex/OpenClaw internals.
