package com.zyb.nativetools.features.meiyou

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeiyouAutomationControllerTest {
    private val packageName = "com.zyb.nativetools"
    private val className =
        "com.zyb.nativetools.features.meiyou.MeiyouAccessibilityService"

    @Test
    fun `finds service with abbreviated class name`() {
        val enabledServices =
            "com.example/.OtherService:$packageName/.features.meiyou.MeiyouAccessibilityService"

        assertTrue(
            MeiyouAutomationController.isServiceEnabledInSettings(
                enabledServices,
                packageName,
                className,
            ),
        )
    }

    @Test
    fun `finds service with fully qualified class name`() {
        val enabledServices = "$packageName/$className"

        assertTrue(
            MeiyouAutomationController.isServiceEnabledInSettings(
                enabledServices,
                packageName,
                className,
            ),
        )
    }

    @Test
    fun `does not match another service`() {
        assertFalse(
            MeiyouAutomationController.isServiceEnabledInSettings(
                "com.example/.OtherService",
                packageName,
                className,
            ),
        )
    }
}
