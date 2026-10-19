import Foundation
import XCTest
@testable import ControlCenterClient

/// Chapter: protocol, selected origin, and the real HTTP client boundary.
final class SummaryContractSpec: XCTestCase {
    func testProtocolOnePreservesServerProjectionAndOmittedMetadata() throws {
        // Given a protocol-one response with unavailable metadata and omitted entry details.
        let data = try SummaryFixtures.data()
        // When the native contract decodes the normal JSON object.
        let summary = try JSONDecoder().decode(OperationalSummary.self, from: data)
        // Then server identity, health, counts, and unavailability remain explicit.
        XCTAssertEqual(summary.protocolVersion, 1)
        XCTAssertEqual(summary.componentId, "control-center")
        XCTAssertEqual(summary.controlCenter.version.state, "unavailable")
        XCTAssertNil(summary.controlCenter.version.value)
        XCTAssertNil(summary.controlCenter.uptime.seconds)
        XCTAssertNil(summary.controlCenter.runtimeVersion.value)
        XCTAssertEqual(summary.overallHealth.status, "ok")
        XCTAssertEqual(summary.runtimes.running, 1)
        XCTAssertNil(summary.runtimes.data.first?.artifactId)
        XCTAssertEqual(summary.runtimes.health, "unavailable")
    }

    func testAvailableMetadataAndUnknownStatusesArePresentedVerbatim() throws {
        // Given available metadata and future status vocabulary supplied by the server.
        let data = try SummaryFixtures.data {
            var control = $0["controlCenter"] as! [String: Any]
            control["version"] = ["state": "available", "value": "0.1.1-SNAPSHOT"]
            control["uptime"] = ["state": "available", "seconds": 0]
            control["runtimeVersion"] = ["state": "available", "value": "runtime-1"]
            control["status"] = "future-control-state"
            $0["controlCenter"] = control
            $0["overallHealth"] = ["status": "future-health", "scope": "control-center", "checks": []]
        }
        // When the projection is decoded.
        let summary = try JSONDecoder().decode(OperationalSummary.self, from: data)
        // Then unknown statuses stay textual and a real zero uptime is available.
        XCTAssertEqual(summary.controlCenter.status, "future-control-state")
        XCTAssertEqual(summary.overallHealth.status, "future-health")
        XCTAssertEqual(summary.controlCenter.version.value, "0.1.1-SNAPSHOT")
        XCTAssertEqual(summary.controlCenter.uptime.seconds, 0)
        XCTAssertEqual(summary.controlCenter.runtimeVersion.value, "runtime-1")
    }

    func testUnsupportedProtocolIsAContractFailure() throws {
        // Given unsupported protocol coordinates.
        for protocolVersion in [0, 2, -1, 100] {
            let data = try SummaryFixtures.data { $0["protocolVersion"] = protocolVersion }
            // When a caller decodes them.
            let result = Result { try JSONDecoder().decode(OperationalSummary.self, from: data) }
            // Then the native caller rejects them rather than guessing compatibility.
            XCTAssertThrowsError(try result.get()) { error in
                guard case SummaryFailure.contract = error else { return XCTFail("Expected contract failure") }
            }
        }
    }

    func testInconsistentMetadataAndCountsAreRejected() throws {
        // Given contradictory metadata, counts, clock, or navigation values.
        let mutations: [(inout [String: Any]) -> Void] = [
            { $0["observedAt"] = "not-a-clock" },
            { $0["dashboardUrl"] = "https://example.com/web/textus-control-center" },
            { $0["refreshAfterSeconds"] = 0 },
            { root in
                var runtimes = root["runtimes"] as! [String: Any]
                runtimes["returned"] = 0
                root["runtimes"] = runtimes
            },
            { root in
                var runtimes = root["runtimes"] as! [String: Any]
                runtimes["truncated"] = true
                root["runtimes"] = runtimes
            },
            { root in
                var control = root["controlCenter"] as! [String: Any]
                control["version"] = ["state": "available"]
                root["controlCenter"] = control
            },
            { root in
                var control = root["controlCenter"] as! [String: Any]
                control["version"] = ["state": "unavailable", "value": "invented"]
                root["controlCenter"] = control
            },
            { root in
                var control = root["controlCenter"] as! [String: Any]
                control["uptime"] = ["state": "available", "seconds": -1]
                root["controlCenter"] = control
            },
            { root in
                var control = root["controlCenter"] as! [String: Any]
                control["runtimeVersion"] = ["state": "unavailable", "value": NSNull()]
                root["controlCenter"] = control
            }
        ]
        for mutate in mutations {
            let data = try SummaryFixtures.data(mutate)
            // When the normal JSON contract is decoded.
            let result = Result { try JSONDecoder().decode(OperationalSummary.self, from: data) }
            // Then malformed or inconsistent metadata cannot become a displayed observation.
            XCTAssertThrowsError(try result.get())
        }
    }

