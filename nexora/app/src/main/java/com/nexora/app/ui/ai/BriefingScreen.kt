package com.nexora.app.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nexora.app.design.GlassSpec
import com.nexora.app.design.GlassSurface
import com.nexora.app.domain.ai.Briefing
import com.nexora.app.ui.common.GlassPanel
import com.nexora.app.ui.common.PillTag

/**
 * NEXORA INTELLIGENCE — the briefing screen.
 *
 * The UI is built around one promise: the user can always tell which sentence is a
 * stored fact and which is a rule-based suggestion. Suggestions are visually separated
 * and never presented as something NEXORA has already done.
 */
@Composable
fun BriefingScreen(
    briefing: Briefing.DailyBriefing,
    spec: GlassSpec,
    onWriteOwn: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(CircleShape).background(spec.accentColor.copy(alpha = 0.16f)).padding(10.dp),
                ) { Icon(Icons.Rounded.AutoAwesome, null, tint = spec.accentColor) }
                Column {
                    Text("Briefing", style = MaterialTheme.typography.headlineSmall, color = spec.textColor)
                    Text("Computed on device, offline", style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                }
            }
        }

        item {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = 18.dp) {
                Column {
                    Text(briefing.headline, style = MaterialTheme.typography.titleLarge, color = spec.textColor)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PillTag("Offline", highlighted = true)
                        if (briefing.conflicts > 0) PillTag("${briefing.conflicts} conflicts")
                        PillTag("Rule-based")
                    }
                }
            }
        }

        item {
            GlassPanel(title = "What is happening", subtitle = "Read from your own data") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    briefing.facts.forEach { line ->
                        Row {
                            Box(Modifier.padding(top = 6.dp).width(6.dp).height(6.dp).clip(CircleShape).background(spec.accentColor))
                            Spacer(Modifier.width(10.dp))
                            Text(line.text, style = MaterialTheme.typography.bodyMedium, color = spec.textColor)
                        }
                    }
                }
            }
        }

        val suggestions = briefing.suggestions
        if (suggestions.isNotEmpty()) {
            item {
                GlassPanel(
                    title = "Suggestions",
                    subtitle = "Not applied — nothing changes without your tap",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        suggestions.forEach { line ->
                            GlassSurface(
                                Modifier.fillMaxWidth(),
                                contentPadding = 12.dp,
                                elevated = false,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            ) {
                                Row {
                                    Icon(
                                        Icons.Rounded.Info, null,
                                        tint = spec.accentColor,
                                        modifier = Modifier.padding(top = 2.dp).width(16.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(line.text, style = MaterialTheme.typography.bodySmall, color = spec.textColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(title = "What NEXORA will never do") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Create, move or delete an alarm, reminder or calendar event on its own.",
                        "Send your calendar, tasks or focus history anywhere — the app has no internet permission.",
                        "Present a guess as a measurement. Anything not stored is labelled a suggestion.",
                    ).forEach { line ->
                        Row {
                            Text("•", color = spec.accentColor)
                            Spacer(Modifier.width(8.dp))
                            Text(line, style = MaterialTheme.typography.bodySmall, color = spec.secondaryTextColor)
                        }
                    }
                }
            }
        }

        item {
            GlassPanel(title = "Focus on what matters") {
                Text(
                    "The briefing above is generated by local rules over your alarms, calendar, tasks, " +
                        "routines and focus sessions. No model, no server, no account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = spec.secondaryTextColor,
                )
            }
        }
    }
}
