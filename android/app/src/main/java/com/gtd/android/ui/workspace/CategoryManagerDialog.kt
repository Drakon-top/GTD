package com.gtd.android.ui.workspace

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CATEGORY_COLORS = listOf(
    "#EF4444", "#F97316", "#F59E0B", "#10B981",
    "#06B6D4", "#3B82F6", "#8B5CF6", "#EC4899",
)

private val CATEGORY_ICON_PRESETS = listOf(
    "book-open", "briefcase", "home", "target", "activity", "palette",
    "music", "wallet", "shopping-cart", "plane", "wrench", "graduation-cap",
    "camera", "coffee", "globe", "headphones", "laptop", "map",
    "phone", "scissors", "smile", "sun", "umbrella", "utensils",
    "bike", "car", "film", "gift", "key", "leaf",
    "mountain", "sparkles", "dumbbell", "heart", "star",
)

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryUiItem>,
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?, color: String?) -> Unit,
    onEdit: (id: String, name: String, icon: String?, color: String?) -> Unit,
    onDelete: (id: String) -> Unit,
) {
    var showCreateForm by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryUiItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column {
                if (categories.isEmpty() && !showCreateForm) {
                    Text(
                        "No categories yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height((categories.size.coerceAtMost(5) * 56).dp),
                ) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryListItem(
                            category = cat,
                            onEdit = { editingCategory = cat },
                            onDelete = { onDelete(cat.id) },
                        )
                    }
                }

                if (showCreateForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryForm(
                        initialName = "",
                        initialIcon = null,
                        initialColor = null,
                        onSubmit = { name, icon, color ->
                            onCreate(name, icon, color)
                            showCreateForm = false
                        },
                        onCancel = { showCreateForm = false },
                        submitLabel = "Create",
                    )
                }

                editingCategory?.let { cat ->
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryForm(
                        initialName = cat.name,
                        initialIcon = cat.icon,
                        initialColor = cat.color,
                        onSubmit = { name, icon, color ->
                            onEdit(cat.id, name, icon, color)
                            editingCategory = null
                        },
                        onCancel = { editingCategory = null },
                        submitLabel = "Save",
                    )
                }
            }
        },
        confirmButton = {
            if (!showCreateForm && editingCategory == null) {
                TextButton(onClick = { showCreateForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun CategoryListItem(
    category: CategoryUiItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = resolveCategoryIcon(category.icon),
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (category.color != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(parseColor(category.color)),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${category.taskCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CategoryForm(
    initialName: String,
    initialIcon: String?,
    initialColor: String?,
    onSubmit: (name: String, icon: String?, color: String?) -> Unit,
    onCancel: () -> Unit,
    submitLabel: String,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(normalizeCategoryIconKey(initialIcon)) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    Column {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Icon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(CATEGORY_ICON_PRESETS) { iconKey ->
                val isSelected = iconKey == selectedIcon ||
                    normalizeCategoryIconKey(selectedIcon) == iconKey
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent,
                        )
                        .clickable { selectedIcon = iconKey },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(resolveCategoryIcon(iconKey), fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Color", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(CATEGORY_COLORS) { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parseColor(color))
                        .clickable { selectedColor = color },
                    contentAlignment = Alignment.Center,
                ) {
                    if (color == selectedColor) {
                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = { onSubmit(name.trim(), selectedIcon, selectedColor) },
                enabled = name.isNotBlank(),
            ) { Text(submitLabel) }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color.Gray
    }
}
