package com.gtd.android.ui.context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gtd.android.ui.auth.AuthViewModel
import kotlinx.coroutines.launch

private data class ThemeStyle(
    val cardBg: Color,
    val textColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val accentColor: Color,
)

private val THEME_STYLES = mapOf(
    "MINIMALIST" to ThemeStyle(Color(0xFFF5F5F5), Color(0xFF1F2937), Color(0xFFE5E7EB), Color(0xFF374151), Color(0xFF6B7280)),
    "DESIGN" to ThemeStyle(Color(0xFFF5F3FF), Color(0xFF5B21B6), Color(0xFFEDE9FE), Color(0xFF7C3AED), Color(0xFF7C3AED)),
    "FORMAL" to ThemeStyle(Color(0xFFF9FAFB), Color(0xFF111827), Color(0xFFF3F4F6), Color(0xFF374151), Color(0xFF4B5563)),
    "NATURE" to ThemeStyle(Color(0xFFF0FDF4), Color(0xFF065F46), Color(0xFFD1FAE5), Color(0xFF059669), Color(0xFF059669)),
    "DARK" to ThemeStyle(Color(0xFF18181B), Color(0xFFF4F4F5), Color(0xFF27272A), Color(0xFFA78BFA), Color(0xFFA78BFA)),
    "DRAGONS" to ThemeStyle(Color(0xFF1C1917), Color(0xFFFBBF24), Color(0xFF292524), Color(0xFFF59E0B), Color(0xFFF59E0B)),
    "ICE_DRAGONS" to ThemeStyle(Color(0xFF0C4A6E), Color(0xFFE0F2FE), Color(0xFF075985), Color(0xFF7DD3FC), Color(0xFF38BDF8)),
)

private val ICON_MAP = mapOf(
    "briefcase" to "\uD83D\uDCBC", "home" to "\uD83C\uDFE0", "book" to "\uD83D\uDCDA",
    "heart" to "❤\uFE0F", "star" to "⭐", "rocket" to "\uD83D\uDE80",
    "palette" to "\uD83C\uDFA8", "code" to "\uD83D\uDCBB", "music" to "\uD83C\uDFB5",
    "globe" to "\uD83C\uDF0D", "camera" to "\uD83D\uDCF7", "zap" to "⚡",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextsScreen(
    onContextClick: (String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    contextsViewModel: ContextsViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val state by contextsViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contexts") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            authViewModel.logout()
                            onLogout()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.contexts.size < ContextsViewModel.MAX_CONTEXTS && !state.isLoading) {
                FloatingActionButton(
                    onClick = { contextsViewModel.showCreateDialog() },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create context")
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Failed to load contexts",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap to retry",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { contextsViewModel.loadContexts() },
                        )
                    }
                }
                state.contexts.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No contexts yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create your first context to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.contexts, key = { it.id }) { ctx ->
                            ContextCard(
                                context = ctx,
                                onClick = { onContextClick(ctx.id) },
                            )
                        }
                    }
                }
            }
        }

        if (state.showCreateDialog) {
            CreateContextDialog(
                isCreating = state.isCreating,
                error = state.createError,
                onDismiss = { contextsViewModel.dismissCreateDialog() },
                onCreate = { name, theme, icon ->
                    contextsViewModel.createContext(name, theme, icon)
                },
            )
        }
    }
}

@Composable
private fun ContextCard(
    context: ContextUiItem,
    onClick: () -> Unit,
) {
    val style = THEME_STYLES[context.theme] ?: THEME_STYLES["MINIMALIST"]!!

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = style.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(style.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ICON_MAP[context.icon] ?: "\uD83D\uDCBC",
                        fontSize = 20.sp,
                    )
                }

                if (context.inboxCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(style.badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${context.inboxCount}",
                            color = style.badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Column {
                Text(
                    text = context.name,
                    color = style.textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (context.inboxCount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${context.inboxCount} in Inbox",
                        color = style.textColor.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
