package com.taplock.app

/**
 * Pure helpers for the colon-separated [android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]
 * value. Must append/remove only our component — never overwrite the full list.
 */
object AccessibilityServicesList {

    fun parse(current: String?): LinkedHashSet<String> {
        if (current.isNullOrBlank()) return linkedSetOf()
        return current.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toCollection(LinkedHashSet())
    }

    fun append(current: String?, component: String): String =
        parse(current).apply { add(component) }.joinToString(":")

    fun remove(current: String?, component: String): String =
        parse(current).apply { remove(component) }.joinToString(":")

    fun contains(current: String?, component: String): Boolean =
        parse(current).contains(component)
}
