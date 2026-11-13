package com.fankes.coloros.notify.hook.systemui

import java.util.WeakHashMap

/** Tracks only our own writes; a host update must never be overwritten with an older value. */
internal class ReversibleOverrides<T : Any, V : Any> {
    private data class Override<V>(val original: V?, val replacement: V)
    private val entries = WeakHashMap<T, Override<V>>()

    @Synchronized
    fun apply(target: T, replacement: V, read: (T) -> V?, write: (T, V?) -> Unit) {
        restore(target, read, write)
        val original = read(target)
        write(target, replacement)
        entries[target] = Override(original, replacement)
    }

    @Synchronized
    fun restore(target: T, read: (T) -> V?, write: (T, V?) -> Unit) {
        val previous = entries[target] ?: return
        if (read(target) === previous.replacement) write(target, previous.original)
        entries.remove(target)
    }

    @Synchronized
    fun restoreAll(read: (T) -> V?, write: (T, V?) -> Unit) {
        entries.keys.toList().forEach { restore(it, read, write) }
    }
}
