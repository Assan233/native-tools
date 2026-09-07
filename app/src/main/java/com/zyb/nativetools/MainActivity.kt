package com.zyb.nativetools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zyb.nativetools.features.meiyou.AutomationSnapshot
import com.zyb.nativetools.features.meiyou.AutomationStatus
import com.zyb.nativetools.features.meiyou.MeiyouAutomationController
import com.zyb.nativetools.ui.theme.NativeToolsTheme

class MainActivity : ComponentActivity() {
    private var automationSnapshot by mutableStateOf(AutomationSnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        automationSnapshot = MeiyouAutomationController.snapshot(this)
        setContent {
            NativeToolsTheme {
                ToolsScreen(
                    automationSnapshot = automationSnapshot,
                    onEnableAccessibility = {
                        MeiyouAutomationController.openAccessibilitySettings(this)
                    },
                    onStart = {
                        MeiyouAutomationController.start(this)
                        automationSnapshot = MeiyouAutomationController.snapshot(this)
                    },
                    onStop = {
                        MeiyouAutomationController.stop(this)
                        automationSnapshot = MeiyouAutomationController.snapshot(this)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        automationSnapshot = MeiyouAutomationController.snapshot(this)
    }
}

@Composable
private fun ToolsScreen(
    automationSnapshot: AutomationSnapshot,
    onEnableAccessibility: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = innerPadding.calculateTopPadding() + 24.dp,
                end = 20.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                MeiyouToolCard(
                    snapshot = automationSnapshot,
                    onEnableAccessibility = onEnableAccessibility,
                    onStart = onStart,
                    onStop = onStop,
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
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.meiyou_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.meiyou_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = automationStatusText(snapshot),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )

            if (!snapshot.accessibilityEnabled) {
                Button(onClick = onEnableAccessibility) {
                    Text(stringResource(R.string.enable_accessibility))
                }
                Text(
                    text = stringResource(R.string.accessibility_explanation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStart,
                    enabled = snapshot.status != AutomationStatus.RUNNING,
                ) {
                    Text(stringResource(R.string.start_meiyou_record))
                }
            }

            if (snapshot.status == AutomationStatus.RUNNING) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStop,
                ) {
                    Text(stringResource(R.string.stop_automation))
                }
            }
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

@Preview(showBackground = true)
@Composable
private fun ToolsScreenPreview() {
    NativeToolsTheme {
        ToolsScreen(
            automationSnapshot = AutomationSnapshot(),
            onEnableAccessibility = {},
            onStart = {},
            onStop = {},
        )
    }
}
