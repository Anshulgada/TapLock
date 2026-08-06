package com.taplock.app

import android.graphics.drawable.VectorDrawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies widget resources load correctly at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetResourceTest {

    @Test
    fun lockIcon_loadsAsVectorDrawable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_lock)
        assertNotNull(drawable)
        assertTrue(drawable is VectorDrawable)
    }

    @Test
    fun lockAnimationFrames_loadAsVectorDrawables() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        listOf(R.drawable.ic_lock_open, R.drawable.ic_lock_mid, R.drawable.ic_lock).forEach { resId ->
            val drawable = AppCompatResources.getDrawable(context, resId)
            assertNotNull(drawable)
            assertTrue(drawable is VectorDrawable)
        }
    }

    @Test
    fun widgetLayout_inflates() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = android.view.LayoutInflater.from(context)
            .inflate(R.layout.widget_lock, null)
        assertNotNull(view.findViewById<android.view.View>(R.id.widget_root))
    }
}
