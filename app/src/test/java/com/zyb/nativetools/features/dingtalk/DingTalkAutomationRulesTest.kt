package com.zyb.nativetools.features.dingtalk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DingTalkAutomationRulesTest {
    @Test
    fun `only matches the target for the current step`() {
        assertEquals(
            "工作台",
            DingTalkAutomationRules.targetLabel(
                DingTalkAutomationStep.WORKBENCH,
                listOf("消息", "工作台", "我的"),
            ),
        )
        assertEquals(
            "考勤打卡",
            DingTalkAutomationRules.targetLabel(
                DingTalkAutomationStep.ATTENDANCE,
                listOf("考勤打卡", "审批"),
            ),
        )
    }

    @Test
    fun `does not match punch outside attendance page`() {
        assertNull(
            DingTalkAutomationRules.targetLabel(
                DingTalkAutomationStep.PUNCH,
                listOf("打卡", "消息", "通讯录"),
            ),
        )
    }

    @Test
    fun `matches exact punch action on attendance page`() {
        assertEquals(
            "打卡",
            DingTalkAutomationRules.targetLabel(
                DingTalkAutomationStep.PUNCH,
                listOf("考勤打卡", "上班打卡", "打卡"),
            ),
        )
    }

    @Test
    fun `does not use partial text for punch action`() {
        assertNull(
            DingTalkAutomationRules.targetLabel(
                DingTalkAutomationStep.PUNCH,
                listOf("考勤打卡", "更新打卡", "外勤打卡"),
            ),
        )
    }
}
