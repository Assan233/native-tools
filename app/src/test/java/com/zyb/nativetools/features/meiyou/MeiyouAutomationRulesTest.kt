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
    fun `matches milk amount input variants`() {
        assertEquals(
            "输入奶量",
            MeiyouAutomationRules.targetLabel(
                AutomationStep.MILK_AMOUNT,
                listOf("输入奶量"),
            ),
        )
        assertEquals(
            "请输入奶量",
            MeiyouAutomationRules.targetLabel(
                AutomationStep.MILK_AMOUNT,
                listOf("请输入奶量"),
            ),
        )
    }
}
