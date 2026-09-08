package com.zyb.nativetools.features.dingtalk

internal enum class DingTalkAutomationStep(val labels: List<String>) {
    WORKBENCH(listOf("工作台")),
    ATTENDANCE(listOf("考勤打卡")),
    PUNCH(listOf("打卡")),
}

internal object DingTalkAutomationRules {
    private val punchPageLabels = listOf("考勤打卡", "上班打卡", "下班打卡")

    fun targetLabel(step: DingTalkAutomationStep, visibleTexts: Collection<String>): String? {
        val normalizedTexts = visibleTexts.map(::normalize)
        if (step == DingTalkAutomationStep.PUNCH && punchPageLabels.none { label ->
                normalize(label) in normalizedTexts
            }
        ) {
            return null
        }

        return step.labels.firstOrNull { label -> normalize(label) in normalizedTexts }
    }

    fun normalize(value: String): String = value
        .replace(Regex("\\s+"), "")
        .lowercase()
}
