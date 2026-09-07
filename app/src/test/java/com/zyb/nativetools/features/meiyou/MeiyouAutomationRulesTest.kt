package com.zyb.nativetools.features.meiyou

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeiyouAutomationRulesTest {
    @Test
    fun `only matches the target for the current navigation step`() {
        val feedingRecord = MeiyouAutomationRules.targetLabel(
            AutomationStep.FEEDING_RECORD,
            listOf("瓶喂母乳", "喂养记录"),
        )
        val bottleBreastMilk = MeiyouAutomationRules.targetLabel(
            AutomationStep.BOTTLE_BREAST_MILK,
            listOf("喂养记录", "瓶喂母乳"),
        )

        assertEquals("喂养记录", feedingRecord)
        assertEquals("瓶喂母乳", bottleBreastMilk)
    }

    @Test
    fun `normalizes whitespace before matching`() {
        val label = MeiyouAutomationRules.targetLabel(
            AutomationStep.BOTTLE_BREAST_MILK,
            listOf("瓶喂 母乳"),
        )

        assertEquals("瓶喂母乳", label)
    }

    @Test
    fun `returns null for an unexpected page`() {
        assertNull(
            MeiyouAutomationRules.targetLabel(
                AutomationStep.FEEDING_RECORD,
                listOf("社区", "我的"),
            ),
        )
    }

    @Test
    fun `parses milk amount labels`() {
        assertEquals(150, MeiyouAutomationRules.parseAmount("150 ml"))
        assertEquals(90, MeiyouAutomationRules.parseAmount("90毫升"))
        assertNull(MeiyouAutomationRules.parseAmount("奶量"))
    }

    @Test
    fun `chooses scroll direction from current amount`() {
        assertEquals(
            AmountAdjustment.SCROLL_FORWARD,
            MeiyouAutomationRules.amountAdjustment(current = 120, target = 150),
        )
        assertEquals(
            AmountAdjustment.SCROLL_BACKWARD,
            MeiyouAutomationRules.amountAdjustment(current = 180, target = 150),
        )
        assertEquals(
            AmountAdjustment.DONE,
            MeiyouAutomationRules.amountAdjustment(current = 150, target = 150),
        )
    }
}
