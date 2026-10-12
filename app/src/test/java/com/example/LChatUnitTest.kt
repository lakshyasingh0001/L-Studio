package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CallStatus
import com.example.data.model.Story
import com.example.data.repository.ChatRepository
import com.example.data.util.CryptoUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LChatUnitTest {

    @Test
    fun testEncryptionAndDecryption() {
        val original = "Secret L Chat conversation message"
        val (ciphertext, iv) = CryptoUtils.encryptText(original)

        assertNotNull(ciphertext)
        assertNotNull(iv)
        assertTrue(ciphertext.isNotBlank())
        assertTrue(iv.isNotBlank())

        val decrypted = CryptoUtils.decryptText(ciphertext, iv)
        assertEquals(original, decrypted)
    }

    @Test
    fun testSafetyNumberGeneration() {
        val safetyNumber = CryptoUtils.generateSafetyNumber("user_lakshya", "user_sarah")
        assertNotNull(safetyNumber)
        val groups = safetyNumber.split(" ")
        assertEquals(12, groups.size)
        groups.forEach { group ->
            assertEquals(5, group.length)
        }
    }

    @Test
    fun testStoryExpiration() {
        val now = System.currentTimeMillis()
        val activeStory = Story(
            id = "s1",
            userId = "u1",
            authorName = "Lakshya",
            authorAvatar = "",
            createdAt = null,
            expiresAtTimestamp = com.google.firebase.Timestamp(java.util.Date(now + (24 * 60 * 60 * 1000L)))
        )
        assertFalse(activeStory.isExpired)
        assertTrue(activeStory.remainingTimeText.contains("h") || activeStory.remainingTimeText.contains("m"))

        val expiredStory = Story(
            id = "s2",
            userId = "u2",
            authorName = "Sarah",
            authorAvatar = "",
            createdAt = null,
            expiresAtTimestamp = com.google.firebase.Timestamp(java.util.Date(now - (1 * 60 * 60 * 1000L)))
        )
        assertTrue(expiredStory.isExpired)
        assertEquals("Expired", expiredStory.remainingTimeText)
    }
}
