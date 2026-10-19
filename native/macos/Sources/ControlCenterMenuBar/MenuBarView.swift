import AppKit
import ControlCenterClient
import SwiftUI

@MainActor
struct MenuBarView: View {
    @ObservedObject var monitor: SummaryMonitor
    let origin: ServerOrigin?
    @State private var dashboardFeedback: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Textus Control Center").font(.headline)
            if let origin {
                Text(monitor.connection.text).textSelection(.enabled)
                Text("Last observation: \(monitor.observedAt ?? "unavailable")")
                    .font(.caption).textSelection(.enabled)
                if let summary = monitor.summary {
                    Text("Summary").font(.subheadline)
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            summaryView(summary)
                        }.frame(maxWidth: .infinity, alignment: .leading)
                    }.frame(height: 320)
                } else {
                    Text("No successful observation yet.")
                }
                HStack {
                    Button("Refresh") { Task { await monitor.refresh() } }
                        .disabled(monitor.isRefreshing)
                    Button("Open Dashboard") {
                        do {
                            let url = try origin.dashboardURL(ServerOrigin.dashboardPath)
                            if NSWorkspace.shared.open(url) {
                                dashboardFeedback = "Dashboard opening requested: \(url.absoluteString)"
                            } else {
                                dashboardFeedback = "Could not open Dashboard in the default browser: \(url.absoluteString)"
                            }
                        } catch {
                            dashboardFeedback = "Could not resolve Dashboard URL."
                        }
                    }
                }
                if let dashboardFeedback {
                    Text(dashboardFeedback).font(.caption).textSelection(.enabled)
                }
            } else {
                Text("Set the local server origin by launching this app with --server-url http://127.0.0.1:PORT. Replace PORT with your configured explicit port.")
                    .textSelection(.enabled)
            }
            Divider()
            Button("Quit") {
                monitor.stop()
                NSApplication.shared.terminate(nil)
            }
        }
        .padding()
        .frame(width: 410)
#if CC5_UI_DIAGNOSTICS
        .onAppear { PresentationDiagnostics.record(.contentAppeared) }
        .onDisappear { PresentationDiagnostics.record(.contentDisappeared) }
#endif
    }

    @ViewBuilder
    private func summaryView(_ summary: OperationalSummary) -> some View {
        Text("Control Center: \(summary.controlCenter.status)")
        Text("Version: \(summary.controlCenter.version.value ?? "unavailable")")
        Text("Uptime: \(summary.controlCenter.uptime.seconds.map { "\($0) seconds" } ?? "unavailable")")
        Text("Runtime version: \(summary.controlCenter.runtimeVersion.value ?? "unavailable")")
        Text("Health (\(summary.overallHealth.scope)): \(summary.overallHealth.status)")
        ForEach(Array(summary.overallHealth.checks.enumerated()), id: \.offset) { _, check in
            Text("\(check.name): \(check.status)").font(.caption)
        }
        Divider()
        Text("Registered runtimes: \(summary.runtimes.total) total; \(summary.runtimes.returned) returned")
        Text("Truncated: \(summary.runtimes.truncated ? "yes" : "no")")
        Text("Starting \(summary.runtimes.starting) · Running \(summary.runtimes.running) · Stopped \(summary.runtimes.stopped) · Stale \(summary.runtimes.stale)")
        Text("Remote runtime health: \(summary.runtimes.health)")
        ForEach(Array(summary.runtimes.data.enumerated()), id: \.offset) { _, entry in
            VStack(alignment: .leading, spacing: 3) {
                Text(entry.target).font(.subheadline)
                Text("Instance: \(entry.instanceId)")
                Text("Lease: \(entry.status); launcher: \(entry.launcherState)")
                Text("Artifact: \(entry.artifactId ?? "unavailable")")
                Text("Subsystem: \(entry.subsystemName ?? "unavailable") \(entry.subsystemVersion ?? "unavailable")")
                Text("Runtime: \(entry.runtimeVersion ?? "unavailable")")
            }.font(.caption).textSelection(.enabled)
            Divider()
        }
    }
}
