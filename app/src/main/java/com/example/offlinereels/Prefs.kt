package com.example.offlinereels

import android.content.Context

/** Small wrapper around SharedPreferences for the user's settings. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("reels", Context.MODE_PRIVATE)

    var favorites: Set<Long>
        get() = (sp.getStringSet("favorites", emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        set(value) {
            sp.edit().putStringSet("favorites", value.map { it.toString() }.toSet()).apply()
        }

    var muted: Boolean
        get() = sp.getBoolean("muted", false)
        set(value) {
            sp.edit().putBoolean("muted", value).apply()
        }

    var shuffle: Boolean
        get() = sp.getBoolean("shuffle", false)
        set(value) {
            sp.edit().putBoolean("shuffle", value).apply()
        }

    var lastVideoId: Long
        get() = sp.getLong("lastVideoId", -1L)
        set(value) {
            sp.edit().putLong("lastVideoId", value).apply()
        }
}
