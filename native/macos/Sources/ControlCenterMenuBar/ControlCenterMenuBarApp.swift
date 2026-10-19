import AppKit
import ControlCenterClient
import Foundation
import SwiftUI

@main
struct ControlCenterEntry {
    @MainActor
    static func main() async {
#if CC5_UI_DIAGNOSTICS
        PresentationDiagnostics.record(.entry)
#endif
        do {
            let options = try LaunchOptions(Array(CommandLine.arguments.dropFirst()))
            let defaults = UserDefaults.standard
            let selected: ServerOrigin?
            if let explicit = options.serverURL {
                selected = try ServerOrigin(explicit)
            } else if let saved = defaults.string(forKey: "serverURL") {
                selected = try ServerOrigin(saved)
            } else {
                selected = nil
            }
            if options.mode == .summaryOnce {
                guard let selected else {
                    throw SummaryFailure.contract("--summary-once requires --server-url URL or a retained server origin")
                }
                let receipt = try await SummaryClient(origin: selected).fetch()
                FileHandle.standardOutput.write(receipt.json)
                FileHandle.standardOutput.write(Data("\n".utf8))
                return
            }
            if let selected, options.serverURL != nil {
                defaults.set(selected.url.absoluteString, forKey: "serverURL")
            }
            switch options.mode {
            case .registerLogin:
                let result = try LoginIntegration.register()
                print(result.text)
                if !result.enabled { exit(2) }
            case .unregisterLogin:
                let result = try await LoginIntegration.unregister()
                print(result.text)
                if !result.unregistered { exit(2) }
            case .application:
                MenuBarApplication.selectedOrigin = selected
#if CC5_UI_DIAGNOSTICS
                PresentationDiagnostics.record(.applicationDispatch)
#endif
                MenuBarApplication.main()
            case .summaryOnce: break
            }
        } catch {
            FileHandle.standardError.write(Data("\(error.localizedDescription)\n".utf8))
            exit(1)
        }
    }
}

private struct LaunchOptions {
    enum Mode { case application, summaryOnce, registerLogin, unregisterLogin }
    let mode: Mode
    let serverURL: String?

    init(_ arguments: [String]) throws {
        var selectedMode = Mode.application
        var selectedURL: String?
        var index = 0
        while index < arguments.count {
            switch arguments[index] {
            case "--server-url":
                guard selectedURL == nil, index + 1 < arguments.count,
                      !arguments[index + 1].hasPrefix("--") else {
                    throw SummaryFailure.contract("--server-url requires exactly one URL")
                }
                index += 1
                selectedURL = arguments[index]
            case "--summary-once", "--register-login", "--unregister-login":
                guard selectedMode == .application else {
                    throw SummaryFailure.contract("Choose one command mode only")
                }
                switch arguments[index] {
                case "--summary-once": selectedMode = .summaryOnce
                case "--register-login": selectedMode = .registerLogin
                default: selectedMode = .unregisterLogin
                }
            default:
                throw SummaryFailure.contract("Unknown argument: \(arguments[index])")
            }
            index += 1
        }
        mode = selectedMode
        serverURL = selectedURL
    }
}

@MainActor
private struct MenuBarApplication: App {
    static var selectedOrigin: ServerOrigin?
    @NSApplicationDelegateAdaptor(ApplicationLifecycle.self) private var lifecycle
    @StateObject private var monitor: SummaryMonitor
    private let origin: ServerOrigin?

    init() {
        let origin = Self.selectedOrigin
        self.origin = origin
        let client = origin.map { SummaryClient(origin: $0) }
        let monitor = SummaryMonitor {
            guard let client else { throw SummaryFailure.contract("Configure --server-url URL") }
            return try await client.fetch().summary
        }
        _monitor = StateObject(wrappedValue: monitor)
        ApplicationLifecycle.monitor = monitor
#if CC5_UI_DIAGNOSTICS
        PresentationDiagnostics.record(.applicationInitialized)
#endif
        if origin != nil { monitor.start() }
    }

    var body: some Scene {
        MenuBarExtra {
            MenuBarView(monitor: monitor, origin: origin)
        } label: {
            Label(menuLabel, systemImage: menuIcon)
        }
        .menuBarExtraStyle(.window)
    }

    private var menuLabel: String {
        guard origin != nil else { return "Textus: setup required" }
        switch monitor.connection {
        case .connected: return "Textus: \(monitor.summary?.overallHealth.status ?? "unavailable")"
        case .awaitingObservation: return "Textus: connecting"
        case .disconnected: return "Textus: disconnected"
        }
    }

    private var menuIcon: String {
        switch monitor.connection {
        case .connected: return "server.rack"
        case .awaitingObservation: return "ellipsis.circle"
        case .disconnected: return "network.slash"
        }
    }
}

@MainActor
private final class ApplicationLifecycle: NSObject, NSApplicationDelegate {
    static var monitor: SummaryMonitor?

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
#endif

    func applicationWillTerminate(_ notification: Notification) {
#if CC5_UI_DIAGNOSTICS
        PresentationDiagnostics.record(.willTerminate)
#endif
        Self.monitor?.stop()
    }
}
