package com.zyb.nativetools.features.meiyou

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeiyouAutomationRulesTest {
    @Test
    fun `prefers bottle breast milk when several actions are visible`() {
        val action = MeiyouAutomationRules.nextAction(listOf("记录", "喂养", "瓶喂母乳"))

        assertEquals(NavigationAction.BOTTLE_BREAST_MILK, action)
    }

    @Test
    fun `normalizes whitespace before matching`() {
        val action = MeiyouAutomationRules.nextAction(listOf("瓶喂 母乳"))

        assertEquals(NavigationAction.BOTTLE_BREAST_MILK, action)
    }

    @Test
    fun `returns null for an unexpected page`() {
        assertNull(MeiyouAutomationRules.nextAction(listOf("社区", "我的")))
    }
}
