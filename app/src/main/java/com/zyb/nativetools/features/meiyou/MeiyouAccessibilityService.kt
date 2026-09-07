package com.zyb.nativetools.features.meiyou

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.zyb.nativetools.R

class MeiyouAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var amountPageReached = false
    private var activeRunStartedAt = 0L
    private var lastActionAt = 0L

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
            amountPageReached = false
            lastActionAt = 0L
        }
        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed >= TIMEOUT_MILLIS) {
            finish(AutomationStatus.PAGE_NOT_FOUND, R.string.automation_page_not_found_toast)
            return
        }

        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MILLIS - elapsed)
        if (System.currentTimeMillis() - lastActionAt < ACTION_DEBOUNCE_MILLIS) return

        val root = rootInActiveWindow ?: return
        if (amountPageReached) {
            fillAmount(root)
        } else {
            navigate(root)
        }
    }

    override fun onInterrupt() {
        handler.removeCallbacks(timeout)
        if (MeiyouAutomationController.readStatus(this) == AutomationStatus.RUNNING) {
            MeiyouAutomationController.updateStatus(this, AutomationStatus.STOPPED)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        super.onDestroy()
    }

    private fun navigate(root: AccessibilityNodeInfo) {
        val nodes = root.flatten()
        val action = MeiyouAutomationRules.nextAction(nodes.mapNotNull { it.nodeLabel() })
            ?: return
        val target = nodes.firstOrNull { node ->
            val label = node.nodeLabel() ?: return@firstOrNull false
            action.labels.any { candidate ->
                MeiyouAutomationRules.normalize(label) == MeiyouAutomationRules.normalize(candidate)
            }
        } ?: return

        if (target.click()) {
            lastActionAt = System.currentTimeMillis()
            if (action == NavigationAction.BOTTLE_BREAST_MILK) {
                amountPageReached = true
            }
        }
    }

    private fun fillAmount(root: AccessibilityNodeInfo) {
        val nodes = root.flatten()
        val presetLabels = listOf("150", "150ml", "150 ml", "150毫升")
        val preset = nodes.firstOrNull { node ->
            val label = node.nodeLabel() ?: return@firstOrNull false
            presetLabels.any { MeiyouAutomationRules.normalize(it) == MeiyouAutomationRules.normalize(label) }
        }
        if (preset?.click() == true) {
            finish(AutomationStatus.READY, R.string.automation_ready_toast)
            return
        }

        val editableNodes = nodes.filter { node ->
            node.isVisibleToUser &&
                (node.isEditable || node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT })
        }
        val amountInput = editableNodes.firstOrNull { it.looksLikeAmountInput() }
            ?: editableNodes.singleOrNull()
            ?: return
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

    private fun finish(status: AutomationStatus, messageResource: Int) {
        handler.removeCallbacks(timeout)
        amountPageReached = false
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

    private fun AccessibilityNodeInfo.looksLikeAmountInput(): Boolean {
        val searchable = listOfNotNull(text, hintText, contentDescription, viewIdResourceName)
            .joinToString(separator = " ")
            .lowercase()
        return listOf("ml", "毫升", "奶量", "amount", "volume").any(searchable::contains)
    }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val ACTION_DEBOUNCE_MILLIS = 500L
    }
}
