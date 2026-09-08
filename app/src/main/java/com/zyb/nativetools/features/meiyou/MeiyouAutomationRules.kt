package com.zyb.nativetools.features.meiyou

internal enum class AutomationStep(val labels: List<String>) {
    FEEDING_RECORD(listOf("喂养记录")),
    BOTTLE_BREAST_MILK(listOf("瓶喂母乳", "母乳瓶喂")),
    MILK_AMOUNT(listOf("输入奶量", "请输入奶量", "奶量")),
}

internal object MeiyouAutomationRules {
    fun targetLabel(step: AutomationStep, visibleTexts: Collection<String>): String? {
        val normalizedTexts = visibleTexts.map(::normalize)
        return step.labels.firstOrNull { label ->
            normalize(label) in normalizedTexts
        }
    }

    fun normalize(value: String): String = value
        .replace(Regex("\\s+"), "")
        .lowercase()
}
