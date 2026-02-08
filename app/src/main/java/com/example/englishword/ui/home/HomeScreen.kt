@file:OptIn(ExperimentalFoundationApi::class)

package com.example.englishword.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishword.domain.model.Level
import com.example.englishword.domain.model.LevelWithProgress
import com.example.englishword.domain.model.ParentLevelWithChildren
import com.example.englishword.ads.AdManager
import com.example.englishword.ui.components.BannerAdView
import com.example.englishword.ui.components.LevelCard
import com.example.englishword.ui.components.StreakBadgeJapanese
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.PremiumGold
import kotlinx.coroutines.flow.collectLatest

/**
 * Home screen displaying the list of levels and study statistics.
 *
 * @param onNavigateToStudy Called when navigating to study a level
 * @param onNavigateToWordList Called when navigating to view word list
 * @param onNavigateToSettings Called when navigating to settings
 * @param onNavigateToPremium Called when navigating to premium screen
 * @param onNavigateToStats Called when navigating to stats screen
 * @param viewModel The HomeViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudy: (Long) -> Unit,
    onNavigateToWordList: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is HomeEvent.NavigateToStudy -> onNavigateToStudy(event.levelId)
                is HomeEvent.NavigateToWordList -> onNavigateToWordList(event.levelId)
                is HomeEvent.NavigateToSettings -> onNavigateToSettings()
                is HomeEvent.NavigateToPremium -> onNavigateToPremium()
                is HomeEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                is HomeEvent.ShowRewardedAd -> {
                    val activity = context as? Activity
                    if (activity != null) {
                        viewModel.adManager.showRewardedAd(
                            activity = activity,
                            onRewarded = { viewModel.onAdWatchedForUnlock(event.levelId) },
                            onAdDismissed = { /* User cancelled or ad failed */ }
                        )
                    }
                }
                is HomeEvent.UnitUnlocked -> {
                    snackbarHostState.showSnackbar("ユニットが3時間アンロックされました")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "英単語帳",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "統計"
                        )
                    }
                    if (!uiState.isPremium) {
                        IconButton(onClick = onNavigateToPremium) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "プレミアム",
                                tint = PremiumGold
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                else -> {
                    HomeContent(
                        uiState = uiState,
                        listState = listState,
                        onLevelClick = { viewModel.navigateToStudy(it.level.id) },
                        onWordListClick = { viewModel.navigateToWordList(it.level.id) },
                        onDeleteClick = { viewModel.showDeleteDialog(it) },
                        onPremiumClick = onNavigateToPremium,
                        onParentClick = { viewModel.toggleParentExpansion(it) },
                        onUnlockClick = { viewModel.showUnlockDialog(it) }
                    )
                }
            }

            // Added: Banner ad at bottom (above FAB) - hidden for premium users
            if (!uiState.isPremium) {
                BannerAdView(
                    adUnitId = AdManager.BANNER_AD_UNIT_ID,
                    isPremium = uiState.isPremium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }


    // Delete confirmation dialog
    if (uiState.showDeleteDialog && uiState.levelToDelete != null) {
        DeleteLevelDialog(
            levelName = uiState.levelToDelete!!.level.name,
            wordCount = uiState.levelToDelete!!.wordCount,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = { viewModel.deleteLevel() }
        )
    }

    // Unit unlock dialog
    if (uiState.showUnlockDialog && uiState.levelToUnlock != null) {
        UnlockUnitDialog(
            onDismiss = { viewModel.hideUnlockDialog() },
            onWatchAd = { viewModel.requestWatchAdForUnlock() },
            onPremiumClick = onNavigateToPremium
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLevelClick: (LevelWithProgress) -> Unit,
    onWordListClick: (LevelWithProgress) -> Unit,
    onDeleteClick: (LevelWithProgress) -> Unit,
    onPremiumClick: () -> Unit,
    onParentClick: (Long) -> Unit = {},
    onUnlockClick: (Long) -> Unit = {}
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats header
        item {
            StatsHeader(
                todayStudiedCount = uiState.todayStudiedCount,
                dailyGoal = uiState.dailyGoal,
                streak = uiState.streak,
                isPremium = uiState.isPremium,
                remainingReviews = uiState.remainingReviews
            )
        }

        // Hierarchical level cards
        if (uiState.parentLevels.isEmpty()) {
            item {
                EmptyLevelsContent()
            }
        } else {
            uiState.parentLevels.forEach { parentWithChildren ->
                // Parent level header
                item(key = "parent_${parentWithChildren.parentLevel.level.id}") {
                    ParentLevelCard(
                        parentWithChildren = parentWithChildren,
                        onClick = { onParentClick(parentWithChildren.parentLevel.level.id) }
                    )
                }

                // Child levels (animated visibility)
                items(
                    items = if (parentWithChildren.isExpanded) parentWithChildren.children else emptyList(),
                    key = { "child_${it.level.id}" }
                ) { childLevel ->
                    ChildLevelCard(
                        levelWithProgress = childLevel,
                        onClick = { onLevelClick(childLevel) },
                        onWordListClick = { onWordListClick(childLevel) },
                        onUnlockClick = { onUnlockClick(childLevel.level.id) },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(300)
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatsHeader(
    todayStudiedCount: Int,
    dailyGoal: Int,
    streak: Int,
    isPremium: Boolean,
    remainingReviews: Int = Int.MAX_VALUE
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Study",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = todayStudiedCount.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (todayStudiedCount >= dailyGoal) {
                                CorrectGreen
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = " / $dailyGoal words",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                StreakBadgeJapanese(streak = streak)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily progress bar
            val progress = if (dailyGoal > 0) {
                (todayStudiedCount.toFloat() / dailyGoal).coerceAtMost(1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progress >= 1f) CorrectGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (todayStudiedCount >= dailyGoal) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily goal achieved! Great job!",
                    style = MaterialTheme.typography.labelMedium,
                    color = CorrectGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            // Show remaining reviews for free users
            if (!isPremium && remainingReviews < Int.MAX_VALUE) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (remainingReviews > 0) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (remainingReviews > 0) {
                                "本日の残り復習: $remainingReviews/10語"
                            } else {
                                "本日の無料復習上限に達しました"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingReviews > 0) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentLevelCard(
    parentWithChildren: ParentLevelWithChildren,
    onClick: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (parentWithChildren.isExpanded) 90f else 0f,
        animationSpec = tween(300),
        label = "expand_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/Collapse indicator with rotation animation
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = if (parentWithChildren.isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(rotationAngle)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parentWithChildren.parentLevel.level.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${parentWithChildren.children.size}ユニット / ${parentWithChildren.totalWordCount}語",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${parentWithChildren.progressPercent}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (parentWithChildren.progressPercent >= 100) {
                        CorrectGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { parentWithChildren.progressFraction },
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (parentWithChildren.progressFraction >= 1f) {
                        CorrectGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChildLevelCard(
    levelWithProgress: LevelWithProgress,
    onClick: () -> Unit,
    onWordListClick: () -> Unit,
    onUnlockClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLocked = levelWithProgress.isLocked

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp)
            .clickable {
                if (isLocked) {
                    onUnlockClick()
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLocked) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock icon for locked units
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = levelWithProgress.level.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isLocked) {
                        "${levelWithProgress.wordCount}語 - 広告視聴でアンロック"
                    } else if (levelWithProgress.remainingUnlockTimeMs > 0) {
                        val minutes = (levelWithProgress.remainingUnlockTimeMs / 60000).toInt()
                        val hours = minutes / 60
                        val mins = minutes % 60
                        "${levelWithProgress.wordCount}語 - 残り${hours}時間${mins}分"
                    } else {
                        "${levelWithProgress.wordCount}語"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress
            Text(
                text = "${levelWithProgress.progressPercent}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isLocked) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else if (levelWithProgress.isCompleted) {
                    CorrectGreen
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Word list button or unlock button
            if (isLocked) {
                TextButton(onClick = onUnlockClick) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "広告で解除",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "広告で解除",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                TextButton(onClick = onWordListClick) {
                    Text(
                        text = "単語一覧",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLevelsContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Levels Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the + button to create your first vocabulary level",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun DeleteLevelDialog(
    levelName: String,
    wordCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Level",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete \"$levelName\"?"
                )
                if (wordCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will also delete $wordCount word${if (wordCount > 1) "s" else ""}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun UnlockUnitDialog(
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
    onPremiumClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ユニットをアンロック",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "このユニットは現在ロックされています。"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "広告を視聴すると3時間アンロックできます。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = PremiumGold.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "プレミアムで全ユニット永久アンロック",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onWatchAd) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "広告を見る",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("広告を見る")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onPremiumClick) {
                    Text(
                        text = "プレミアム",
                        color = PremiumGold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val grade1Units = listOf(
        LevelWithProgress(
            level = Level(id = 11, name = "Unit 1 - 基本動詞", displayOrder = 0, parentId = 1),
            wordCount = 10,
            masteredCount = 8
        ),
        LevelWithProgress(
            level = Level(id = 12, name = "Unit 2 - 家族・学校", displayOrder = 1, parentId = 1),
            wordCount = 10,
            masteredCount = 5
        )
    )
    val grade1Parent = LevelWithProgress(
        level = Level(id = 1, name = "中学1年", displayOrder = 0),
        wordCount = 50,
        masteredCount = 35
    )

    EnglishWordTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                parentLevels = listOf(
                    ParentLevelWithChildren(
                        parentLevel = grade1Parent,
                        children = grade1Units,
                        isExpanded = true
                    )
                ),
                todayStudiedCount = 15,
                streak = 7,
                isPremium = false,
                dailyGoal = 20
            ),
            listState = rememberLazyListState(),
            onLevelClick = {},
            onWordListClick = {},
            onDeleteClick = {},
            onPremiumClick = {},
            onParentClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    EnglishWordTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                parentLevels = emptyList(),
                todayStudiedCount = 0,
                streak = 0,
                isPremium = false,
                dailyGoal = 20
            ),
            listState = rememberLazyListState(),
            onLevelClick = {},
            onWordListClick = {},
            onDeleteClick = {},
            onPremiumClick = {},
            onParentClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsHeaderPreview() {
    EnglishWordTheme {
        StatsHeader(
            todayStudiedCount = 25,
            dailyGoal = 20,
            streak = 14,
            isPremium = true
        )
    }
}
