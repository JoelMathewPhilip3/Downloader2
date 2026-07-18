package com.joel.videodownloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class DownloadService : Service() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_MP4
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "720p"
        val cookies = intent.getStringExtra(EXTRA_COOKIES)
        val mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL)
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        startForeground(notificationId, buildNotification("Starting…", 0, true))

        executor.execute {
            try {
                val outputDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "JoelDownloader")
                outputDir.mkdirs()
                val request = buildRequest(url, type, quality, outputDir, cookies, mediaUrl)
                val processId = UUID.randomUUID().toString()
                try {
                    executeWithUpdateRetry(request, processId, notificationId)
                } catch (pageError: Throwable) {
                    val direct = mediaUrl?.takeIf { it.startsWith("http") && it != url }
                    if (direct == null) throw pageError
                    getSystemService(NotificationManager::class.java).notify(
                        notificationId,
                        buildNotification("Page extraction failed · trying detected media…", 0, true)
                    )
                    val fallback = buildRequest(direct, type, quality, outputDir, cookies, url)
                    executeWithUpdateRetry(fallback, UUID.randomUUID().toString(), notificationId)
                }
                getSystemService(NotificationManager::class.java).notify(
                    notificationId,
                    buildNotification("Completed · Downloads/JoelDownloader", 100, false)
                )
            } catch (t: Throwable) {
                getSystemService(NotificationManager::class.java).notify(
                    notificationId,
                    buildNotification("Failed: ${t.message ?: "unknown error"}", 0, false)
                )
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }


    private fun executeWithUpdateRetry(
        request: YoutubeDLRequest,
        processId: String,
        notificationId: Int
    ) {
        fun execute() {
            YoutubeDL.getInstance().execute(
                request,
                processId
            ) { progress: Float, etaInSeconds: Long, _: String ->
                getSystemService(NotificationManager::class.java).notify(
                    notificationId,
                    buildNotification(
                        "${progress.toInt()}% · ETA ${etaInSeconds}s",
                        progress.toInt(),
                        false
                    )
                )
            }
        }

        try {
            execute()
        } catch (first: Throwable) {
            val message = first.message.orEmpty().lowercase()
            val looksStale = message.contains("90 days") ||
                message.contains("outdated") ||
                message.contains("update yt-dlp") ||
                message.contains("unsupported url")
            if (!looksStale) throw first

            getSystemService(NotificationManager::class.java).notify(
                notificationId,
                buildNotification("Updating yt-dlp and retrying…", 0, true)
            )
            EngineUpdater.updateNightly(this, force = true).getOrThrow()
            execute()
        }
    }

    private fun buildRequest(url: String, type: String, quality: String, outputDir: File, cookies: String?, mediaUrl: String?): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        request.addOption("--no-playlist")
        request.addOption("--newline")
        request.addOption("--no-mtime")
        request.addOption("--restrict-filenames")
        request.addOption("--embed-metadata")
        request.addOption("--js-runtimes", "quickjs")
        request.addOption("--remote-components", "ejs:github")
        if (!cookies.isNullOrBlank()) request.addOption("--add-header", "Cookie:$cookies")
        if (!mediaUrl.isNullOrBlank() && mediaUrl.startsWith("http") && mediaUrl != url) {
            request.addOption("--referer", url)
        }

        if (type == TYPE_MP3) {
            request.addOption("-x")
            request.addOption("--audio-format", "mp3")
            request.addOption("--audio-quality", "192K")
            request.addOption("--embed-thumbnail")
            request.addOption("--convert-thumbnails", "jpg")
            request.addOption("--parse-metadata", "%(uploader)s:%(meta_artist)s")
            request.addOption("--parse-metadata", "YouTube:%(meta_album)s")
            request.addOption("-o", File(outputDir, "%(artist,uploader)s - %(title)s.%(ext)s").absolutePath)
        } else {
            val height = quality.filter(Char::isDigit).toIntOrNull()
            val format = if (height == null) {
                "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/best"
            } else {
                "bv*[height<=$height][ext=mp4]+ba[ext=m4a]/b[height<=$height][ext=mp4]/best[height<=$height]"
            }
            request.addOption("-f", format)
            request.addOption("--merge-output-format", "mp4")
            request.addOption("-o", File(outputDir, "%(title)s.%(ext)s").absolutePath)
        }
        return request
    }

    private fun buildNotification(text: String, progress: Int, indeterminate: Boolean): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Joel Downloader")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(progress in 0..99)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val TYPE_MP3 = "mp3"
        const val TYPE_MP4 = "mp4"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_QUALITY = "quality"
        private const val EXTRA_COOKIES = "cookies"
        private const val EXTRA_MEDIA_URL = "media_url"
        private const val CHANNEL = "downloads"

        fun start(context: Context, url: String, type: String, quality: String, cookies: String? = null, mediaUrl: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TYPE, type)
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_COOKIES, cookies)
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
