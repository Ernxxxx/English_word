package com.example.englishword.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.englishword.ui.theme.EvaluationAgain
import com.example.englishword.ui.theme.EvaluationKnown
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

        // Buttons - always at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // まだ - 覚えていない、もう一度
            Button(
                onClick = { onEvaluate(EvaluationResult.AGAIN) },
                enabled = state.isFlipped,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvaluationAgain,
                    disabledContainerColor = EvaluationAgain.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "まだ",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "もう一度",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            // 覚えた - 次の単語へ
            Button(
                onClick = { onEvaluate(EvaluationResult.KNOWN) },
                enabled = state.isFlipped,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvaluationKnown,
                    disabledContainerColor = EvaluationKnown.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "覚えた",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "次へ",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
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
