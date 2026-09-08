package com.zyb.nativetools.features.meiyou

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.zyb.nativetools.R
import kotlin.math.abs

class MeiyouAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = AutomationStep.FEEDING_RECORD
    private var activeRunStartedAt = 0L
    private var lastActionAt = 0L

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
            AutomationStep.MILK_AMOUNT -> inputAmount(root)
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

    private fun inputAmount(root: AccessibilityNodeInfo) {
        val nodes = root.flatten().filter(AccessibilityNodeInfo::isVisibleToUser)
        val amountLabel = nodes.firstOrNull { node ->
            node.nodeLabel()?.let { MeiyouAutomationRules.targetLabel(currentStep, listOf(it)) } != null
        } ?: return
        val amountInput = findAmountInput(amountLabel) ?: return
        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                MeiyouAutomationController.DEFAULT_MILK_ML.toString(),
            )
        }
        if (amountInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
            finish(AutomationStatus.READY, R.string.automation_ready_toast)
        }
    }

    private fun findAmountInput(amountLabel: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (amountLabel.supportsTextInput()) return amountLabel

        var container: AccessibilityNodeInfo? = amountLabel.parent
        repeat(MAX_ANCESTOR_SEARCH_DEPTH) {
            val localInput = container
                ?.flatten()
                ?.filter(AccessibilityNodeInfo::isVisibleToUser)
                ?.filter { it.supportsTextInput() }
                ?.minByOrNull { it.distanceFrom(amountLabel) }
            if (localInput != null) return localInput
            container = container?.parent
        }
        return null
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

    private fun AccessibilityNodeInfo.supportsTextInput(): Boolean =
        isEditable || actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }

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
        const val ACTION_DEBOUNCE_MILLIS = 400L
        const val MAX_ANCESTOR_SEARCH_DEPTH = 4
    }
}
