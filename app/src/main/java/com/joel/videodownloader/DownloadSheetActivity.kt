package com.joel.videodownloader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DownloadSheetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_sheet)
        window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(android.view.Gravity.BOTTOM)

        val urlInput = findViewById<TextInputEditText>(R.id.sheetUrlInput)
        val formatGroup = findViewById<RadioGroup>(R.id.formatGroup)
        val mp3Radio = findViewById<RadioButton>(R.id.mp3Radio)
        val qualityLayout = findViewById<TextInputLayout>(R.id.qualityLayout)
        val qualityInput = findViewById<AutoCompleteTextView>(R.id.qualityInput)

        val qualities = listOf("720p", "1080p", "480p", "360p", "Best available")
        qualityInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, qualities))
        qualityInput.setText("720p", false)

        val supplied = intent.getStringExtra(EXTRA_URL)
        val cookies = intent.getStringExtra(EXTRA_COOKIES)
        val mediaUrl = intent.getStringExtra(EXTRA_MEDIA_URL)
        urlInput.setText(supplied ?: Prefs.lastUrl(this).orEmpty())

        findViewById<android.view.View>(R.id.sheetPasteButton).setOnClickListener {
            val url = UrlTools.clipboardUrl(this)
            if (url == null) Toast.makeText(this, "Clipboard does not contain a web link", Toast.LENGTH_SHORT).show()
            else urlInput.setText(url)
        }

        formatGroup.setOnCheckedChangeListener { _, checked ->
            qualityLayout.visibility = if (checked == R.id.mp3Radio) android.view.View.GONE else android.view.View.VISIBLE
        }

        findViewById<android.view.View>(R.id.startDownloadButton).setOnClickListener {
            val url = urlInput.text?.toString()?.trim().orEmpty()
            if (!UrlTools.isWebUrl(url)) {
                Toast.makeText(this, "This browser page does not have a valid web address.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (!EngineState.ready) {
                Toast.makeText(this, EngineState.error ?: "Downloader is still initializing", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Prefs.setLastUrl(this, url)
            val type = if (mp3Radio.isChecked) DownloadService.TYPE_MP3 else DownloadService.TYPE_MP4
            DownloadService.start(this, url, type, qualityInput.text.toString(), cookies, mediaUrl)
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<android.view.View>(R.id.cancelButton).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_URL = "url"
        private const val EXTRA_COOKIES = "cookies"
        private const val EXTRA_MEDIA_URL = "media_url"
        fun open(context: Context, url: String?, cookies: String? = null, mediaUrl: String? = null) {
            context.startActivity(Intent(context, DownloadSheetActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_COOKIES, cookies)
                putExtra(EXTRA_MEDIA_URL, mediaUrl)
            })
        }
    }
}
