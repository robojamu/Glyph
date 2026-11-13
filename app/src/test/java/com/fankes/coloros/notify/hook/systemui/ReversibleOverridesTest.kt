package com.fankes.coloros.notify.hook.systemui

import org.junit.Assert.*
import org.junit.Test

class ReversibleOverridesTest {
    private class Target(var icon: Any?)
    private val read: (Target) -> Any? = { it.icon }
    private val write: (Target, Any?) -> Unit = { target, icon -> target.icon = icon }

    @Test fun `disabling replacement restores the actual host icon`() {
        val original = Any()
        val target = Target(original)
        val registry = ReversibleOverrides<Target, Any>()
        registry.apply(target, Any(), read, write)
        registry.restore(target, read, write)
        assertSame(original, target.icon)
    }

    @Test fun `repeated replacement preserves the first host value`() {
        val original = Any()
        val target = Target(original)
        val registry = ReversibleOverrides<Target, Any>()
        registry.apply(target, Any(), read, write)
        registry.apply(target, Any(), read, write)
        registry.restoreAll(read, write)
        assertSame(original, target.icon)
    }

    @Test fun `a host update is never rolled back to an earlier notification icon`() {
        val target = Target(Any())
        val hostUpdate = Any()
        val registry = ReversibleOverrides<Target, Any>()
        registry.apply(target, Any(), read, write)
        target.icon = hostUpdate
        registry.restoreAll(read, write)
        assertSame(hostUpdate, target.icon)
        registry.apply(target, Any(), read, write)
        registry.restoreAll(read, write)
        assertSame(hostUpdate, target.icon)
    }

    @Test fun `null host value and multiple live notifications restore independently`() {
        val first = Target(null)
        val original = Any()
        val second = Target(original)
        val registry = ReversibleOverrides<Target, Any>()
        registry.apply(first, Any(), read, write)
        registry.apply(second, Any(), read, write)
        registry.restoreAll(read, write)
        assertNull(first.icon)
        assertSame(original, second.icon)
    }
}
