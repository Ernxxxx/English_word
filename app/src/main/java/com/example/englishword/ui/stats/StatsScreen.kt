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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import java.time.LocalDate
import com.example.englishword.ui.theme.AppDimens
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.StreakOrange
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState

/**
 * Tab-friendly version of StatsScreen without Scaffold/TopAppBar.
 * Used by MainShellScreen for the bottom navigation layout.
 */
@Composable
fun StatsTab(
    modifier: Modifier = Modifier,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // SRS Explanation Dialog
    if (uiState.showSrsExplanation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideSrsExplanation() },
            title = { Text("取得単語とは？") },
            text = {
                Text(
                    "取得単語は、ユニット内の「単語テスト」で正解した単語です。\n\n" +
                    "学習画面で単語を見ただけでは取得になりません。\n" +
                    "単語テストで正解すると、その単語が取得済みとして記録されます。"
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
            title = "取得済みの単語",
            words = uiState.masteredWordsList,
            emptyMessage = "まだ取得済みの単語はありません",
            accentColor = CorrectGreen,
            onDismiss = { viewModel.hideMasteredWordsDialog() }
        )
    }

    // Today's Studied Words Detail Dialog
    if (uiState.showTodayWordsDialog) {
        WordListDialog(
            title = "今日学習した単語",
            words = uiState.todayWordsList,
            emptyMessage = "今日はまだ学習していません",
            accentColor = MaterialTheme.colorScheme.primary,
            onDismiss = { viewModel.hideTodayWordsDialog() }
        )
    }

    AnimatedVisibility(
        visible = uiState.isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
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
        if (uiState.error != null) {
            StatsErrorContent(
                error = uiState.error.orEmpty(),
                onRetry = { viewModel.refresh() },
                modifier = modifier
            )
        } else {
            StatsContent(
                uiState = uiState,
                onMasteredCardClick = { viewModel.showMasteredWordsDialog() },
                onTodayCardClick = { viewModel.showTodayWordsDialog() },
                onSrsHelpClick = { viewModel.showSrsExplanation() },
                onShiftWeek = { viewModel.shiftWeek(it) },
                onShiftMonth = { viewModel.shiftMonth(it) },
                modifier = modifier
            )
        }
    }
}

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
            title = { Text("取得単語とは？") },
            text = {
                Text(
                    "取得単語は、ユニット内の「単語テスト」で正解した単語です。\n\n" +
                    "学習画面で単語を見ただけでは取得になりません。\n" +
                    "単語テストで正解すると、その単語が取得済みとして記録されます。"
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
            title = "取得済みの単語",
            words = uiState.masteredWordsList,
            emptyMessage = "まだ取得済みの単語はありません",
            accentColor = CorrectGreen,
            onDismiss = { viewModel.hideMasteredWordsDialog() }
        )
    }

    // Today's Studied Words Detail Dialog
    if (uiState.showTodayWordsDialog) {
        WordListDialog(
            title = "今日学習した単語",
            words = uiState.todayWordsList,
            emptyMessage = "今日はまだ学習していません",
            accentColor = MaterialTheme.colorScheme.primary,
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
            if (uiState.error != null) {
                StatsErrorContent(
                    error = uiState.error.orEmpty(),
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                StatsContent(
                    uiState = uiState,
                    onMasteredCardClick = { viewModel.showMasteredWordsDialog() },
                    onTodayCardClick = { viewModel.showTodayWordsDialog() },
                    onSrsHelpClick = { viewModel.showSrsExplanation() },
                    onShiftWeek = { viewModel.shiftWeek(it) },
                    onShiftMonth = { viewModel.shiftMonth(it) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun StatsErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "統計データの読み込みに失敗しました",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetry) {
                Text("再試行")
            }
        }
    }
}

@Composable
private fun StatsContent(
    uiState: StatsUiState,
    onMasteredCardClick: () -> Unit = {},
    onTodayCardClick: () -> Unit = {},
    onSrsHelpClick: () -> Unit = {},
    onShiftWeek: (Int) -> Unit = {},
    onShiftMonth: (Int) -> Unit = {},
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

        // Section 2: Weekly Study Chart (with week navigation)
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShiftWeek(-1) },
                        enabled = uiState.canGoBackWeek
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "前の週",
                            tint = if (uiState.canGoBackWeek)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.weekOffset == 0) "今週の学習" else "週間学習",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.weekLabel.isNotEmpty()) {
                            Text(
                                text = uiState.weekLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { onShiftWeek(1) },
                        enabled = uiState.canGoForwardWeek
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "次の週",
                            tint = if (uiState.canGoForwardWeek)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.RadiusXl),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppDimens.ElevationMedium),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        WeeklyBarChart(
                            data = uiState.weeklyData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
            }
        }

        // Section 3: Monthly Heatmap (with month navigation)
        item {
            val targetMonth = LocalDate.now().plusMonths(uiState.monthOffset.toLong())
            val monthNames = listOf(
                "1月", "2月", "3月", "4月", "5月", "6月",
                "7月", "8月", "9月", "10月", "11月", "12月"
            )
            val monthLabel = "${targetMonth.year}年 ${monthNames[targetMonth.monthValue - 1]}"

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShiftMonth(-1) },
                        enabled = uiState.canGoBackMonth
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "前の月",
                            tint = if (uiState.canGoBackMonth)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "学習カレンダー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onShiftMonth(1) },
                        enabled = uiState.canGoForwardMonth
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "次の月",
                            tint = if (uiState.canGoForwardMonth)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimens.RadiusXl),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppDimens.ElevationMedium),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        MonthlyHeatmap(
                            data = uiState.monthlyData,
                            modifier = Modifier.fillMaxWidth(),
                            targetDate = targetMonth
                        )
                    }
                }
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
                title = "取得単語",
                value = "${uiState.totalWordsAcquired}",
                subtitle = "/ ${uiState.totalWords}語",
                icon = Icons.Default.CheckCircle,
                iconTint = CorrectGreen,
                progress = if (uiState.totalWords > 0) {
                    uiState.totalWordsAcquired.toFloat() / uiState.totalWords
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
        shape = RoundedCornerShape(AppDimens.RadiusXl),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppDimens.ElevationMedium),
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
// Word List Detail Dialog (Modern BottomSheet)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordListDialog(
    title: String,
    words: List<WordSummary>,
    emptyMessage: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${words.size}語",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (words.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(words) { word ->
                        WordItemCard(word = word)
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WordItemCard(word: WordSummary) {
    val masteryInfo = getMasteryInfo(word.masteryLevel)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mastery color indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(masteryInfo.color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Word text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mastery badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = masteryInfo.color.copy(alpha = 0.12f)
            ) {
                Text(
                    text = masteryInfo.label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = masteryInfo.color
                )
            }
        }
    }
}

private data class MasteryInfo(val label: String, val color: Color)

private fun getMasteryInfo(level: Int): MasteryInfo {
    return when (level) {
        0 -> MasteryInfo("未学習", Color(0xFF9E9E9E))
        in 1..2 -> MasteryInfo("学習中", Color(0xFFFFA726))
        in 3..4 -> MasteryInfo("定着中", Color(0xFF42A5F5))
        else -> MasteryInfo("習得済み", Color(0xFF4CAF50))
    }
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
            shape = RoundedCornerShape(AppDimens.RadiusXl),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = AppDimens.ElevationMedium),
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
