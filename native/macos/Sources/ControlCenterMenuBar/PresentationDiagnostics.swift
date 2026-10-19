#if CC5_UI_DIAGNOSTICS
import AppKit
import Foundation
import OSLog

@MainActor
enum PresentationEvent: String {
    case entry = "entry"
    case applicationDispatch = "applicationDispatch"
    case applicationInitialized = "applicationInitialized"
    case willFinishLaunching = "willFinishLaunching"
    case didFinishLaunching = "didFinishLaunching"
    case didBecomeActive = "didBecomeActive"
    case didResignActive = "didResignActive"
    case contentAppeared = "contentAppeared"
    case contentDisappeared = "contentDisappeared"
    case willTerminate = "willTerminate"
}

@MainActor
enum PresentationDiagnostics {
    private static var emittedCount = 0
    private static let logger = Logger(
        subsystem: "org.simplemodeling.textus.controlcenter.menubar",
        category: "cc5-presentation"
    )

    static func record(_ event: PresentationEvent) {
        // Bound observations to 64 records per process; subsequent calls are ignored.
        // OSLog may drop records, so absence and asynchronous ordering prove no cause.
        guard emittedCount < 64 else { return }
        emittedCount += 1
#if CC5_MINIMAL_REPRO
        let role = "minimal"
#else
        let role = "production"
#endif
        // NSApp reads the optional existing application without creating one.
        let application: NSApplication? = NSApp
        let applicationExists = application != nil
        let activationPolicy = application.map { String($0.activationPolicy().rawValue) } ?? "unknown"
        let isActive = application.map { String($0.isActive) } ?? "unknown"
        let totalWindows = application.map { String($0.windows.count) } ?? "unknown"
        let visibleWindows = application.map { String($0.windows.filter { $0.isVisible }.count) } ?? "unknown"
        logger.notice("event=\(event.rawValue, privacy: .public) role=\(role, privacy: .public) pid=\(ProcessInfo.processInfo.processIdentifier, privacy: .public) mainThread=\(Thread.isMainThread, privacy: .public) nsAppExists=\(applicationExists, privacy: .public) activationPolicy=\(activationPolicy, privacy: .public) isActive=\(isActive, privacy: .public) totalWindows=\(totalWindows, privacy: .public) visibleWindows=\(visibleWindows, privacy: .public)")
    }
}
#endif
