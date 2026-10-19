import Combine
import Foundation

@MainActor
public final class SummaryMonitor: ObservableObject {
    public enum Connection: Equatable {
        case awaitingObservation
        case connected
        case disconnected(SummaryFailure)

        public var text: String {
            switch self {
            case .awaitingObservation: return "Awaiting observation"
            case .connected: return "Connected"
            case .disconnected(let failure): return failure.localizedDescription
            }
        }
    }

    @Published public private(set) var summary: OperationalSummary?
    @Published public private(set) var connection: Connection = .awaitingObservation
    @Published public private(set) var isRefreshing = false
    public var observedAt: String? { summary?.observedAt }

    private let load: () async throws -> OperationalSummary
    private let wait: (UInt64) async throws -> Void
    private var inFlight: Task<OperationalSummary, Error>?
    private var automatic: Task<Void, Never>?

    public init(load: @escaping () async throws -> OperationalSummary,
                wait: @escaping (UInt64) async throws -> Void = { try await Task.sleep(nanoseconds: $0) }) {
        self.load = load
        self.wait = wait
    }

    /// Concurrent manual/automatic requests share the active refresh boundary.
    public func refresh() async {
        guard !Task.isCancelled else { return }
        if let inFlight {
            // The automatic loop also waits for a manual owner's completion.
            _ = try? await inFlight.value
            return
        }
        let task = Task { try await load() }
        inFlight = task
        isRefreshing = true
        defer {
            inFlight = nil
            isRefreshing = false
        }
        do {
            let received = try await withTaskCancellationHandler {
                try await task.value
            } onCancel: { task.cancel() }
            guard !task.isCancelled, !Task.isCancelled else { return }
            summary = received
            connection = .connected
        } catch {
            guard !task.isCancelled, !Task.isCancelled else { return }
            let failure = (error as? SummaryFailure) ?? .connection(error.localizedDescription)
            connection = .disconnected(failure)
        }
    }

    public func start() {
        guard automatic == nil else { return }
        let activeRequest = inFlight
        automatic = Task { [weak self] in
            var sharedRequest = activeRequest
            while !Task.isCancelled {
                guard let self else { return }
                if let pending = sharedRequest {
                    _ = try? await pending.value
                    sharedRequest = nil
                } else {
                    await self.refresh()
                }
                guard !Task.isCancelled else { return }
                do { try await self.wait(15_000_000_000) }
                catch { return }
            }
        }
    }

    public func stop() {
        automatic?.cancel()
        automatic = nil
        inFlight?.cancel()
    }

    deinit {
        automatic?.cancel()
        inFlight?.cancel()
    }
}
