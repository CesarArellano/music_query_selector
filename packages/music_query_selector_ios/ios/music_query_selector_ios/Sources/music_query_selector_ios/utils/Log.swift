import Foundation

class Log {
    private init() {}

    enum Level: Int, Comparable {
        case verbose = 2, debug = 3, info = 4, warning = 5, error = 6

        static func < (lhs: Level, rhs: Level) -> Bool { lhs.rawValue < rhs.rawValue }

        var label: String {
            switch self {
            case .verbose: return "VERBOSE"
            case .debug:   return "DEBUG"
            case .info:    return "INFO"
            case .warning: return "WARNING"
            case .error:   return "ERROR"
            }
        }
    }

    private static var minLevel: Level = .warning

    static let type = Logger()

    struct Logger {
        func verbose(_ msg: String) { Log.emit(.verbose, msg) }
        func debug(_ msg: String)   { Log.emit(.debug, msg) }
        func info(_ msg: String)    { Log.emit(.info, msg) }
        func warning(_ msg: String) { Log.emit(.warning, msg) }
        func error(_ msg: String)   { Log.emit(.error, msg) }
    }

    private static func emit(_ level: Level, _ msg: String) {
        guard level >= minLevel else { return }
        print("[MusicQuerySelector][\(level.label)] \(msg)")
    }

    static func setLogLevel(level: Level) {
        minLevel = level
    }

    static func setLogLevel(dartLevel: Int) {
        minLevel = Level(rawValue: dartLevel) ?? .warning
    }
}
