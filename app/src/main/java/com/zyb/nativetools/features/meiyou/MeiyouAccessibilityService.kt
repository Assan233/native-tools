package com.zyb.nativetools.features.meiyou

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.zyb.nativetools.R
import kotlin.math.abs
import kotlin.math.roundToInt

class MeiyouAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = AutomationStep.FEEDING_RECORD
    private var activeRunStartedAt = 0L
    private var lastActionAt = 0L
    private var amountScrollCount = 0

    private val processWindow = Runnable { processCurrentWindow() }

    private val timeout = Runnable {
        if (MeiyouAutomationController.readStatus(this) == AutomationStatus.RUNNING) {
            finish(AutomationStatus.PAGE_NOT_FOUND, R.string.automation_page_not_found_toast)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != MeiyouAutomationController.MEIYOU_PACKAGE) return
        if (MeiyouAutomationController.readStatus(this) != AutomationStatus.RUNNING) return

        val startedAt = MeiyouAutomationController.startedAt(this)
        if (startedAt != activeRunStartedAt) {
            activeRunStartedAt = startedAt
            currentStep = AutomationStep.FEEDING_RECORD
            lastActionAt = 0L
            amountScrollCount = 0
        }
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed >= TIMEOUT_MILLIS) {
            finish(AutomationStatus.PAGE_NOT_FOUND, R.string.automation_page_not_found_toast)
            return
        }

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MILLIS - elapsed)
        handler.removeCallbacks(processWindow)
        handler.postDelayed(processWindow, CONTENT_SETTLE_MILLIS)
    }

    private fun processCurrentWindow() {
        if (MeiyouAutomationController.readStatus(this) != AutomationStatus.RUNNING) return
        if (System.currentTimeMillis() - lastActionAt < ACTION_DEBOUNCE_MILLIS) {
            scheduleProcess(ACTION_DEBOUNCE_MILLIS)
            return
        }

        val root = rootInActiveWindow ?: return
        when (currentStep) {
            AutomationStep.FEEDING_RECORD,
            AutomationStep.BOTTLE_BREAST_MILK,
            -> navigate(root)
            AutomationStep.MILK_AMOUNT -> adjustAmount(root)
        }
    }

    override fun onInterrupt() {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(processWindow)
        if (MeiyouAutomationController.readStatus(this) == AutomationStatus.RUNNING) {
            MeiyouAutomationController.updateStatus(this, AutomationStatus.STOPPED)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(processWindow)
        super.onDestroy()
    }

    private fun navigate(root: AccessibilityNodeInfo) {
        val nodes = root.flatten().filter(AccessibilityNodeInfo::isVisibleToUser)
        val targetLabel = MeiyouAutomationRules.targetLabel(
            currentStep,
            nodes.mapNotNull { it.nodeLabel() },
        ) ?: return
        val target = nodes.firstOrNull { node ->
            val label = node.nodeLabel() ?: return@firstOrNull false
            MeiyouAutomationRules.normalize(label) == MeiyouAutomationRules.normalize(targetLabel)
        } ?: return

        if (target.click()) {
            lastActionAt = System.currentTimeMillis()
            currentStep = when (currentStep) {
                AutomationStep.FEEDING_RECORD -> AutomationStep.BOTTLE_BREAST_MILK
                AutomationStep.BOTTLE_BREAST_MILK -> AutomationStep.MILK_AMOUNT
                AutomationStep.MILK_AMOUNT -> AutomationStep.MILK_AMOUNT
            }
            scheduleProcess(PAGE_LOAD_WAIT_MILLIS)
        }
    }

    private fun adjustAmount(root: AccessibilityNodeInfo) {
        val nodes = root.flatten().filter(AccessibilityNodeInfo::isVisibleToUser)
        val amountLabel = nodes.firstOrNull { node ->
            node.nodeLabel()?.let { MeiyouAutomationRules.targetLabel(currentStep, listOf(it)) } != null
        } ?: return
        val picker = findAmountPicker(amountLabel, nodes) ?: return
        val currentAmount = picker.currentAmount() ?: return

        val adjustment = MeiyouAutomationRules.amountAdjustment(
            currentAmount,
            MeiyouAutomationController.DEFAULT_MILK_ML,
        )
        if (adjustment == AmountAdjustment.DONE) {
            finish(AutomationStatus.READY, R.string.automation_ready_toast)
            return
        }

        if (amountScrollCount >= MAX_AMOUNT_SCROLLS) return
        val action = when (adjustment) {
            AmountAdjustment.SCROLL_FORWARD -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            AmountAdjustment.SCROLL_BACKWARD -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            AmountAdjustment.DONE -> return
        }
        if (picker.performAction(action)) {
            amountScrollCount += 1
            lastActionAt = System.currentTimeMillis()
            scheduleProcess(PICKER_SETTLE_MILLIS)
        }
    }

    private fun findAmountPicker(
        amountLabel: AccessibilityNodeInfo,
        visibleNodes: List<AccessibilityNodeInfo>,
    ): AccessibilityNodeInfo? {
        var container: AccessibilityNodeInfo? = amountLabel.parent
        repeat(MAX_ANCESTOR_SEARCH_DEPTH) {
            val localPicker = container
                ?.flatten()
                ?.filter(AccessibilityNodeInfo::isVisibleToUser)
                ?.filter { it.supportsAmountScroll() }
                ?.minByOrNull { it.distanceFrom(amountLabel) }
            if (localPicker != null) return localPicker
            container = container?.parent
        }
        return visibleNodes
            .filter { it.supportsAmountScroll() }
            .minByOrNull { it.distanceFrom(amountLabel) }
    }

    private fun AccessibilityNodeInfo.currentAmount(): Int? {
        rangeInfo?.current?.roundToInt()?.let { return it }
        nodeLabel()?.let(MeiyouAutomationRules::parseAmount)?.let { return it }
        flatten()
            .filter(AccessibilityNodeInfo::isVisibleToUser)
            .firstOrNull { it.isSelected }
            ?.nodeLabel()
            ?.let(MeiyouAutomationRules::parseAmount)
            ?.let { return it }

        val pickerBounds = Rect().also(::getBoundsInScreen)
        return flatten()
            .filter(AccessibilityNodeInfo::isVisibleToUser)
            .mapNotNull { node ->
                val value = node.nodeLabel()?.let(MeiyouAutomationRules::parseAmount)
                    ?: return@mapNotNull null
                val bounds = Rect().also(node::getBoundsInScreen)
                value to abs(bounds.centerY() - pickerBounds.centerY())
            }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun finish(status: AutomationStatus, messageResource: Int) {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(processWindow)
        currentStep = AutomationStep.FEEDING_RECORD
        MeiyouAutomationController.updateStatus(this, status)
        Toast.makeText(this, messageResource, Toast.LENGTH_LONG).show()
    }

    private fun AccessibilityNodeInfo.flatten(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(this)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            result.add(node)
            repeat(node.childCount) { index -> node.getChild(index)?.let(pending::addLast) }
        }
        return result
    }

    private fun AccessibilityNodeInfo.nodeLabel(): String? =
        text?.toString()?.takeIf(String::isNotBlank)
            ?: contentDescription?.toString()?.takeIf(String::isNotBlank)

    private fun AccessibilityNodeInfo.click(): Boolean {
        var target: AccessibilityNodeInfo? = this
        while (target != null) {
            if (target.isClickable && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            target = target.parent
        }
        return false
    }

    private fun AccessibilityNodeInfo.supportsAmountScroll(): Boolean =
        isScrollable || actionList.any { action ->
            action.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ||
                action.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }

    private fun AccessibilityNodeInfo.distanceFrom(other: AccessibilityNodeInfo): Int {
        val bounds = Rect().also(::getBoundsInScreen)
        val otherBounds = Rect().also(other::getBoundsInScreen)
        return abs(bounds.centerX() - otherBounds.centerX()) +
            abs(bounds.centerY() - otherBounds.centerY())
    }

    private fun scheduleProcess(delayMillis: Long) {
        handler.removeCallbacks(processWindow)
        handler.postDelayed(processWindow, delayMillis)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
        const val CONTENT_SETTLE_MILLIS = 350L
        const val PAGE_LOAD_WAIT_MILLIS = 1_000L
        const val PICKER_SETTLE_MILLIS = 450L
        const val ACTION_DEBOUNCE_MILLIS = 400L
        const val MAX_AMOUNT_SCROLLS = 40
        const val MAX_ANCESTOR_SEARCH_DEPTH = 4
    }
}
