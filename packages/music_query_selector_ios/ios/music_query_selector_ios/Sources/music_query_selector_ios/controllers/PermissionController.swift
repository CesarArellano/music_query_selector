import MediaPlayer

class PermissionController {
    public static func checkPermission() -> Bool {
        let permissionStatus = MPMediaLibrary.authorizationStatus()
        if permissionStatus == MPMediaLibraryAuthorizationStatus.authorized {
            return true
        } else {
            return false
        }
    }
    
    public static func requestPermission() -> Bool {
        Log.type.debug("Requesting permissions.")
        Log.type.debug("iOS Version: \(ProcessInfo().operatingSystemVersion.majorVersion)")

        // MPMediaLibrary.requestAuthorization fires its callback asynchronously.
        // Block this (background) thread with a semaphore until the callback signals.
        // MUST be called from a non-main thread to avoid deadlock.
        let semaphore = DispatchSemaphore(value: 0)
        var isPermissionGranted = false
        MPMediaLibrary.requestAuthorization { status in
            isPermissionGranted = status == .authorized
            Log.type.debug("Permission accepted: \(isPermissionGranted)")
            semaphore.signal()
        }
        semaphore.wait()
        return isPermissionGranted
    }
}
