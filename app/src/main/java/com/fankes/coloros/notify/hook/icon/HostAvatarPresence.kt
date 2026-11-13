package com.fankes.coloros.notify.hook.icon

import android.app.Notification
import android.app.Person
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification

/** Metadata such as MessagingStyle or a contact name alone cannot supply an avatar. */
internal object HostAvatarPresence {
    fun contains(sbn: StatusBarNotification?): Boolean {
        val notification = sbn?.notification ?: return false
        return try {
            val extras = notification.extras
            notification.getLargeIcon() != null ||
                hasImage(parcelable(extras, Notification.EXTRA_LARGE_ICON)) ||
                hasImage(parcelable(extras, Notification.EXTRA_LARGE_ICON_BIG)) ||
                hasPeople(extras) ||
                hasMessageAvatar(extras, Notification.EXTRA_MESSAGES) ||
                hasMessageAvatar(extras, Notification.EXTRA_HISTORIC_MESSAGES)
        } catch (_: Exception) {
            // Malformed extras must not break a SystemUI hook or invent an avatar.
            false
        }
    }

    private fun hasImage(value: Parcelable?): Boolean = value is Icon || value is Bitmap

    @Suppress("DEPRECATION")
    private fun hasPeople(extras: Bundle): Boolean =
        extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
            ?.any { it.icon != null } == true

    @Suppress("DEPRECATION")
    private fun hasMessageAvatar(extras: Bundle, key: String): Boolean {
        val messages = extras.getParcelableArray(key) ?: return false
        return Notification.MessagingStyle.Message.getMessagesFromBundleArray(messages)
            .any { it.senderPerson?.icon != null }
    }

    @Suppress("DEPRECATION")
    private fun parcelable(extras: Bundle, key: String): Parcelable? = extras.getParcelable(key)
}
