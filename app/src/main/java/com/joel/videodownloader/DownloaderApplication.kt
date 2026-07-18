package com.joel.videodownloader

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class DownloaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                EngineUpdater.updateNightly(this, force = false)
                EngineState.ready = true
            } catch (t: Throwable) {
                EngineState.error = t.message ?: "Downloader initialization failed"
                Log.e("JoelDownloader", "Initialization failed", t)
            }
        }.start()
    }
}

object EngineState {
    @Volatile var ready: Boolean = false
    @Volatile var error: String? = null
    @Volatile var updating: Boolean = false
    @Volatile var updateMessage: String? = null
}
