package com.joel.videodownloader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Patterns

object UrlTools {
    fun clipboardUrl(context: Context): String? {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
        return text?.takeIf(::isWebUrl)
    }

    fun isWebUrl(value: String): Boolean =
        (value.startsWith("https://", true) || value.startsWith("http://", true)) &&
            Patterns.WEB_URL.matcher(value).matches()

    fun normalizeBrowserUrl(raw: String): String? {
        val value = raw.trim().removePrefix("URL: ").trim()
        if (value.isBlank() || value.contains(' ') || value.length > 2048) return null
        if (isWebUrl(value)) return value
        // Brave may expose an omnibox URL without the scheme.
        if (value.contains('.') && !value.startsWith("javascript:", true) && !value.startsWith("data:", true)) {
            val candidate = "https://$value"
            if (Patterns.WEB_URL.matcher(candidate).matches()) return candidate
        }
        return null
    }

    fun copy(context: Context, value: String) {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("video link", value))
    }
}
