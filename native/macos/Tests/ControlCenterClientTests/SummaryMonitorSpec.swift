import Foundation
import XCTest
@testable import ControlCenterClient

/// Chapter: observation, disconnection, refresh serialization, and loop ownership.
@MainActor
final class SummaryMonitorSpec: XCTestCase {
    func testSuccessfulRefreshPublishesTheServerObservation() async throws {
        // Given an unobserved monitor and a server-owned Summary.
        let expected = try SummaryFixtures.summary()
        let monitor = SummaryMonitor(load: { expected })
        XCTAssertNil(monitor.summary)
        XCTAssertEqual(monitor.connection, .awaitingObservation)
        // When a refresh succeeds.
        await monitor.refresh()
        // Then the exact Summary and its server clock become connected presentation state.
        XCTAssertEqual(monitor.summary, expected)
        XCTAssertEqual(monitor.observedAt, expected.observedAt)
        XCTAssertEqual(monitor.connection, .connected)
        XCTAssertFalse(monitor.isRefreshing)
    }

    func testFailuresRetainLastSummaryAndObservationWhileConnectionChanges() async throws {
        // Given a successful observation followed by each supported failure category.
        let expected = try SummaryFixtures.summary()
        for failure in [SummaryFailure.connection("offline"), .http(403), .contract("invalid protocol")] {
            var failNext = false
            let monitor = SummaryMonitor {
                if failNext { throw failure }
                return expected
            }
            await monitor.refresh()
            failNext = true
            // When the next refresh fails.
            await monitor.refresh()
            // Then stale server state remains attributable and disconnection is explicit.
            XCTAssertEqual(monitor.summary, expected)
            XCTAssertEqual(monitor.observedAt, expected.observedAt)
            XCTAssertEqual(monitor.connection, .disconnected(failure))
            XCTAssertEqual(monitor.connection.text, failure.localizedDescription)
        }
    }

    func testFirstFailureLeavesObservationUnavailable() async {
        // Given a newly launched monitor without a successful server response.
        let monitor = SummaryMonitor { throw SummaryFailure.connection("offline") }
        // When its first refresh fails.
        await monitor.refresh()
        // Then neither a clock nor a healthy/zero fixture is manufactured.
        XCTAssertNil(monitor.summary)
        XCTAssertNil(monitor.observedAt)
        XCTAssertEqual(monitor.connection, .disconnected(.connection("offline")))
    }

    func testManualRefreshDoesNotOverlapASuspendedRefresh() async throws {
        // Given a real monitor whose client is suspended until explicitly released.
        let gate = ControlledSuspension<OperationalSummary>()
        let expected = try SummaryFixtures.summary()
        let monitor = SummaryMonitor { try await gate.suspend() }
        let first = Task { await monitor.refresh() }
        await gate.waitUntilEntered(1)
        // When another manual refresh arrives before the first client completes.
        let joining = expectation(description: "second refresh joins active request")
        let second = Task {
            joining.fulfill()
            await monitor.refresh()
        }
        await fulfillment(of: [joining], timeout: 2)
        // Then there is one active client invocation and no premature observation.
        XCTAssertEqual(gate.entries, 1)
        XCTAssertTrue(monitor.isRefreshing)
        XCTAssertNil(monitor.summary)
        gate.resume(returning: expected)
        await first.value
        await second.value
        XCTAssertEqual(monitor.summary, expected)
        XCTAssertFalse(monitor.isRefreshing)
    }

    func testAutomaticRefreshBeginsImmediatelyAndWaitsAfterCompletion() async throws {
        // Given a controlled client and controlled completion-relative refresh delay.
        let client = ControlledSuspension<OperationalSummary>()
        let delay = ControlledSuspension<Void>()
        let expected = try SummaryFixtures.summary()
        var intervals: [UInt64] = []
        let monitor = SummaryMonitor(load: { try await client.suspend() }, wait: {
            intervals.append($0)
            try await delay.suspend()
        })
        // When automatic refresh starts and a manual request arrives while it is pending.
        monitor.start()
        monitor.start()
        await client.waitUntilEntered(1)
        let joining = expectation(description: "manual request joins automatic refresh")
        let manual = Task {
            joining.fulfill()
            await monitor.refresh()
        }
        await fulfillment(of: [joining], timeout: 2)
        // Then the client starts immediately once, and no delay begins before completion.
        XCTAssertEqual(client.entries, 1)
        XCTAssertEqual(delay.entries, 0)
        XCTAssertEqual(intervals, [])
        client.resume(returning: expected)
        await manual.value
        await delay.waitUntilEntered(1)
        XCTAssertEqual(intervals, [15_000_000_000])
        XCTAssertEqual(monitor.summary, expected)
        delay.resume(returning: ())
        await client.waitUntilEntered(2)
        XCTAssertEqual(client.entries, 2)
        XCTAssertEqual(delay.entries, 1)
        monitor.stop()
        await client.waitUntilCancelled()
    }

