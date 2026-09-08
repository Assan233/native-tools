package com.zyb.nativetools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zyb.nativetools.features.dingtalk.DingTalkAutomationController
import com.zyb.nativetools.features.dingtalk.DingTalkAutomationSnapshot
import com.zyb.nativetools.features.dingtalk.DingTalkAutomationStatus
import com.zyb.nativetools.features.meiyou.AutomationSnapshot
import com.zyb.nativetools.features.meiyou.AutomationStatus
import com.zyb.nativetools.features.meiyou.MeiyouAutomationController
import com.zyb.nativetools.ui.theme.NativeToolsTheme

class MainActivity : ComponentActivity() {
    private var meiyouSnapshot by mutableStateOf(AutomationSnapshot())
    private var dingTalkSnapshot by mutableStateOf(DingTalkAutomationSnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshSnapshots()
        setContent {
            NativeToolsTheme {
                ToolsScreen(
                    meiyouSnapshot = meiyouSnapshot,
                    dingTalkSnapshot = dingTalkSnapshot,
                    onEnableMeiyouAccessibility = {
                        MeiyouAutomationController.openAccessibilitySettings(this)
                    },
                    onStartMeiyou = {
                        MeiyouAutomationController.start(this)
                        refreshSnapshots()
                    },
                    onStopMeiyou = {
                        MeiyouAutomationController.stop(this)
                        refreshSnapshots()
                    },
                    onEnableDingTalkAccessibility = {
                        DingTalkAutomationController.openAccessibilitySettings(this)
                    },
                    onStartDingTalk = {
                        DingTalkAutomationController.start(this)
                        refreshSnapshots()
                    },
                    onStopDingTalk = {
                        DingTalkAutomationController.stop(this)
                        refreshSnapshots()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSnapshots()
    }

    private fun refreshSnapshots() {
        meiyouSnapshot = MeiyouAutomationController.snapshot(this)
        dingTalkSnapshot = DingTalkAutomationController.snapshot(this)
    }
}

@Composable
private fun ToolsScreen(
    meiyouSnapshot: AutomationSnapshot,
    dingTalkSnapshot: DingTalkAutomationSnapshot,
    onEnableMeiyouAccessibility: () -> Unit,
    onStartMeiyou: () -> Unit,
    onStopMeiyou: () -> Unit,
    onEnableDingTalkAccessibility: () -> Unit,
    onStartDingTalk: () -> Unit,
    onStopDingTalk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(background),
    ) {
        DecorativeRing(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 92.dp, y = (-108).dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            ToolListIcon()
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.home_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(30.dp))

            MeiyouToolCard(
                snapshot = meiyouSnapshot,
                onEnableAccessibility = onEnableMeiyouAccessibility,
                onStart = onStartMeiyou,
                onStop = onStopMeiyou,
            )
            Spacer(Modifier.height(18.dp))
            DingTalkToolCard(
                snapshot = dingTalkSnapshot,
                onEnableAccessibility = onEnableDingTalkAccessibility,
                onStart = onStartDingTalk,
                onStop = onStopDingTalk,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.local_automation_note),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DingTalkToolCard(
    snapshot: DingTalkAutomationSnapshot,
    onEnableAccessibility: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    ClipboardIcon()
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tool_automation_label),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.dingtalk_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.dingtalk_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ToolMetric(
                    label = stringResource(R.string.dingtalk_path_label),
                    value = stringResource(R.string.dingtalk_path_value),
                    modifier = Modifier.weight(1f),
                )
                ToolMetric(
                    label = stringResource(R.string.dingtalk_action_label),
                    value = stringResource(R.string.dingtalk_action_value),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))
            DingTalkStatusLine(snapshot)
            Spacer(Modifier.height(18.dp))

            when {
                !snapshot.accessibilityEnabled -> {
                    PrimaryActionButton(
                        text = stringResource(R.string.enable_accessibility),
                        onClick = onEnableAccessibility,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.dingtalk_accessibility_explanation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }

                snapshot.status == DingTalkAutomationStatus.RUNNING -> {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = onStop,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(stringResource(R.string.stop_automation))
                    }
                }

                else -> PrimaryActionButton(
                    text = stringResource(R.string.start_dingtalk_punch),
                    onClick = onStart,
                )
            }
        }
    }
}

@Composable
private fun MeiyouToolCard(
    snapshot: AutomationSnapshot,
    onEnableAccessibility: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 5.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    ClipboardIcon()
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tool_automation_label),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.meiyou_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.meiyou_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp,
            )
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ToolMetric(
                    label = stringResource(R.string.default_amount_label),
                    value = stringResource(R.string.default_amount_value),
                    modifier = Modifier.weight(1f),
                )
                ToolMetric(
                    label = stringResource(R.string.save_mode_label),
                    value = stringResource(R.string.save_mode_value),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))
            StatusLine(snapshot)
            Spacer(Modifier.height(18.dp))

            when {
                !snapshot.accessibilityEnabled -> {
                    PrimaryActionButton(
                        text = stringResource(R.string.enable_accessibility),
                        onClick = onEnableAccessibility,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.accessibility_explanation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                    )
                }

                snapshot.status == AutomationStatus.RUNNING -> {
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        onClick = onStop,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(stringResource(R.string.stop_automation))
                    }
                }

                else -> PrimaryActionButton(
                    text = stringResource(R.string.start_meiyou_record),
                    onClick = onStart,
                )
            }
        }
    }
}

