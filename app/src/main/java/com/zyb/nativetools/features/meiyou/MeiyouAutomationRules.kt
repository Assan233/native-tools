package com.zyb.nativetools.features.meiyou

internal enum class NavigationAction(val labels: List<String>) {
    BOTTLE_BREAST_MILK(listOf("瓶喂母乳", "母乳瓶喂")),
    BOTTLE_FEEDING(listOf("瓶喂", "奶瓶喂养")),
    FEEDING(listOf("喂养", "喂奶")),
    RECORD(listOf("记录", "添加记录", "记一记")),
}

internal object MeiyouAutomationRules {
    fun nextAction(visibleTexts: Collection<String>): NavigationAction? {
        val normalizedTexts = visibleTexts.map(::normalize)
        return NavigationAction.entries.firstOrNull { action ->
            action.labels.any { label -> normalize(label) in normalizedTexts }
        }
    }

    fun normalize(value: String): String = value
        .replace(Regex("\\s+"), "")
        .lowercase()
}
