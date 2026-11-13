package com.fankes.coloros.notify.hook.systemui

import android.app.Notification
import android.graphics.drawable.Icon
import java.lang.reflect.Field

internal object NotificationSmallIconOverrides {
    private val overrides = ReversibleOverrides<Notification, Icon>()
    private val field: Field by lazy {
        Notification::class.java.getDeclaredField("mSmallIcon").apply { isAccessible = true }
    }

    fun apply(notification: Notification, icon: Icon) =
        overrides.apply(notification, icon, ::read, ::write)

    fun restore(notification: Notification) = overrides.restore(notification, ::read, ::write)

    fun restoreAll() = overrides.restoreAll(::read, ::write)

    private fun read(notification: Notification): Icon? = notification.smallIcon
    private fun write(notification: Notification, icon: Icon?) { field.set(notification, icon) }
}
