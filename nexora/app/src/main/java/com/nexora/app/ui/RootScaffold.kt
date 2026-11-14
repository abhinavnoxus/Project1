package com.nexora.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.NexoraBackdrop

@Composable
fun RootScaffold(
    viewModel: NexoraViewModel,
    spec: GlassSpec,
    pendingAlarmId: Long?,
    pendingTriggerAt: Long?,
    pendingTaskId: Long?,
    pendingRoute: String?,
    onConsumePending: () -> Unit,
) {
    NexoraBackdrop(spec = spec) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("NEXORA", color = spec.textColor)
        }
    }
}
