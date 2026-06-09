package com.example.myapplication.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DECODE_SIZE_PX = 900

@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val normalizedPath = remember(assetPath) { assetPath.trim().removePrefix("/") }
    var imageBitmap by remember(normalizedPath) {
        mutableStateOf(AssetImageCache.get(normalizedPath))
    }

    LaunchedEffect(normalizedPath) {
        if (normalizedPath.isNotBlank() && imageBitmap == null) {
            imageBitmap = AssetImageCache.load(context, normalizedPath)
        }
    }

    Box(modifier = modifier) {
        val bitmap = imageBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            fallback()
        }
    }
}

private object AssetImageCache {
    private val cache = LruCache<String, ImageBitmap>(20)
    private val failedPaths = mutableSetOf<String>()

    @Synchronized
    fun get(path: String): ImageBitmap? = cache.get(path)

    suspend fun load(context: Context, path: String): ImageBitmap? {
        if (path.isBlank() || isFailed(path)) return null
        get(path)?.let { return it }

        return withContext(Dispatchers.IO) {
            get(path)?.let { return@withContext it }
            runCatching {
                val bounds = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.applicationContext.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                }

                val options = BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(bounds)
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                context.applicationContext.assets.open(path).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                }
            }.getOrNull()
                ?.also { put(path, it) }
                ?: run {
                    markFailed(path)
                    null
                }
        }
    }

    @Synchronized
    private fun put(path: String, imageBitmap: ImageBitmap) {
        cache.put(path, imageBitmap)
    }

    @Synchronized
    private fun isFailed(path: String): Boolean = path in failedPaths

    @Synchronized
    private fun markFailed(path: String) {
        failedPaths.add(path)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        var inSampleSize = 1
        val height = options.outHeight
        val width = options.outWidth

        if (height > MAX_DECODE_SIZE_PX || width > MAX_DECODE_SIZE_PX) {
            while (
                height / inSampleSize > MAX_DECODE_SIZE_PX ||
                    width / inSampleSize > MAX_DECODE_SIZE_PX
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize.coerceAtLeast(1)
    }
}
