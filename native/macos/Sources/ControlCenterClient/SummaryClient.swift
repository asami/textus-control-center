import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public enum SummaryFailure: Error, Equatable, LocalizedError, Sendable {
    case connection(String)
    case http(Int)
    case redirect
    case contract(String)

    public var errorDescription: String? {
        switch self {
        case .connection(let message): return "Disconnected: \(message)"
        case .http(let status): return "HTTP error: \(status)"
        case .redirect: return "HTTP error: redirects are refused"
        case .contract(let message): return "Contract error: \(message)"
        }
    }
}

public struct ServerOrigin: Equatable, Sendable {
    public static let dashboardPath = "/web/textus-control-center"
    public static let summaryPath = "/rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary"
    public let url: URL

    public init(_ value: String) throws {
        guard let parts = URLComponents(string: value),
              parts.scheme == "http",
              let host = parts.host, ["127.0.0.1", "[::1]", "::1", "localhost"].contains(host),
              let port = parts.port, (1...65535).contains(port),
              parts.user == nil, parts.password == nil,
              parts.path.isEmpty || parts.path == "/",
              parts.percentEncodedPath.isEmpty || parts.percentEncodedPath == "/",
              parts.query == nil, parts.fragment == nil,
              let parsed = parts.url else {
            throw SummaryFailure.contract("Use an explicit loopback HTTP origin and port, for example http://127.0.0.1:8080")
        }
        var canonical = URLComponents(url: parsed, resolvingAgainstBaseURL: false)!
        canonical.path = ""
        url = canonical.url!
    }

    public var summaryURL: URL {
        var parts = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        parts.path = Self.summaryPath
        parts.queryItems = [URLQueryItem(name: "limit", value: "50")]
        return parts.url!
    }

    public func dashboardURL(_ relativePath: String) throws -> URL {
        guard relativePath == Self.dashboardPath else {
            throw SummaryFailure.contract("Dashboard URL must be \(Self.dashboardPath)")
        }
        return URL(string: relativePath, relativeTo: url)!.absoluteURL
    }
}

public struct SummaryReceipt: Sendable {
    public let summary: OperationalSummary
    public let json: Data
}

/// The same bounded transport serves the presentation and --summary-once.
public final class SummaryClient: @unchecked Sendable {
    public static let maximumResponseBytes = 1_048_576
    public let origin: ServerOrigin
    private let session: URLSession

    public init(origin: ServerOrigin) {
        self.origin = origin
        session = Self.makeSession(configuration: .ephemeral)
    }

    // URLProtocol is injected only at this production transport boundary by specifications.
    init(origin: ServerOrigin, configuration: URLSessionConfiguration) {
        self.origin = origin
        session = Self.makeSession(configuration: configuration)
    }

    private static func makeSession(configuration: URLSessionConfiguration) -> URLSession {
        configuration.timeoutIntervalForRequest = 5
        configuration.timeoutIntervalForResource = 5
        configuration.httpShouldSetCookies = false
        configuration.httpCookieStorage = nil
        configuration.urlCache = nil
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        return URLSession(configuration: configuration, delegate: RefuseRedirects(), delegateQueue: nil)
    }

    deinit { session.invalidateAndCancel() }

    public func fetch() async throws -> SummaryReceipt {
        var request = URLRequest(url: origin.summaryURL)
        request.httpMethod = "GET"
        request.timeoutInterval = 5
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        do {
            let (bytes, response) = try await session.bytes(for: request)
            guard let http = response as? HTTPURLResponse else {
                bytes.task.cancel()
                throw SummaryFailure.contract("Expected HTTP response")
            }
            guard (200...299).contains(http.statusCode) else {
                bytes.task.cancel()
                if (300...399).contains(http.statusCode) { throw SummaryFailure.redirect }
                throw SummaryFailure.http(http.statusCode)
            }
            guard response.expectedContentLength <= Int64(Self.maximumResponseBytes) else {
                bytes.task.cancel()
                throw SummaryFailure.contract("Summary exceeds 1 MiB")
            }
            var data = Data()
            for try await byte in bytes {
                guard data.count < Self.maximumResponseBytes else {
                    bytes.task.cancel()
                    throw SummaryFailure.contract("Summary exceeds 1 MiB")
                }
                data.append(byte)
            }
            try Task.checkCancellation()
            let summary: OperationalSummary
            do { summary = try JSONDecoder().decode(OperationalSummary.self, from: data) }
            catch let failure as SummaryFailure { throw failure }
            catch { throw SummaryFailure.contract("Invalid Summary JSON: \(error.localizedDescription)") }
            _ = try origin.dashboardURL(summary.dashboardUrl)
            return SummaryReceipt(summary: summary, json: data)
        } catch is CancellationError {
            throw CancellationError()
        } catch let failure as SummaryFailure {
            throw failure
        } catch {
            if Task.isCancelled { throw CancellationError() }
            throw SummaryFailure.connection(error.localizedDescription)
        }
    }
}

private final class RefuseRedirects: NSObject, URLSessionTaskDelegate {
    func urlSession(_ session: URLSession, task: URLSessionTask,
                    willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest,
                    completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}
