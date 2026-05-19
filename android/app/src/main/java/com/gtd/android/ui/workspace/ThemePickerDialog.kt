package com.gtd.android.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class ThemeOption(
    val key: String,
    val label: String,
    val primaryColor: Color,
    val bgColor: Color,
)

private val THEMES = listOf(
    ThemeOption("MINIMALIST", "Minimalist", Color(0xFF6B7280), Color(0xFFF5F5F5)),
    ThemeOption("DESIGN", "Design", Color(0xFF7C3AED), Color(0xFFF5F3FF)),
    ThemeOption("FORMAL", "Formal", Color(0xFF374151), Color(0xFFF9FAFB)),
    ThemeOption("NATURE", "Nature", Color(0xFF059669), Color(0xFFF0FDF4)),
    ThemeOption("DARK", "Dark", Color(0xFFA78BFA), Color(0xFF18181B)),
    ThemeOption("DRAGONS", "Dragons", Color(0xFFF59E0B), Color(0xFF1C1917)),
    ThemeOption("ICE_DRAGONS", "Ice Dragons", Color(0xFF38BDF8), Color(0xFF0C4A6E)),
)

@Composable
fun ThemePickerDialog(
    currentTheme: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                THEMES.forEach { theme ->
                    val isSelected = theme.key == currentTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.bgColor)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.dp,
                                    theme.primaryColor,
                                    RoundedCornerShape(12.dp),
                                ) else Modifier
                            )
                            .clickable { onSelect(theme.key) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(theme.primaryColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = theme.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (theme.key in listOf("DARK", "DRAGONS", "ICE_DRAGONS"))
                                Color.White else Color(0xFF1F2937),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
