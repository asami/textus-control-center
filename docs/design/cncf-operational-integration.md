# CNCF Operational Integration

Textus Control Center is the system-level operations surface above CNCF Runtime administration.

## Boundary

CNCF Dashboard/Admin provides basic, mechanism-oriented information for Entity/View, Job, Workflow/StateMachine and Service Bus, including runtime health and journal/subscription/delivery status.

Control Center consumes CNCF operational APIs/views and authoritative Service Bus journal information and turns them into richer system-oriented presentations:

- cross-application and cross-subsystem status
- meaningful operational timelines
- incident/failed-execution diagnosis
- Job/Workflow development progress such as sm-workflow
- integrated resources outside CNCF, including PostgreSQL ops/dev, OpenTelemetry, OpenClaw and AI runtimes
- Web, macOS, future mobile and wearable presentations

Control Center must not reimplement CNCF runtime administration. It composes and interprets CNCF operational information together with the wider Textus environment.
