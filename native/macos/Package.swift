// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "TextusControlCenterMenuBar",
    platforms: [.macOS(.v13)],
    products: [
        .library(name: "ControlCenterClient", targets: ["ControlCenterClient"]),
        .executable(name: "TextusControlCenterMenuBar", targets: ["ControlCenterMenuBar"])
    ],
    targets: [
        .target(name: "ControlCenterClient"),
        .executableTarget(name: "ControlCenterMenuBar", dependencies: ["ControlCenterClient"]),
        .testTarget(name: "ControlCenterClientTests", dependencies: ["ControlCenterClient"])
    ],
    swiftLanguageVersions: [.v5]
)
