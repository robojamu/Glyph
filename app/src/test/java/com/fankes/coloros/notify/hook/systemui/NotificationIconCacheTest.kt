package com.fankes.coloros.notify.hook.systemui

import org.junit.Assert.*
import org.junit.Test

class NotificationIconCacheTest {
    @Test fun `default snapshot works before remote configuration is available`() {
        val cache = NotificationIconCache<String>()
        cache.put("default", "app", "icon", 0)
        assertEquals("icon", cache.lookup("default", "app", 0))
    }

    @Test fun `publication discards old results and rejects late writers`() {
        val cache = NotificationIconCache<String>()
        cache.reset(1)
        cache.put("0|app|1", "app", "old", 1)
        cache.reset(2)
        assertNull(cache.lookup("0|app|1", "app", 2))
        cache.put("0|app|1", "app", "late", 1)
        assertNull(cache.lookup("0|app|1", "app", 2))
        cache.put("0|app|1", "app", "new", 2)
        assertEquals("new", cache.lookup("0|app|1", "app", 2))
        assertNull(cache.lookup("0|app|1", "app", 1))
    }

    @Test fun `missing keys never borrow another notification or user result`() {
        val cache = NotificationIconCache<String>()
        cache.reset(1)
        cache.put("0|app|1", "app", "personal", 1)
        assertNull(cache.lookup("0|app|2", "app", 1))
        assertNull(cache.lookup("10|app|1", "app", 1))
        assertNull(cache.lookup(null, "app", 1))
        assertNull(cache.lookup("0|app|1", "other", 1))
        assertEquals("personal", cache.lookup("0|app|1", null, 1))
    }

    @Test fun `historical keys are bounded and least recently used entries are reclaimed`() {
        val cache = NotificationIconCache<String>(2)
        cache.reset(1)
        cache.put("A", "app", "A", 1)
        cache.put("B", "app", "B", 1)
        assertEquals("A", cache.lookup("A", "app", 1))
        cache.put("C", "app", "C", 1)
        assertEquals(2, cache.size())
        assertNull(cache.lookup("B", "app", 1))
        repeat(10000) { cache.put("key$it", "app", "icon$it", 1) }
        assertEquals(2, cache.size())
    }

    @Test fun `removal prevents reuse after a notification exits the replacement policy`() {
        val cache = NotificationIconCache<String>()
        cache.reset(1)
        cache.put("A", "app", "theme", 1)
        cache.remove("A")
        assertNull(cache.lookup("A", "app", 1))
        cache.put("B", "app", "rule", 1)
        cache.clear()
        assertEquals(0, cache.size())
    }
}
