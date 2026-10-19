import AppKit
import SwiftUI

@main
@MainActor
struct MinimalMenuBarApplication: App {
    @NSApplicationDelegateAdaptor(MinimalApplicationLifecycle.self) private var lifecycle

    init() {
#if CC5_UI_DIAGNOSTICS
        PresentationDiagnostics.record(.applicationInitialized)
#endif
    }

    var body: some Scene {
        MenuBarExtra {
            VStack {
                Text("CC5 minimal content")
                Button("Quit") { NSApplication.shared.terminate(nil) }
            }
            .padding()
            .frame(width: 410)
#if CC5_UI_DIAGNOSTICS
            .onAppear { PresentationDiagnostics.record(.contentAppeared) }
            .onDisappear { PresentationDiagnostics.record(.contentDisappeared) }
#endif
        } label: {
            Label {
                Text("CC5 Minimal")
            } icon: {
                Image(systemName: "server.rack")
            }
        }
        .menuBarExtraStyle(.window)
    }
}

@MainActor
private final class MinimalApplicationLifecycle: NSObject, NSApplicationDelegate {
#if CC5_UI_DIAGNOSTICS
    func applicationWillFinishLaunching(_ notification: Notification) {
        PresentationDiagnostics.record(.willFinishLaunching)
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        PresentationDiagnostics.record(.didFinishLaunching)
    }

    func applicationDidBecomeActive(_ notification: Notification) {
        PresentationDiagnostics.record(.didBecomeActive)
    }

    func applicationDidResignActive(_ notification: Notification) {
        PresentationDiagnostics.record(.didResignActive)
    }

    func applicationWillTerminate(_ notification: Notification) {
        PresentationDiagnostics.record(.willTerminate)
    }
#endif
}
