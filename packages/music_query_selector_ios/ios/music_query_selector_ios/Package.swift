// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "music_query_selector_ios",
    platforms: [
        .iOS("12.0"),
    ],
    products: [
        .library(name: "music-query-selector-ios", targets: ["music_query_selector_ios"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "music_query_selector_ios",
            dependencies: []
        )
    ]
)
