package com.fankes.coloros.notify.hook.systemui

/** Bounded, revision-scoped results. A package alone never identifies a notification. */
internal class NotificationIconCache<T>(private val capacity: Int = 256) {
    init { require(capacity > 0) }

    private data class Record<T>(val packageName: String, val value: T)
    private val entries = LinkedHashMap<String, Record<T>>(16, 0.75f, true)
    private var revision = 0L

    @Synchronized
    fun reset(newRevision: Long) {
        if (newRevision == revision) return
        entries.clear()
        revision = newRevision
    }

    @Synchronized
    fun put(key: String, packageName: String, value: T, revision: Long) {
        // A resolver started before publication must not repopulate a newer cache.
        if (revision != this.revision) return
        entries[key] = Record(packageName, value)
        while (entries.size > capacity) entries.remove(entries.keys.first())
    }

    @Synchronized
    fun lookup(key: String?, packageName: String?, revision: Long): T? {
        if (revision != this.revision || key == null) return null
        val record = entries[key] ?: return null
        return record.value.takeIf { packageName == null || record.packageName == packageName }
    }

    @Synchronized
    fun remove(key: String) { entries.remove(key) }

    @Synchronized
    fun clear() { entries.clear() }

    @Synchronized
    fun size(): Int = entries.size
}
