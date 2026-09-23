# sm-workflow Remote Development Monitoring

Date: 2026-09-24

Decision: development monitoring is integrated into Control Center's development-CAR presentation, while sm-workflow remains a separate development control plane.

Control Center uses sm-workflow current views plus CNCF Service Bus authoritative journal events to show phase/action progress, failures and approval requests. This supports the target scenario where sm-workflow runs Codex or OpenClaw on a Mac/server while the user is away and monitors progress from smartphone or Apple Watch/Pixel Watch.

Simple approval is supported through Control Center, but the watch does not publish an ApprovalAccepted event directly. It requests an authenticated sm-workflow admission/continuation operation; sm-workflow validates and publishes the resulting authoritative event. Persistent events provide temporal decoupling so producers and clients need not be online simultaneously.
