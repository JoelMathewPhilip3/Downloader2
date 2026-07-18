package com.joel.videodownloader

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL

/**
 * Uses reflection so this project remains compatible with youtubedl-android
 * builds that expose UpdateChannel as either a Java or Kotlin enum.
 */
object EngineUpdater {
    private const val TAG = "EngineUpdater"
    private const val UPDATE_INTERVAL_MS = 24L * 60L * 60L * 1000L

    @Synchronized
    fun updateNightly(context: Context, force: Boolean = false): Result<String> {
        val now = System.currentTimeMillis()
        if (!force && now - Prefs.lastEngineUpdate(context) < UPDATE_INTERVAL_MS) {
            return Result.success("Recently checked")
        }

        return runCatching {
            EngineState.updating = true
            EngineState.updateMessage = "Updating yt-dlp…"

            val instance = YoutubeDL.getInstance()
            val method = instance.javaClass.methods.firstOrNull {
                it.name == "updateYoutubeDL" && it.parameterTypes.size == 2
            } ?: error("This downloader library does not expose updateYoutubeDL")

            val channelType = method.parameterTypes[1]
            val nightly = channelType.enumConstants?.firstOrNull {
                (it as? Enum<*>)?.name == "NIGHTLY"
            } ?: channelType.enumConstants?.firstOrNull {
                (it as? Enum<*>)?.name == "STABLE"
            } ?: error("No supported yt-dlp update channel found")

            val response = method.invoke(instance, context.applicationContext, nightly)
            Prefs.setLastEngineUpdate(context, now)
            val message = response?.toString()?.takeIf { it.isNotBlank() } ?: "yt-dlp is current"
            EngineState.updateMessage = message
            Log.i(TAG, message)
            message
        }.onFailure {
            EngineState.updateMessage = "Update failed: ${it.cause?.message ?: it.message}"
            Log.w(TAG, "yt-dlp update failed", it)
        }.also {
            EngineState.updating = false
        }
    }
}
