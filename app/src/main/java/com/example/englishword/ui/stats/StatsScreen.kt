package com.example.englishword.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.StreakOrange

/**
 * Statistics screen displaying learning progress, charts, and streak information.
 *
 * @param onNavigateBack Called when the user presses the back button
 * @param viewModel The StatsViewModel providing UI state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // SRS Explanation Dialog
    if (uiState.showSrsExplanation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSrsExplanation() },
            title = { Text("習得単語とは？") },
            text = {
                Text(
                    "単語の習熟度は5段階で上がります。\n\n" +
                    "1回目「覚えた」→ 1時間後に復習\n" +
                    "2回目「覚えた」→ 8時間後に復習\n" +
                    "3回目「覚えた」→ 1日後に復習\n" +
                    "4回目「覚えた」→ 3日後に復習\n" +
                    "5回目「覚えた」→ 習得完了\n\n" +
                    "同じ単語を間隔を空けて5回正解すると「習得済み」になります。" +
                    "これは科学的に効果が証明されている間隔反復学習（SRS）です。"
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideSrsExplanation() }) {
                    Text("閉じる")
                }
            }
        )
    }

    // Mastered Words Detail Dialog
    if (uiState.showMasteredWordsDialog) {
        WordListDialog(
            title = "習得済みの単語",
            words = uiState.masteredWordsList,
            emptyMessage = "まだ習得済みの単語はありません",
            onDismiss = { viewModel.hideMasteredWordsDialog() }
        )
    }

    // Today's Studied Words Detail Dialog
    if (uiState.showTodayWordsDialog) {
        WordListDialog(
            title = "今日学習した単語",
            words = uiState.todayWordsList,
            emptyMessage = "今日はまだ学習していません",
            onDismiss = { viewModel.hideTodayWordsDialog() }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "\u5b66\u7fd2\u7d71\u8a08",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u623b\u308b"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            StatsContent(
                uiState = uiState,
                onMasteredCardClick = { viewModel.showMasteredWordsDialog() },
                onTodayCardClick = { viewModel.showTodayWordsDialog() },
                onSrsHelpClick = { viewModel.showSrsExplanation() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun StatsContent(
    uiState: StatsUiState,
    onMasteredCardClick: () -> Unit = {},
    onTodayCardClick: () -> Unit = {},
    onSrsHelpClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Overview Cards (2x2 grid)
        item {
            OverviewCardsSection(
                uiState = uiState,
                onMasteredCardClick = onMasteredCardClick,
                onTodayCardClick = onTodayCardClick,
                onSrsHelpClick = onSrsHelpClick
            )
        }

        // Section 2: Weekly Study Chart
        item {
            StatsSection(title = "\u4eca\u9031\u306e\u5b66\u7fd2") {
                WeeklyBarChart(
                    data = uiState.weeklyData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Section 3: Monthly Heatmap
        item {
            StatsSection(title = "\u6708\u9593\u5b66\u7fd2\u30ab\u30ec\u30f3\u30c0\u30fc") {
                MonthlyHeatmap(
                    data = uiState.monthlyData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section 4: Mastery Distribution
        item {
            StatsSection(title = "\u7fd2\u719f\u5ea6\u5206\u5e03") {
                MasteryDonutChart(
                    distribution = uiState.masteryDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                )
            }
        }

        // Section 5: Streak Information
        item {
            StreakCard(
                currentStreak = uiState.currentStreak,
                maxStreak = uiState.maxStreak
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Section 1: Overview Cards
// ---------------------------------------------------------------------------

@Composable
private fun OverviewCardsSection(
    uiState: StatsUiState,
    onMasteredCardClick: () -> Unit = {},
    onTodayCardClick: () -> Unit = {},
    onSrsHelpClick: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "習得単語",
                value = "${uiState.totalWordsMastered}",
                subtitle = "/ ${uiState.totalWords}語",
                icon = Icons.Default.CheckCircle,
                iconTint = CorrectGreen,
                progress = if (uiState.totalWords > 0) {
                    uiState.totalWordsMastered.toFloat() / uiState.totalWords
                } else 0f,
                onClick = onMasteredCardClick,
                helpIcon = true,
                onHelpClick = onSrsHelpClick,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "連続記録",
                value = "${uiState.currentStreak}",
                subtitle = "日",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = StreakOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "今日の学習",
                value = "${uiState.totalWordsStudied}",
                subtitle = "語",
                icon = Icons.Default.School,
                iconTint = MaterialTheme.colorScheme.primary,
                onClick = onTodayCardClick,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "日平均",
                value = String.format("%.1f", uiState.averageDaily),
                subtitle = "語/日",
                icon = Icons.Default.Speed,
                iconTint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
    helpIcon: Boolean = false,
    onHelpClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Icon row with optional help icon
            if (icon != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (helpIcon && onHelpClick != null) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "説明を見る",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onHelpClick)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Value + subtitle inline
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card title with tap hint
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "詳細 >",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            // Optional progress indicator
            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (progress >= 1f) CorrectGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Word List Detail Dialog
// ---------------------------------------------------------------------------

@Composable
private fun WordListDialog(
    title: String,
    words: List<WordSummary>,
    emptyMessage: String,
    onDismiss: () -> Unit
) {
    val masteryLabels = listOf("未学習", "学習開始", "学習中", "定着中", "ほぼ習得", "習得済み")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title)
                Text(
                    text = "${words.size}語",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (words.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(words) { word ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = word.english,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = word.japanese,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = masteryLabels.getOrElse(word.masteryLevel) { "Lv${word.masteryLevel}" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Reusable Section Wrapper
// ---------------------------------------------------------------------------

@Composable
private fun StatsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}
