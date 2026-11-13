package com.fankes.coloros.notify.hook.systemui

import android.graphics.drawable.Icon

/** Shares replacements only for the exact notification and configuration that produced them. */
internal object StatusBarIconReplacementCache {
    data class Entry(val icon: Icon, val isColorable: Boolean)

    private val cache = NotificationIconCache<Entry>()

    fun reset(revision: Long) = cache.reset(revision)

    fun put(notificationKey: String, packageName: String, icon: Icon, isColorable: Boolean, revision: Long) =
        cache.put(notificationKey, packageName, Entry(icon, isColorable), revision)

    fun iconFor(notificationKey: String?, packageName: String?, revision: Long): Icon? =
        lookup(notificationKey, packageName, revision)?.icon

    fun lookup(notificationKey: String?, packageName: String?, revision: Long): Entry? =
        cache.lookup(notificationKey, packageName, revision)

    fun remove(notificationKey: String) = cache.remove(notificationKey)

    fun clear() = cache.clear()
}
