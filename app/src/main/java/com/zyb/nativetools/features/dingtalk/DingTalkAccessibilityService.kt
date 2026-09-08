package com.zyb.nativetools.features.dingtalk

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.zyb.nativetools.R

class DingTalkAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = DingTalkAutomationStep.WORKBENCH
    private var activeRunStartedAt = 0L
    private var lastActionAt = 0L

    private val processWindow = Runnable { processCurrentWindow() }
    private val timeout = Runnable {
        if (DingTalkAutomationController.readStatus(this) == DingTalkAutomationStatus.RUNNING) {
            finish(
                DingTalkAutomationStatus.PAGE_NOT_FOUND,
                R.string.dingtalk_page_not_found_toast,
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != DingTalkAutomationController.DINGTALK_PACKAGE) return
        if (DingTalkAutomationController.readStatus(this) != DingTalkAutomationStatus.RUNNING) return

        val startedAt = DingTalkAutomationController.startedAt(this)
        if (startedAt != activeRunStartedAt) {
            activeRunStartedAt = startedAt
            currentStep = DingTalkAutomationStep.WORKBENCH
            lastActionAt = 0L
        }
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed >= TIMEOUT_MILLIS) {
            finish(
                DingTalkAutomationStatus.PAGE_NOT_FOUND,
                R.string.dingtalk_page_not_found_toast,
            )
            return
        }

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MILLIS - elapsed)
        handler.removeCallbacks(processWindow)
        handler.postDelayed(processWindow, CONTENT_SETTLE_MILLIS)
    }

    override fun onInterrupt() {
        clearCallbacks()
        if (DingTalkAutomationController.readStatus(this) == DingTalkAutomationStatus.RUNNING) {
            DingTalkAutomationController.updateStatus(this, DingTalkAutomationStatus.STOPPED)
        }
    }

    override fun onDestroy() {
        clearCallbacks()
        super.onDestroy()
    }

    private fun processCurrentWindow() {
        if (DingTalkAutomationController.readStatus(this) != DingTalkAutomationStatus.RUNNING) return
        if (System.currentTimeMillis() - lastActionAt < ACTION_DEBOUNCE_MILLIS) {
            scheduleProcess(ACTION_DEBOUNCE_MILLIS)
            return
        }

        val root = rootInActiveWindow ?: return
        val nodes = root.flatten().filter(AccessibilityNodeInfo::isVisibleToUser)
        val targetLabel = DingTalkAutomationRules.targetLabel(
            currentStep,
            nodes.mapNotNull { it.nodeLabel() },
        ) ?: return
        val target = nodes.firstOrNull { node ->
            node.nodeLabel()?.let(DingTalkAutomationRules::normalize) ==
                DingTalkAutomationRules.normalize(targetLabel)
        } ?: return

        if (!target.click()) return

        lastActionAt = System.currentTimeMillis()
        if (currentStep == DingTalkAutomationStep.PUNCH) {
            finish(DingTalkAutomationStatus.COMPLETED, R.string.dingtalk_completed_toast)
            return
        }
        currentStep = when (currentStep) {
            DingTalkAutomationStep.WORKBENCH -> DingTalkAutomationStep.ATTENDANCE
            DingTalkAutomationStep.ATTENDANCE -> DingTalkAutomationStep.PUNCH
            DingTalkAutomationStep.PUNCH -> DingTalkAutomationStep.PUNCH
        }
        scheduleProcess(PAGE_LOAD_WAIT_MILLIS)
    }

    private fun finish(status: DingTalkAutomationStatus, messageResource: Int) {
        clearCallbacks()
        currentStep = DingTalkAutomationStep.WORKBENCH
        DingTalkAutomationController.updateStatus(this, status)
        Toast.makeText(this, messageResource, Toast.LENGTH_LONG).show()
    }

    private fun clearCallbacks() {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(processWindow)
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
        repeat(MAX_CLICK_ANCESTOR_DEPTH) {
            if (target?.isClickable == true &&
                target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            ) {
                return true
            }
            target = target?.parent
        }
        return false
    }

    private fun scheduleProcess(delayMillis: Long) {
        handler.removeCallbacks(processWindow)
        handler.postDelayed(processWindow, delayMillis)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
        const val CONTENT_SETTLE_MILLIS = 350L
        const val PAGE_LOAD_WAIT_MILLIS = 1_200L
        const val ACTION_DEBOUNCE_MILLIS = 400L
        const val MAX_CLICK_ANCESTOR_DEPTH = 5
    }
}
