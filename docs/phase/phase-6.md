# Phase 6: Integrated Admission Console

Status: planned
Planned: 2026-10-05
Depends on: Phase 5
Upstream: goldenport-cncf Phase 102 Integrated Admission Management

## Goal

Make Textus Control Center the integrated Human-in-the-Loop admission surface for development and knowledge-management candidates while preserving each provider's authority.

The initial vertical slice consumes CNCF Phase 102 and presents a unified Admission Inbox for:

- sm-workflow pending admissions;
- GitHub Phase/Plan Pull Requests;
- GitHub-managed BoK editorial/Semantic-GC Pull Requests.

## CNCF driven-development boundary

Phase 6 is the initial driver for goldenport-cncf Phase 102.

CNCF Phase 102 remains CNCF-owned and is developed in a dedicated CNCF branch/worktree for this Control Center effort. Starting Phase 6 MUST NOT select Phase 102 as a global CNCF active phase, reuse another project's CNCF worktree, or block independent consumer-driven CNCF Phase 100/101 work merely because their phase numbers are adjacent.

Control Center may prepare consumer-side projections and UI work while the CNCF contract is being implemented, but it MUST consume the accepted CNCF Phase 102 API rather than create a private duplicate Admission model.

When CNCF main changes are incorporated into the dedicated Phase 102 worktree, validate CNCF according to its full-test policy before validating Control Center against that dependency baseline.

## Initial vertical slice

```text
Admission Provider
   -> CNCF Phase 102 projection
   -> Control Center Admission Inbox
   -> Human reviews evidence/context
   -> provider-supported typed action
   -> authoritative provider
   -> resulting status/event
   -> Control Center projection refresh
```

For GitHub Pull Requests, GitHub remains the candidate/review/merge authority. For sm-workflow, its Workflow/Candidate-Admission state remains authoritative.

## Scope

1. Consume the CNCF Phase 102 AdmissionItem/query/action contract.
2. Add an operation-backed Admission Inbox list/detail projection.
3. Integrate the sm-workflow admission provider.
4. Integrate the GitHub Pull Request provider for Phase/Plan candidates.
5. Support the same GitHub provider path for Git-managed BoK editorial candidates.
6. Show bounded evidence/provenance/authority references without copying complete external review stores.
7. Expose only provider-supported authorized actions and reject stale decisions.
8. Journal/observe admission lifecycle changes.
9. Provide Slack notification/deep-link integration for pending human admissions; direct Slack decision submission may be a later slice if needed.
10. Keep Web/Slack/mobile/watch presentation replaceable over the same typed model.

## Non-goals

- Reimplementing GitHub Pull Requests.
- Moving sm-workflow Workflow authority into Control Center.
- Giving Dot/OpenClaw admission authority.
- Semantic Management Agent integration; that follows after the Admission Console boundary is stable.
- Autonomous operation-to-development feedback changes.