    func testAutomaticLoopJoinsAnAlreadyActiveManualObservation() async throws {
        // Given a manual refresh that already owns the suspended client.
        let client = ControlledSuspension<OperationalSummary>()
        let delay = ControlledSuspension<Void>()
        let expected = try SummaryFixtures.summary()
        let monitor = SummaryMonitor(load: { try await client.suspend() }, wait: { _ in
            try await delay.suspend()
        })
        let manual = Task { await monitor.refresh() }
        await client.waitUntilEntered(1)
        // When automatic refresh begins during that manual observation.
        monitor.start()
        client.resume(returning: expected)
        await manual.value
        await delay.waitUntilEntered(1)
        // Then it waits after the shared completion without issuing a second client call.
        XCTAssertEqual(client.entries, 1)
        XCTAssertEqual(delay.entries, 1)
        XCTAssertEqual(monitor.summary, expected)
        monitor.stop()
        await delay.waitUntilCancelled()
    }

    func testStoppingAutomaticLoopCancelsDelayAndAllowsOneNewOwner() async throws {
        // Given an automatic loop parked after its first successful observation.
        let expected = try SummaryFixtures.summary()
        let delay = ControlledSuspension<Void>()
        var loads = 0
        let monitor = SummaryMonitor(load: {
            loads += 1
            return expected
        }, wait: { _ in try await delay.suspend() })
        monitor.start()
        await delay.waitUntilEntered(1)
        // When termination stops the loop while it owns the delay.
        monitor.stop()
        await delay.waitUntilCancelled()
        // Then that delay is cancelled and a later explicit start has a single new owner.
        XCTAssertEqual(loads, 1)
        XCTAssertEqual(delay.cancellations, 1)
        monitor.start()
        await delay.waitUntilEntered(2)
        XCTAssertEqual(loads, 2)
        monitor.stop()
        await delay.waitUntilCancelled(2)
    }

    func testStoppingAnActiveRefreshCancelsWithoutPublishingALateResult() async throws {
        // Given a previously connected monitor with its next client invocation suspended.
        let expected = try SummaryFixtures.summary()
        let gate = ControlledSuspension<OperationalSummary>()
        var loads = 0
        let monitor = SummaryMonitor {
            loads += 1
            if loads == 1 { return expected }
            return try await gate.suspend()
        }
        await monitor.refresh()
        let pending = Task { await monitor.refresh() }
        await gate.waitUntilEntered(1)
        // When application termination cancels the owned refresh.
        monitor.stop()
        await gate.waitUntilCancelled()
        await pending.value
        // Then cancellation releases refreshing state without rewriting the last observation.
        XCTAssertEqual(gate.cancellations, 1)
        XCTAssertEqual(monitor.summary, expected)
        XCTAssertEqual(monitor.observedAt, expected.observedAt)
        XCTAssertEqual(monitor.connection, .connected)
        XCTAssertFalse(monitor.isRefreshing)
    }

    func testCancellingTheCallerCancelsItsSharedClientInvocation() async {
        // Given a manual refresh suspended inside the actual monitor boundary.
        let gate = ControlledSuspension<OperationalSummary>()
        let monitor = SummaryMonitor { try await gate.suspend() }
        let pending = Task { await monitor.refresh() }
        await gate.waitUntilEntered(1)
        // When the task owning that refresh is cancelled.
        pending.cancel()
        await gate.waitUntilCancelled()
        await pending.value
        // Then cancellation reaches the client without synthesizing an error observation.
        XCTAssertEqual(gate.cancellations, 1)
        XCTAssertNil(monitor.summary)
        XCTAssertEqual(monitor.connection, .awaitingObservation)
        XCTAssertFalse(monitor.isRefreshing)
    }
}

/// A deterministic asynchronous boundary: no wall-clock delay or polling fixtures.
@MainActor
private final class ControlledSuspension<Value> {
    private var continuation: CheckedContinuation<Value, Error>?
    private var entryWaiters: [(Int, CheckedContinuation<Void, Never>)] = []
    private var cancellationWaiters: [(Int, CheckedContinuation<Void, Never>)] = []
    private(set) var entries = 0
    private(set) var cancellations = 0

    func suspend() async throws -> Value {
        try Task.checkCancellation()
        return try await withTaskCancellationHandler {
            try await withCheckedThrowingContinuation { continuation in
                self.continuation = continuation
                entries += 1
                releaseWaiters(&entryWaiters, reached: entries)
            }
        } onCancel: {
            Task { @MainActor in self.cancel() }
        }
    }

    func resume(returning value: Value) {
        let pending = continuation
        continuation = nil
        pending?.resume(returning: value)
    }

    func waitUntilEntered(_ count: Int) async {
        if entries >= count { return }
        await withCheckedContinuation { entryWaiters.append((count, $0)) }
    }

    func waitUntilCancelled(_ count: Int = 1) async {
        if cancellations >= count { return }
        await withCheckedContinuation { cancellationWaiters.append((count, $0)) }
    }

    private func cancel() {
        let pending = continuation
        continuation = nil
        pending?.resume(throwing: CancellationError())
        cancellations += 1
        releaseWaiters(&cancellationWaiters, reached: cancellations)
    }

    private func releaseWaiters(_ waiters: inout [(Int, CheckedContinuation<Void, Never>)], reached: Int) {
        let ready = waiters.filter { $0.0 <= reached }
        waiters.removeAll { $0.0 <= reached }
        for (_, waiter) in ready { waiter.resume() }
    }
}
