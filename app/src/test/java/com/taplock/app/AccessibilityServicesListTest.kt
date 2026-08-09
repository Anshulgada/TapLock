package com.taplock.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [AccessibilityServicesList] colon-separated list helpers. */
class AccessibilityServicesListTest {

    private val ours = "com.taplock.app/com.taplock.app.LockAccessibilityService"
    private val foreign = "com.example.app/com.example.app.OtherService"

    @Test
    fun append_emptyList_addsComponent() {
        assertEquals(ours, AccessibilityServicesList.append(null, ours))
    }

    @Test
    fun append_existingForeign_preservesForeign() {
        val result = AccessibilityServicesList.append(foreign, ours)
        assertTrue(result.contains(foreign))
        assertTrue(result.contains(ours))
        assertEquals("$foreign:$ours", result)
    }

    @Test
    fun append_alreadyPresent_doesNotDuplicate() {
        val current = "$foreign:$ours"
        assertEquals(current, AccessibilityServicesList.append(current, ours))
    }

    @Test
    fun remove_midList_leavesOthers() {
        val current = "$foreign:$ours:com.third/com.third.Svc"
        assertEquals("$foreign:com.third/com.third.Svc", AccessibilityServicesList.remove(current, ours))
    }

    @Test
    fun remove_onlyEntry_returnsEmpty() {
        assertEquals("", AccessibilityServicesList.remove(ours, ours))
    }

    @Test
    fun contains_detectsComponent() {
        assertTrue(AccessibilityServicesList.contains("$foreign:$ours", ours))
        assertFalse(AccessibilityServicesList.contains(foreign, ours))
    }

    @Test
    fun parse_ignoresBlankSegments() {
        assertEquals(linkedSetOf(foreign, ours), AccessibilityServicesList.parse(":$foreign:::$ours:"))
    }
}
