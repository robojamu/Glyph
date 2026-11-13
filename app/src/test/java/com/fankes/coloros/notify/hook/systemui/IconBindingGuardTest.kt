package com.fankes.coloros.notify.hook.systemui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IconBindingGuardTest {
    private class Snapshot(val revision: Long = 1)
    private val guard = IconBindingGuard<Snapshot>()
    private val target = Any()
    private var snapshot = Snapshot()
    private var key: String? = "a"
    private var rendered: String? = null
    private var avatar = false
    private val queue = mutableListOf<() -> Unit>()

    private fun bind(replace: Boolean = true, retries: Int = 1) {
        val binding = guard.bind(target, key, snapshot)
        val plan = key
        if (!replace) return
        repeat(retries) {
            queue += {
                if (guard.isCurrent(target, binding, key, snapshot) && !avatar) rendered = plan
            }
        }
    }

    @Test
    fun `queued banner plan cannot overwrite a newer notification`() {
        bind()
        key = "b"
        bind()
        queue.last().invoke()
        queue.first().invoke()
        assertEquals("b", rendered)
    }

    @Test
    fun `same key rebinding invalidates old generation`() {
        val old = guard.bind(target, key, snapshot)
        val current = guard.bind(target, key, snapshot)
        assertFalse(guard.isCurrent(target, old, key, snapshot))
        assertTrue(guard.isCurrent(target, current, key, snapshot))
    }

    @Test
    fun `binding without replacement invalidates queued work`() {
        bind(retries = 3)
        bind(replace = false)
        queue.forEach { it() }
        assertEquals(null, rendered)
    }

    @Test
    fun `avatar rebind of same notification invalidates queued work`() {
        bind(retries = 3)
        avatar = true
        bind(replace = false)
        avatar = false // Generation must reject the old work independently of the avatar check.
        queue.forEach { it() }
        assertEquals(null, rendered)
    }

    @Test
    fun `snapshot identity change rejects captured plan even with equal revisions`() {
        bind()
        snapshot = Snapshot(snapshot.revision)
        queue.single().invoke()
        assertEquals(null, rendered)
    }

    @Test
    fun `unknown binding key cannot authorize a write`() {
        key = null
        bind()
        queue.single().invoke()
        assertEquals(null, rendered)
    }

    @Test
    fun `all capsule retries remain valid for the current bind`() {
        val binding = guard.bind(target, key, snapshot)
        var writes = 0
        val tasks = List(3) {
            {
                if (guard.isCurrent(target, binding, key, snapshot)) writes++
            }
        }
        tasks.forEach { it() }
        assertEquals(3, writes)
        assertTrue(guard.isCurrent(target, binding, key, snapshot))
    }

    @Test
    fun `host rendering stages preserve callbacks from the same bind`() {
        val binding = guard.bind(target, key, snapshot)
        assertSame(binding, guard.currentOrBind(target, key, snapshot))
        assertSame(binding, guard.currentOrBind(target, key, snapshot))
        assertTrue(guard.isCurrent(target, binding, key, snapshot))
    }

    @Test
    fun `rendering stage for another notification invalidates earlier callbacks`() {
        val binding = guard.bind(target, key, snapshot)
        guard.currentOrBind(target, "b", snapshot)
        assertFalse(guard.isCurrent(target, binding, key, snapshot))
    }

    @Test
    fun `card rebinding rejects old innerData even when its icon is unchanged`() {
        val card = Any()
        val oldCard = guard.bind(card, key, snapshot)
        val icon = guard.bind(target, key, snapshot)
        var staleInnerDataWrites = 0
        val oldTask = {
            if (guard.isCurrent(card, oldCard, key, snapshot) &&
                guard.isCurrent(target, icon, key, snapshot)
            ) staleInnerDataWrites++
        }
        guard.bind(card, key, snapshot)
        repeat(3) { oldTask() }
        assertEquals(0, staleInnerDataWrites)
    }

    @Test
    fun `a token cannot be used for another view`() {
        val binding = guard.bind(target, key, snapshot)
        val other = Any()
        guard.bind(other, key, snapshot)
        assertFalse(guard.isCurrent(other, binding, key, snapshot))
        assertTrue(guard.isCurrent(target, binding, key, snapshot))
    }
}
