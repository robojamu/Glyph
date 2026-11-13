package com.fankes.coloros.notify.hook.systemui

import java.util.WeakHashMap

/** Tokens contain no target reference, so queued work cannot keep registry keys alive. */
internal class IconBindingGuard<S : Any> {
    internal class Binding<S : Any>(
        val generation: Long,
        val notificationKey: String?,
        val snapshot: S,
    )

    private val bindings = WeakHashMap<Any, Binding<S>>()
    private var generation = 0L

    /** Call for every host bind, including binds that will not receive a replacement. */
    @Synchronized
    fun bind(target: Any, notificationKey: String?, snapshot: S): Binding<S> =
        Binding(++generation, notificationKey, snapshot).also { bindings[target] = it }

    /** Rendering stages of one bind share its token, preserving all host callback retries. */
    @Synchronized
    fun currentOrBind(target: Any, notificationKey: String?, snapshot: S): Binding<S> {
        val current = bindings[target]
        return if (current != null && current.notificationKey == notificationKey &&
            current.snapshot === snapshot
        ) current else bind(target, notificationKey, snapshot)
    }

    @Synchronized
    fun isCurrent(target: Any, binding: Binding<S>, notificationKey: String?, snapshot: S): Boolean {
        val current = bindings[target] ?: return false
        return current.generation == binding.generation &&
            current === binding &&
            binding.notificationKey != null &&
            binding.notificationKey == notificationKey &&
            binding.snapshot === snapshot
    }
}