@Composable
private fun ToolMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatusLine(snapshot: AutomationSnapshot) {
    val statusColor = when {
        !snapshot.accessibilityEnabled -> MaterialTheme.colorScheme.tertiary
        snapshot.status == AutomationStatus.APP_NOT_INSTALLED -> MaterialTheme.colorScheme.error
        snapshot.status == AutomationStatus.PAGE_NOT_FOUND -> MaterialTheme.colorScheme.error
        snapshot.status == AutomationStatus.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = automationStatusText(snapshot),
            color = statusColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DingTalkStatusLine(snapshot: DingTalkAutomationSnapshot) {
    val statusColor = when {
        !snapshot.accessibilityEnabled -> MaterialTheme.colorScheme.tertiary
        snapshot.status == DingTalkAutomationStatus.APP_NOT_INSTALLED -> MaterialTheme.colorScheme.error
        snapshot.status == DingTalkAutomationStatus.PAGE_NOT_FOUND -> MaterialTheme.colorScheme.error
        snapshot.status == DingTalkAutomationStatus.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = dingTalkAutomationStatusText(snapshot),
            color = statusColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DecorativeRing(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(230.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape),
        )
    }
}

@Composable
private fun ToolListIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onBackground
    Canvas(modifier = modifier.size(width = 28.dp, height = 24.dp)) {
        val stroke = 2.3.dp.toPx()
        repeat(3) { index ->
            val y = (4 + index * 8).dp.toPx()
            drawLine(color, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}

@Composable
private fun ClipboardIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.size(28.dp)) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
            size = Size(18.dp.toPx(), 20.dp.toPx()),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(9.dp.toPx(), 2.dp.toPx()),
            size = Size(10.dp.toPx(), 6.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        repeat(3) { index ->
            val y = (12 + index * 4).dp.toPx()
            drawLine(
                color = color,
                start = Offset(10.dp.toPx(), y),
                end = Offset(19.dp.toPx(), y),
                strokeWidth = stroke,
            )
        }
    }
}

@Composable
private fun automationStatusText(snapshot: AutomationSnapshot): String = when {
    !snapshot.accessibilityEnabled -> stringResource(R.string.status_permission_required)
    snapshot.status == AutomationStatus.RUNNING -> stringResource(R.string.status_running)
    snapshot.status == AutomationStatus.READY -> stringResource(R.string.status_ready)
    snapshot.status == AutomationStatus.APP_NOT_INSTALLED -> stringResource(R.string.status_app_missing)
    snapshot.status == AutomationStatus.PAGE_NOT_FOUND -> stringResource(R.string.status_page_not_found)
    snapshot.status == AutomationStatus.STOPPED -> stringResource(R.string.status_stopped)
    else -> stringResource(R.string.status_ready_to_start)
}

@Composable
private fun dingTalkAutomationStatusText(snapshot: DingTalkAutomationSnapshot): String = when {
    !snapshot.accessibilityEnabled -> stringResource(R.string.dingtalk_status_permission_required)
    snapshot.status == DingTalkAutomationStatus.RUNNING -> stringResource(R.string.dingtalk_status_running)
    snapshot.status == DingTalkAutomationStatus.COMPLETED -> stringResource(R.string.dingtalk_status_completed)
    snapshot.status == DingTalkAutomationStatus.APP_NOT_INSTALLED -> stringResource(R.string.dingtalk_status_app_missing)
    snapshot.status == DingTalkAutomationStatus.PAGE_NOT_FOUND -> stringResource(R.string.dingtalk_status_page_not_found)
    snapshot.status == DingTalkAutomationStatus.STOPPED -> stringResource(R.string.dingtalk_status_stopped)
    else -> stringResource(R.string.dingtalk_status_ready)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ToolsScreenPreview() {
    NativeToolsTheme(darkTheme = false) {
        ToolsScreen(
            meiyouSnapshot = AutomationSnapshot(
                accessibilityEnabled = true,
                status = AutomationStatus.IDLE,
            ),
            dingTalkSnapshot = DingTalkAutomationSnapshot(
                accessibilityEnabled = true,
                status = DingTalkAutomationStatus.IDLE,
            ),
            onEnableMeiyouAccessibility = {},
            onStartMeiyou = {},
            onStopMeiyou = {},
            onEnableDingTalkAccessibility = {},
            onStartDingTalk = {},
            onStopDingTalk = {},
        )
    }
}
