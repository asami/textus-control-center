import Foundation
import ServiceManagement

@MainActor
enum LoginIntegration {
    struct Result {
        let text: String
        let enabled: Bool
        let unregistered: Bool
    }

    static func register() throws -> Result {
        try requireBundle()
        try SMAppService.mainApp.register()
        return status()
    }

    static func unregister() async throws -> Result {
        try requireBundle()
        try await SMAppService.mainApp.unregister()
        return status()
    }

    private static func requireBundle() throws {
        guard Bundle.main.bundleURL.pathExtension == "app",
              Bundle.main.bundleIdentifier == "org.simplemodeling.textus.controlcenter.menubar" else {
            throw LoginError.installedBundleRequired
        }
    }

    private static func status() -> Result {
        switch SMAppService.mainApp.status {
        case .enabled: return Result(text: "enabled", enabled: true, unregistered: false)
        case .requiresApproval: return Result(text: "requiresApproval", enabled: false, unregistered: false)
        case .notRegistered: return Result(text: "notRegistered", enabled: false, unregistered: true)
        case .notFound: return Result(text: "notFound", enabled: false, unregistered: false)
        @unknown default: return Result(text: "unknown login service status", enabled: false, unregistered: false)
        }
    }

    private enum LoginError: LocalizedError {
        case installedBundleRequired
        var errorDescription: String? {
            "Run login integration from the installed Textus Control Center.app bundle"
        }
    }
}
