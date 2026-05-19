package com.gtd.android.ui.context

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ThemeOption(val key: String, val label: String, val color: Color, val textColor: Color)
private data class IconOption(val key: String, val emoji: String)

private val THEMES = listOf(
    ThemeOption("MINIMALIST", "Minimal", Color(0xFFF5F5F5), Color.Black),
    ThemeOption("DESIGN", "Design", Color(0xFF7C3AED), Color.White),
    ThemeOption("FORMAL", "Formal", Color(0xFF1F2937), Color.White),
    ThemeOption("NATURE", "Nature", Color(0xFF059669), Color.White),
    ThemeOption("DARK", "Dark", Color(0xFF18181B), Color.White),
    ThemeOption("DRAGONS", "Dragons", Color(0xFF78350F), Color(0xFFFBBF24)),
    ThemeOption("ICE_DRAGONS", "Ice", Color(0xFF0C4A6E), Color(0xFF7DD3FC)),
)

private val ICONS = listOf(
    IconOption("briefcase", "\uD83D\uDCBC"),
    IconOption("home", "\uD83C\uDFE0"),
    IconOption("book", "\uD83D\uDCDA"),
    IconOption("heart", "❤\uFE0F"),
    IconOption("star", "⭐"),
    IconOption("rocket", "\uD83D\uDE80"),
    IconOption("palette", "\uD83C\uDFA8"),
    IconOption("code", "\uD83D\uDCBB"),
    IconOption("music", "\uD83C\uDFB5"),
    IconOption("globe", "\uD83C\uDF0D"),
    IconOption("camera", "\uD83D\uDCF7"),
    IconOption("zap", "⚡"),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateContextDialog(
    isCreating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, theme: String, icon: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("MINIMALIST") }
    var selectedIcon by remember { mutableStateOf("briefcase") }

    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text("New Context") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCreating,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    THEMES.forEach { theme ->
                        val isSelected = selectedTheme == theme.key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.color)
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp),
                                    ) else Modifier
                                )
                                .clickable(enabled = !isCreating) { selectedTheme = theme.key }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = theme.label,
                                color = theme.textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ICONS.forEach { icon ->
                        val isSelected = selectedIcon == icon.key
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ) else Modifier
                                )
                                .clickable(enabled = !isCreating) { selectedIcon = icon.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = icon.emoji,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), selectedTheme, selectedIcon) },
                enabled = name.isNotBlank() && !isCreating,
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isCreating,
            ) {
                Text("Cancel")
            }
        },
    )
}
