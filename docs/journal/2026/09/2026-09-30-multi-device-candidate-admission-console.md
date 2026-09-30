# Multi-device Candidate-Admission Console

Date: 2026-09-30
Status: architectural direction / reference scenario

## Direction

Textus Control Center should treat Candidate-Admission as a multi-device human authority surface rather than only as a desktop dashboard function.

The same AdmissionCandidate, Display Model, and Action Protocol should support device-appropriate realization.

- Watch: confirmation surface. Optimize for Confirm and Defer/Review.
- Smartphone: compact review and admission.
- Fold/tablet: Admission Inbox + Candidate Detail + Evidence/Diff.
- Desktop/Web: full analysis, editing, Model-up inspection, and operational control.

Pixel Watch / Wear OS is the initial Watch target because the Android/Pixel environment is suitable for rapid proving. Apple Watch/watchOS remains an important later product target and must be supported through the same protocol rather than a separate admission model.

## First vertical slice

sm-workflow
-> AdmissionRequired
-> Control Center / notification delivery
-> Pixel Watch
-> Confirm
-> Admission Action Protocol
-> sm-workflow continuation/close
-> resulting event visible in Control Center

The first proof may rely on Android notification bridging and notification actions. A full Wear OS application is not required to prove the protocol.

## Interaction principle

Watch confirmation is intentionally not full review. Candidates suitable for Watch should already have enough automated/review evidence that the remaining human responsibility is primarily authority confirmation.

If the user feels uncertainty, Review moves the candidate to a richer Smartphone/Fold/Desktop surface. Reject/edit/evidence-heavy decisions need not be optimized for Watch.

This minimizes human-in-the-loop friction while retaining explicit human authority for Candidate-Admission.
