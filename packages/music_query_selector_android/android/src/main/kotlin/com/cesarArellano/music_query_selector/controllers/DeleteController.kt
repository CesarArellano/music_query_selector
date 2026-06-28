package com.cesarArellano.music_query_selector.controllers

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.cesarArellano.music_query_selector.PluginProvider
import io.flutter.Log
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/**
 * Deletes audio files using the native MediaStore flow.
 *
 * On Android 11+ (R) this shows the system "Allow this app to delete?" dialog
 * via [MediaStore.createDeleteRequest]; on Android 10 (Q) it falls back to the
 * [RecoverableSecurityException] dialog; below that it deletes directly.
 *
 * Returns `true` to Dart only when the user confirms and the file is removed.
 */
class DeleteController : PluginRegistry.ActivityResultListener {

    companion object {
        private const val TAG = "DeleteController"
        private const val DELETE_REQUEST_CODE = 984
    }

    // Held across the activity result round-trip. The plugin-wide
    // PluginProvider.result() is a WeakReference that gets replaced by the next
    // method call, so we keep our own strong reference here.
    private var pendingResult: MethodChannel.Result? = null

    fun deleteSongs(call: MethodCall, result: MethodChannel.Result) {
        val activity = PluginProvider.activity()
        val resolver = activity.contentResolver

        val ids = call.argument<List<Int>>("ids")
        if (ids.isNullOrEmpty()) {
            result.success(false)
            return
        }

        val uris: List<Uri> = ids.map {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.toLong())
        }

        try {
            when {
                // Android 11+ : single system dialog for one or many items.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                    pendingResult = result
                    activity.startIntentSenderForResult(
                        pendingIntent.intentSender, DELETE_REQUEST_CODE, null, 0, 0, 0
                    )
                }

                // Android 10 : delete may throw a recoverable exception -> dialog.
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                    try {
                        var deleted = 0
                        uris.forEach { deleted += resolver.delete(it, null, null) }
                        result.success(deleted > 0)
                    } catch (securityException: RecoverableSecurityException) {
                        pendingResult = result
                        activity.startIntentSenderForResult(
                            securityException.userAction.actionIntent.intentSender,
                            DELETE_REQUEST_CODE, null, 0, 0, 0
                        )
                    }
                }

                // Android 9 and below : direct delete, no dialog needed.
                else -> {
                    var deleted = 0
                    uris.forEach { deleted += resolver.delete(it, null, null) }
                    result.success(deleted > 0)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Delete failed: $e")
            pendingResult = null
            result.success(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != DELETE_REQUEST_CODE) return false
        pendingResult?.success(resultCode == Activity.RESULT_OK)
        pendingResult = null
        return true
    }
}
