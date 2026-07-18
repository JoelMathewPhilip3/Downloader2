package com.joel.videodownloader

import android.content.Context

object Prefs {
    private const val FILE = "settings"
    private const val OVERLAY_ENABLED = "overlay_enabled"
    private const val LAST_URL = "last_url"
    private const val LAST_ENGINE_UPDATE = "last_engine_update"

    fun overlayEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(OVERLAY_ENABLED, false)

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(OVERLAY_ENABLED, enabled).apply()
    }

    fun lastUrl(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(LAST_URL, null)

    fun setLastUrl(context: Context, url: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(LAST_URL, url).apply()
    }

    fun lastEngineUpdate(context: Context): Long =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(LAST_ENGINE_UPDATE, 0L)

    fun setLastEngineUpdate(context: Context, value: Long) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putLong(LAST_ENGINE_UPDATE, value).apply()
    }
}
