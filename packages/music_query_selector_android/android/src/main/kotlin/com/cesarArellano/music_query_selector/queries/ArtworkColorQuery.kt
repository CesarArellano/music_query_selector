package com.cesarArellano.music_query_selector.queries

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.cesarArellano.music_query_selector.PluginProvider
import com.cesarArellano.music_query_selector.queries.helper.QueryHelper
import com.cesarArellano.music_query_selector.types.checkArtworkType
import io.flutter.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream

/**
 * Extracts the dominant color from an artwork natively, reusing the decoded
 * bitmap instead of round-tripping the compressed bytes to Dart.
 *
 * Returns the ARGB color as an [Int] (consumed in Dart via `Color(value)`),
 * or null when the item has no artwork.
 */
class ArtworkColorQuery : ViewModel() {

    companion object {
        private const val TAG = "OnArtworkColorQuery"

        // Palette only needs a small bitmap to find the dominant color and a
        // smaller decode keeps the cold path cheap.
        private const val SAMPLE_SIZE = 64
        private const val MAX_COLOR_COUNT = 16
    }

    private val helper = QueryHelper()
    private var type: Int = -1
    private var id: Number = 0
    private var showDetailedLog: Boolean = false

    private lateinit var uri: Uri
    private lateinit var resolver: ContentResolver

    fun queryArtworkColor() {
        val call = PluginProvider.call()
        val result = PluginProvider.result()
        val context = PluginProvider.context()
        this.resolver = context.contentResolver
        this.showDetailedLog = PluginProvider.showDetailedLog

        id = call.argument<Number>("id")!!

        // Check uri/type:
        //   * 0 -> Song.
        //   * 1 -> Album.
        //   * 2 -> Playlist.
        //   * 3 -> Artist.
        //   * 4 -> Genre.
        uri = checkArtworkType(call.argument<Int>("type")!!)
        type = call.argument<Int>("type")!!

        Log.d(TAG, "Query config: ")
        Log.d(TAG, "\tid: $id")
        Log.d(TAG, "\turi: $uri")
        Log.d(TAG, "\ttype: $type")

        viewModelScope.launch {
            val bitmap = loadBitmap()
            if (bitmap == null) {
                result.success(null)
                return@launch
            }

            val color = withContext(Dispatchers.Default) {
                Palette.from(bitmap)
                    .maximumColorCount(MAX_COLOR_COUNT)
                    .generate()
                    .dominantSwatch
                    ?.rgb
            }

            result.success(color)
        }
    }

    // Decode the artwork into a Bitmap in background.
    private suspend fun loadBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        // If 'Android' >= 29/Q:
        //   * Limited access to files/folders. Use 'loadThumbnail'.
        // If 'Android' < 29/Q:
        //   * Use the 'embeddedPicture' from 'MediaMetadataRetriever' to get the image.
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                // For playlist/artist/genre, use the first item to 'simulate' the artwork.
                val query = if (type == 2 || type == 3 || type == 4) {
                    val item = helper.loadFirstItem(type, id, resolver) ?: return@withContext null
                    ContentUris.withAppendedId(uri, item.toLong())
                } else {
                    ContentUris.withAppendedId(uri, id.toLong())
                }

                return@withContext resolver.loadThumbnail(query, Size(SAMPLE_SIZE, SAMPLE_SIZE), null)
            } catch (e: Exception) {
                if (showDetailedLog) Log.w(TAG, "($id) Message: $e")
                return@withContext null
            }
        } else {
            val item = helper.loadFirstItem(type, id, resolver) ?: return@withContext null

            try {
                val file = FileInputStream(item)
                val metadata = MediaMetadataRetriever()

                metadata.setDataSource(file.fd)
                val image = metadata.embeddedPicture ?: return@withContext null

                return@withContext BitmapFactory.decodeByteArray(image, 0, image.size)
            } catch (e: Exception) {
                if (showDetailedLog) Log.w(TAG, "($id) Message: $e")
                return@withContext null
            }
        }
    }
}
