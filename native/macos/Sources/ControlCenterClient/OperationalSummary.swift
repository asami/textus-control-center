import Foundation

/// The server owns every status, health value, and lease count in this projection.
public struct OperationalSummary: Codable, Equatable, Sendable {
    public let protocolVersion: Int
    public let observedAt: String
    public let componentId: String
    public let instanceId: String
    public let controlCenter: ControlCenter
    public let overallHealth: OverallHealth
    public let runtimes: Runtimes
    public let dashboardUrl: String
    public let refreshAfterSeconds: Int

    public struct TextMetadata: Codable, Equatable, Sendable {
        public let state: String
        public let value: String?

        public init(from decoder: Decoder) throws {
            let fields = try decoder.container(keyedBy: CodingKeys.self)
            state = try fields.decode(String.self, forKey: .state)
            value = try fields.decodeIfPresent(String.self, forKey: .value)
            guard (state == "available" && value?.isEmpty == false)
                || (state == "unavailable" && !fields.contains(.value)) else {
                throw SummaryFailure.contract("Inconsistent text metadata")
            }
        }
    }

    public struct UptimeMetadata: Codable, Equatable, Sendable {
        public let state: String
        public let seconds: Int?

        public init(from decoder: Decoder) throws {
            let fields = try decoder.container(keyedBy: CodingKeys.self)
            state = try fields.decode(String.self, forKey: .state)
            seconds = try fields.decodeIfPresent(Int.self, forKey: .seconds)
            guard (state == "available" && seconds.map { $0 >= 0 } == true)
                || (state == "unavailable" && !fields.contains(.seconds)) else {
                throw SummaryFailure.contract("Inconsistent uptime metadata")
            }
        }
    }

    public struct ControlCenter: Codable, Equatable, Sendable {
        public let status: String
        public let version: TextMetadata
        public let uptime: UptimeMetadata
        public let runtimeVersion: TextMetadata
    }

    public struct OverallHealth: Codable, Equatable, Sendable {
        public let status: String
        public let scope: String
        public let checks: [HealthCheck]
    }

    public struct HealthCheck: Codable, Equatable, Sendable {
        public let name: String
        public let status: String
    }

    public struct Runtimes: Codable, Equatable, Sendable {
        public let total: Int
        public let returned: Int
        public let truncated: Bool
        public let starting: Int
        public let running: Int
        public let stopped: Int
        public let stale: Int
        public let health: String
        public let data: [RuntimeEntry]
    }

    public struct RuntimeEntry: Codable, Equatable, Sendable {
        public let instanceId: String
        public let target: String
        public let artifactId: String?
        public let subsystemName: String?
        public let subsystemVersion: String?
        public let runtimeVersion: String?
        public let launcherState: String
        public let status: String
    }

    public init(from decoder: Decoder) throws {
        let fields = try decoder.container(keyedBy: CodingKeys.self)
        protocolVersion = try fields.decode(Int.self, forKey: .protocolVersion)
        guard protocolVersion == 1 else {
            throw SummaryFailure.contract("Unsupported Summary protocol: \(protocolVersion)")
        }
        observedAt = try fields.decode(String.self, forKey: .observedAt)
        componentId = try fields.decode(String.self, forKey: .componentId)
        instanceId = try fields.decode(String.self, forKey: .instanceId)
        controlCenter = try fields.decode(ControlCenter.self, forKey: .controlCenter)
        overallHealth = try fields.decode(OverallHealth.self, forKey: .overallHealth)
        runtimes = try fields.decode(Runtimes.self, forKey: .runtimes)
        dashboardUrl = try fields.decode(String.self, forKey: .dashboardUrl)
        refreshAfterSeconds = try fields.decode(Int.self, forKey: .refreshAfterSeconds)

        let clock = ISO8601DateFormatter()
        let fractionalClock = ISO8601DateFormatter()
        fractionalClock.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let counts = [runtimes.starting, runtimes.running, runtimes.stopped, runtimes.stale]
        guard clock.date(from: observedAt) != nil || fractionalClock.date(from: observedAt) != nil,
              !componentId.isEmpty, !instanceId.isEmpty,
              overallHealth.scope == "control-center",
              dashboardUrl == ServerOrigin.dashboardPath, refreshAfterSeconds == 15,
              runtimes.total >= 0, runtimes.returned >= 0, runtimes.returned <= 50,
              runtimes.returned <= runtimes.total, runtimes.returned == runtimes.data.count,
              runtimes.truncated == (runtimes.returned < runtimes.total),
              counts.allSatisfy({ $0 >= 0 && $0 <= runtimes.total }),
              counts.reduce(Decimal.zero, { $0 + Decimal($1) }) == Decimal(runtimes.total),
              runtimes.health == "unavailable" else {
            throw SummaryFailure.contract("Malformed or inconsistent Summary metadata")
        }
    }
}