    func testExplicitLoopbackOriginsAndCanonicalRoutes() throws {
        // Given the supported IPv4, IPv6, and localhost development origins.
        for value in ["http://127.0.0.1:8080", "http://[::1]:9090/", "http://localhost:1", "http://localhost:65535/"] {
            // When the selected origin supplies Summary and Dashboard navigation.
            let origin = try ServerOrigin(value)
            let summary = URLComponents(url: origin.summaryURL, resolvingAgainstBaseURL: false)!
            let dashboard = try origin.dashboardURL("/web/textus-control-center")
            // Then the request path/limit are exact and Dashboard keeps the selected authority.
            XCTAssertEqual(summary.path, "/rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary")
            XCTAssertEqual(summary.query, "limit=50")
            XCTAssertEqual(dashboard.host, origin.url.host)
            XCTAssertEqual(dashboard.port, origin.url.port)
            XCTAssertEqual(dashboard.path, "/web/textus-control-center")
        }
    }

    func testOriginInvariantMatrixRejectsImplicitOrForeignAuthority() {
        // Given invalid credentials, ports, paths, queries, fragments, and remote schemes/hosts.
        let values = [
            "http://localhost", "http://localhost:0", "http://localhost:65536", "http://localhost:-1",
            "http://user@localhost:8080", "http://user:secret@localhost:8080", "http://@localhost:8080",
            "http://localhost:8080/api", "http://localhost:8080//", "http://localhost:8080/%2F",
            "http://localhost:8080?limit=50", "http://localhost:8080?", "http://localhost:8080#part",
            "https://localhost:8080", "http://example.com:8080", "http://127.0.0.2:8080",
            "http://localhost.example.com:8080", "http://[::2]:8080", "localhost:8080"
        ]
        for value in values {
            // When the caller selects that origin.
            let result = Result { try ServerOrigin(value) }
            // Then selection fails without a guessed fallback.
            XCTAssertThrowsError(try result.get(), value)
        }
    }

    func testDashboardRejectsEveryNoncanonicalRelativeOrAbsoluteURL() throws {
        // Given an explicitly selected origin and foreign or altered Dashboard links.
        let origin = try ServerOrigin("http://127.0.0.1:8080")
        for value in ["https://example.com", "//example.com/web/textus-control-center", "http://127.0.0.1:8080/web/textus-control-center", "/web/textus-control-center/", "/web/textus-control-center?x=1", "/web/textus-control-center#x", "web/textus-control-center"] {
            // When Dashboard resolution is requested.
            let result = Result { try origin.dashboardURL(value) }
            // Then no alternate navigation target is admitted.
            XCTAssertThrowsError(try result.get(), value)
        }
    }

    func testProductionTransportGetsExactRouteAndReturnsReceivedJSON() async throws {
        // Given the real client with a URLProtocol response at its network boundary.
        let payload = try SummaryFixtures.data()
        let requestSeen = expectation(description: "canonical GET received")
        StubProtocol.handler = { request, protocolClient in
            XCTAssertEqual(request.url?.absoluteString, "http://localhost:8080/rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary?limit=50")
            XCTAssertEqual(request.httpMethod, "GET")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Accept"), "application/json")
            XCTAssertEqual(request.timeoutInterval, 5)
            requestSeen.fulfill()
            protocolClient.respond(status: 200, body: payload)
        }
        let client = try makeClient()
        // When the same fetch used by the app and one-shot diagnostic completes.
        let receipt = try await client.fetch()
        // Then received JSON and decoded server observation agree without a wrapper.
        await fulfillment(of: [requestSeen], timeout: 2)
        XCTAssertEqual(receipt.json, payload)
        XCTAssertEqual(receipt.summary.observedAt, "2026-10-18T00:00:00Z")
        XCTAssertEqual(receipt.summary.runtimes.returned, 1)
    }

