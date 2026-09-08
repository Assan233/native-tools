package com.zyb.nativetools.features.meiyou

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

enum class AutomationStatus {
    IDLE,
    RUNNING,
    READY,
    APP_NOT_INSTALLED,
    PAGE_NOT_FOUND,
    STOPPED,
}

data class AutomationSnapshot(
    val accessibilityEnabled: Boolean = false,
    val status: AutomationStatus = AutomationStatus.IDLE,
)

object MeiyouAutomationController {
    const val MEIYOU_PACKAGE = "com.lingan.seeyou"
    const val DEFAULT_MILK_ML = 150

    private const val PREFERENCES_NAME = "meiyou_automation"
    private const val STATUS_KEY = "status"
    private const val STARTED_AT_KEY = "started_at"

    fun snapshot(context: Context): AutomationSnapshot = AutomationSnapshot(
        accessibilityEnabled = isAccessibilityEnabled(context),
        status = readStatus(context),
    )

    fun start(context: Context) {
        if (!isAccessibilityEnabled(context)) return

        val launchIntent = context.packageManager.getLaunchIntentForPackage(MEIYOU_PACKAGE)
        if (launchIntent == null) {
            updateStatus(context, AutomationStatus.APP_NOT_INSTALLED)
            return
        }

        preferences(context).edit()
            .putString(STATUS_KEY, AutomationStatus.RUNNING.name)
            .putLong(STARTED_AT_KEY, System.currentTimeMillis())
            .apply()
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun stop(context: Context) {
        updateStatus(context, AutomationStatus.STOPPED)
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    internal fun readStatus(context: Context): AutomationStatus {
        val value = preferences(context).getString(STATUS_KEY, null)
        return value?.let { runCatching { AutomationStatus.valueOf(it) }.getOrNull() }
            ?: AutomationStatus.IDLE
    }

    internal fun updateStatus(context: Context, status: AutomationStatus) {
        preferences(context).edit().putString(STATUS_KEY, status.name).apply()
    }

    internal fun startedAt(context: Context): Long =
        preferences(context).getLong(STARTED_AT_KEY, 0L)

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val expectedComponent = ComponentName(context, MeiyouAccessibilityService::class.java)
        val serviceIsRunning = manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { service ->
                val info = service.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expectedComponent
            }
        if (serviceIsRunning) return true

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        return isServiceEnabledInSettings(
            enabledServices = enabledServices,
            expectedPackageName = expectedComponent.packageName,
            expectedClassName = expectedComponent.className,
        )
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    internal fun isServiceEnabledInSettings(
        enabledServices: String?,
        expectedPackageName: String,
        expectedClassName: String,
    ): Boolean = enabledServices
        .orEmpty()
        .split(':')
        .any { flattenedComponent ->
            val separatorIndex = flattenedComponent.indexOf('/')
            if (separatorIndex <= 0) return@any false

            val packageName = flattenedComponent.substring(0, separatorIndex)
            val declaredClassName = flattenedComponent.substring(separatorIndex + 1)
            val className = if (declaredClassName.startsWith('.')) {
                packageName + declaredClassName
            } else {
                declaredClassName
            }
            packageName == expectedPackageName && className == expectedClassName
        }
}
