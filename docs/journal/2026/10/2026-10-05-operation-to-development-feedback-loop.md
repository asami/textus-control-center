# Operation-to-Development Feedback Loop

Date: 2026-10-05
Status: architectural direction
Related: goldenport-cncf AI Feedback Architecture, CNCF Phase 102 Integrated Admission Management

Textus Control Center should connect operational evidence to development and knowledge improvement without becoming the authority for either.

The target loop is:

```text
Operation / Usage
  -> Observability / Service Bus / Experiment / AI Audit
  -> Control Center integrated context
  -> semantic interpretation by Dot/OpenClaw/other agent when useful
  -> Candidate
  -> Integrated Admission Management
  -> sm-workflow / GitHub PR / provider-owned Apply
  -> Operation / Usage
```

Control Center's role is to make relevant evidence and current state available, correlate development and operational context, surface resulting candidates/admissions, and preserve provenance links.

Dot or OpenClaw may interpret evidence and formulate proposals but does not obtain mutation authority. Operational observations must not directly become autonomous code, knowledge, configuration or production mutations.

This feedback loop is a central evaluation point for the combined Control Center + semantic management agent architecture: useful operation-derived improvement candidates are more important than raw monitoring volume.
