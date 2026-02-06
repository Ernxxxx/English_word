package com.example.englishword.ui.study

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishword.data.local.entity.Word
import com.example.englishword.ui.components.EvaluationButtons
import com.example.englishword.ui.components.FlashCard
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.MasteryLevel1
import com.example.englishword.ui.theme.MasteryLevel2
import com.example.englishword.ui.theme.MasteryLevel3
import com.example.englishword.ui.theme.MasteryLevel4
import com.example.englishword.ui.theme.MasteryLevel5

/**
 * Main study screen composable.
 * Displays flash cards and handles user evaluation.
 */
@Composable
fun StudyScreen(
    levelId: Long,
    onNavigateToResult: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load words when screen is first displayed
    LaunchedEffect(levelId) {
        viewModel.loadWords(levelId)
    }

    // Navigate to result when completed
    LaunchedEffect(uiState) {
        if (uiState is StudyUiState.Completed) {
            val completedState = uiState as StudyUiState.Completed
            onNavigateToResult(completedState.sessionId)
        }
    }

    StudyScreenContent(
        uiState = uiState,
        onFlipCard = viewModel::flipCard,
        onEvaluate = viewModel::evaluateWord,
        onToggleDirection = viewModel::toggleDirection,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Study screen content - stateless composable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyScreenContent(
    uiState: StudyUiState,
    onFlipCard: () -> Unit,
    onEvaluate: (EvaluationResult) -> Unit,
    onToggleDirection: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            StudyTopAppBar(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onToggleDirection = onToggleDirection
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is StudyUiState.Loading -> {
                    LoadingContent()
                }
                is StudyUiState.Studying -> {
                    StudyingContent(
                        state = uiState,
                        onFlipCard = onFlipCard,
                        onEvaluate = onEvaluate
                    )
                }
                is StudyUiState.Completed -> {
                    // Will navigate away, show loading
                    LoadingContent()
                }
                is StudyUiState.Error -> {
                    ErrorContent(message = uiState.message)
                }
            }
        }
    }
}

/**
 * Top app bar with progress indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudyTopAppBar(
    uiState: StudyUiState,
    onNavigateBack: () -> Unit,
    onToggleDirection: () -> Unit = {}
) {
    TopAppBar(
        title = {
            when (uiState) {
                is StudyUiState.Studying -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${uiState.progress}/${uiState.totalCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Animated progress indicator
                        val progress by animateFloatAsState(
                            targetValue = uiState.progress.toFloat() / uiState.totalCount,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "progressAnimation"
                        )

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
                else -> {
                    Text(
                        text = "学習",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "戻る"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * Loading state content.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading words...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Active studying content with flash card and evaluation buttons.
 */
@Composable
private fun StudyingContent(
    state: StudyUiState.Studying,
    onFlipCard: () -> Unit,
    onEvaluate: (EvaluationResult) -> Unit
) {
    val currentWord = state.currentWord ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mastery info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val masteryLevel = currentWord.masteryLevel
            val masteryColor = getMasteryColor(masteryLevel)
            val masteryLabel = when (masteryLevel) {
                0 -> "新規"
                1 -> "1/5"
                2 -> "2/5"
                3 -> "3/5"
                4 -> "4/5"
                5 -> "完璧"
                else -> "$masteryLevel/5"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "習熟度",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = masteryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = masteryColor
                )
            }
            Text(
                text = "復習 ${currentWord.reviewCount}回",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Flash card - fixed height
        FlashCard(
            word = currentWord,
            isFlipped = state.isFlipped,
            onFlip = onFlipCard,
            isReversed = state.isReversed,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isFlipped) {
            // カードめくり後: 評価ボタン表示
            EvaluationButtons(
                onEvaluate = onEvaluate,
                enabled = true
            )
        } else {
            // カードめくり前: ヒント表示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "カードをタップして答えを確認",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Error state content.
 */
@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyScreenLoadingPreview() {
    EnglishWordTheme {
        StudyScreenContent(
            uiState = StudyUiState.Loading,
            onFlipCard = {},
            onEvaluate = {},
            onToggleDirection = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyScreenStudyingPreview() {
    EnglishWordTheme {
        StudyScreenContent(
            uiState = StudyUiState.Studying(
                words = listOf(
                    Word(
                        id = 1,
                        levelId = 1,
                        english = "Accomplish",
                        japanese = "achieve",
                        exampleEn = "She accomplished her goal.",
                        exampleJa = "She achieved her goal."
                    ),
                    Word(
                        id = 2,
                        levelId = 1,
                        english = "Acquire",
                        japanese = "obtain"
                    )
                ),
                currentIndex = 0,
                isFlipped = false,
                sessionId = 1
            ),
            onFlipCard = {},
            onEvaluate = {},
            onToggleDirection = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyScreenFlippedPreview() {
    EnglishWordTheme {
        StudyScreenContent(
            uiState = StudyUiState.Studying(
                words = listOf(
                    Word(
                        id = 1,
                        levelId = 1,
                        english = "Accomplish",
                        japanese = "achieve",
                        exampleEn = "She accomplished her goal.",
                        exampleJa = "She achieved her goal."
                    )
                ),
                currentIndex = 0,
                isFlipped = true,
                sessionId = 1
            ),
            onFlipCard = {},
            onEvaluate = {},
            onToggleDirection = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyScreenErrorPreview() {
    EnglishWordTheme {
        StudyScreenContent(
            uiState = StudyUiState.Error("Failed to load words"),
            onFlipCard = {},
            onEvaluate = {},
            onToggleDirection = {},
            onNavigateBack = {}
        )
    }
}

private fun getMasteryColor(level: Int): androidx.compose.ui.graphics.Color {
    return when (level) {
        0 -> MasteryLevel1
        1 -> MasteryLevel1
        2 -> MasteryLevel2
        3 -> MasteryLevel3
        4 -> MasteryLevel4
        5 -> MasteryLevel5
        else -> MasteryLevel1
    }
}
