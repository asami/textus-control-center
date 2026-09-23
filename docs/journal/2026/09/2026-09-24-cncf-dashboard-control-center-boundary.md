# CNCF Dashboard and Control Center Boundary

Date: 2026-09-24

CNCF Dashboard/Admin and Textus Control Center have complementary roles. CNCF presents basic runtime mechanism information and administration. Control Center provides the richer Textus-system operational view.

Service Bus makes this boundary particularly useful: CNCF can show bus health, journal/subscription/delivery basics and recent events, while Control Center can use authoritative journal records to present sm-workflow progress, cross-application/subsystem timelines and incident context. Control Center additionally integrates resources outside CNCF such as PostgreSQL ops/dev, OpenTelemetry, OpenClaw and AI runtimes.

Rule of thumb: CNCF UI shows the mechanism; Control Center shows what is happening in the system.