    func testHTTPFailuresCannotProduceHealthyObservations() async throws {
        for status in [401, 403, 404, 500] {
            // Given a normal HTTP rejection from the selected endpoint.
            StubProtocol.handler = { _, stub in stub.respond(status: status, body: Data()) }
            let client = try makeClient()
            // When the production client fetches Summary.
            do {
                _ = try await client.fetch()
                XCTFail("Expected HTTP error")
            } catch {
                // Then the exact HTTP status remains visible to the caller.
                XCTAssertEqual(error as? SummaryFailure, .http(status))
            }
        }
    }

    func testRedirectResponseIsRefused() async throws {
        // Given a redirect response toward another endpoint.
        StubProtocol.handler = { _, stub in
            stub.respond(status: 302, body: Data(), headers: ["Location": "http://localhost:9090/other"])
        }
        let client = try makeClient()
        // When Summary encounters the redirect.
        do {
            _ = try await client.fetch()
            XCTFail("Expected redirect refusal")
        } catch {
            // Then it is an HTTP redirect failure rather than a connected observation.
            XCTAssertEqual(error as? SummaryFailure, .redirect)
        }
    }

    func testMalformedWrappedAndOversizedBodiesAreContractFailures() async throws {
        let wrapped = try JSONSerialization.data(withJSONObject: ["result": SummaryFixtures.object()])
        for payload in [Data("not JSON".utf8), wrapped, Data(repeating: 32, count: SummaryClient.maximumResponseBytes + 1)] {
            // Given a successful HTTP status carrying an unusable Summary body.
            StubProtocol.handler = { _, stub in stub.respond(status: 200, body: payload) }
            let client = try makeClient()
            // When the production decoder consumes the bounded response.
            do {
                _ = try await client.fetch()
                XCTFail("Expected contract error")
            } catch {
                // Then neither speculative envelopes nor oversized data become state.
                guard case SummaryFailure.contract = error else { return XCTFail("Expected contract failure, got \(error)") }
            }
        }
    }

    func testConnectionFailureIsDistinctFromHTTPAndContractFailure() async throws {
        // Given an unreachable selected endpoint.
        StubProtocol.handler = { _, stub in
            stub.client?.urlProtocol(stub, didFailWithError: URLError(.cannotConnectToHost))
        }
        let client = try makeClient()
        // When a Summary refresh attempts transport.
        do {
            _ = try await client.fetch()
            XCTFail("Expected connection failure")
        } catch {
            // Then connection status remains distinguishable.
            guard case SummaryFailure.connection = error else { return XCTFail("Expected connection failure") }
        }
    }

    private func makeClient() throws -> SummaryClient {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        return SummaryClient(origin: try ServerOrigin("http://localhost:8080"), configuration: configuration)
    }
}

private final class StubProtocol: URLProtocol {
    static var handler: ((URLRequest, StubProtocol) -> Void)?
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() { Self.handler?(request, self) }
    override func stopLoading() {}

    func respond(status: Int, body: Data, headers: [String: String] = [:]) {
        let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: headers)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: body)
        client?.urlProtocolDidFinishLoading(self)
    }
}

enum SummaryFixtures {
    static func object() -> [String: Any] {
        [
            "protocolVersion": 1, "observedAt": "2026-10-18T00:00:00Z",
            "componentId": "control-center", "instanceId": "current-instance",
            "controlCenter": ["status": "available", "version": ["state": "unavailable"],
                              "uptime": ["state": "unavailable"], "runtimeVersion": ["state": "unavailable"]],
            "overallHealth": ["status": "ok", "scope": "control-center", "checks": [["name": "component.reachable", "status": "ok"]]],
            "runtimes": ["total": 1, "returned": 1, "truncated": false,
                         "starting": 0, "running": 1, "stopped": 0, "stale": 0, "health": "unavailable",
                         "data": [["instanceId": "registered-1", "target": "example", "launcherState": "running", "status": "running"]]],
            "dashboardUrl": "/web/textus-control-center", "refreshAfterSeconds": 15
        ]
    }

    static func data(_ mutate: (inout [String: Any]) -> Void = { _ in }) throws -> Data {
        var value = object()
        mutate(&value)
        return try JSONSerialization.data(withJSONObject: value, options: [.sortedKeys])
    }

    static func summary() throws -> OperationalSummary {
        try JSONDecoder().decode(OperationalSummary.self, from: data())
    }
}
