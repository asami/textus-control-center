# Textus Security Operations

status=initial
updated_at=2026-10-14
canonical_architecture=goldenport-cncf/docs/design/textus-security-architecture.md

Textus Control Center is the system-level operational presentation for
security-relevant Textus signals. CNCF and individual subsystems remain the
owners of runtime mechanisms and authoritative operational data.

Control Center should correlate normal operational telemetry with
security-relevant events: authorization denials/anomalies, unexpected service
or subsystem failures, repeated failed operations, credential/authority
revocation events when exposed, runtime replacement/recovery, and relevant
infrastructure observations from OpenTelemetry and managed services.

The objective is diagnosis, containment and recovery, not a second security
policy engine. Control Center must not reimplement CNCF authorization or infer
permission from dashboards.

Security views must avoid becoming a sensitive-data repository. Tokens,
credentials and unnecessary personal/business payloads are redacted or omitted.

A suspected compromised runtime should be representable as untrusted,
isolated/replaced, and recovered from authoritative state. Future work should
define a Textus security-event taxonomy, incident timeline and subsystem
containment/recovery operations.


## Availability / replaceable-runtime relationship

CNCF's canonical Subsystem Runtime Architecture and Availability Architecture
own Active/Standby, readiness, rotation, drain and takeover semantics. Control
Center projects those semantics and uses the same ordinary replacement route for
security containment.

A recent successful real Active rotation is stronger operational evidence than
an indefinitely idle Standby health check. Security views should therefore show
rotation/takeover history and readiness, while avoiding any claim that rotation
proves persistent state or credentials uncompromised.

Standby mutation authority is never inferred from availability. Promotion is an
authorized CNCF transition; read-only/cache service remains separately bounded.
