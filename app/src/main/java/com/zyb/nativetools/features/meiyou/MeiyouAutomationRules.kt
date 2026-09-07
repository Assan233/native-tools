package com.zyb.nativetools.features.meiyou

internal enum class AutomationStep(val labels: List<String>) {
    FEEDING_RECORD(listOf("喂养记录")),
    BOTTLE_BREAST_MILK(listOf("瓶喂母乳", "母乳瓶喂")),
    MILK_AMOUNT(listOf("奶量")),
}

internal enum class AmountAdjustment {
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    DONE,
}

internal object MeiyouAutomationRules {
    fun targetLabel(step: AutomationStep, visibleTexts: Collection<String>): String? {
        val normalizedTexts = visibleTexts.map(::normalize)
        return step.labels.firstOrNull { label ->
            normalize(label) in normalizedTexts
        }
    }

    fun amountAdjustment(current: Int, target: Int): AmountAdjustment = when {
        current < target -> AmountAdjustment.SCROLL_FORWARD
        current > target -> AmountAdjustment.SCROLL_BACKWARD
        else -> AmountAdjustment.DONE
    }

    fun parseAmount(value: String): Int? = AMOUNT_PATTERN
        .find(normalize(value))
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()

    fun normalize(value: String): String = value
        .replace(Regex("\\s+"), "")
        .lowercase()

    private val AMOUNT_PATTERN = Regex("(?:^|\\D)(\\d{1,3})(?:ml|毫升)?(?:$|\\D)")
}
