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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.englishword.ui.quiz.QuizContent
import com.example.englishword.ui.quiz.QuizOptions
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.MasteryLevel1
import com.example.englishword.ui.theme.MasteryLevel2
import com.example.englishword.ui.theme.MasteryLevel3
import com.example.englishword.ui.theme.MasteryLevel4
import com.example.englishword.ui.theme.MasteryLevel5
import com.example.englishword.util.SrsCalculator

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isTtsReady by viewModel.ttsManager.isReady.collectAsStateWithLifecycle()
    val isTtsSpeaking by viewModel.ttsManager.isSpeaking.collectAsStateWithLifecycle()
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    // Back confirmation dialog during study
    BackHandler(enabled = uiState is StudyUiState.Studying) {
        showBackConfirmDialog = true
    }

    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            title = { Text("学習を中断しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    showBackConfirmDialog = false
                    onNavigateBack()
                }) {
                    Text("中断する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text("続ける")
                }
            }
        )
    }

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
        onSelectQuizAnswer = viewModel::selectQuizAnswer,
        onNextQuizWord = viewModel::nextQuizWord,
        onQuizSkip = viewModel::quizSkipWord,
        onSpeakWord = viewModel::speakWord,
        isTtsReady = isTtsReady,
        isTtsSpeaking = isTtsSpeaking,
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
    onSelectQuizAnswer: (Int) -> Unit = {},
    onNextQuizWord: () -> Unit = {},
    onQuizSkip: () -> Unit = {},
    onSpeakWord: () -> Unit = {},
    isTtsReady: Boolean = false,
    isTtsSpeaking: Boolean = false,
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
                        onEvaluate = onEvaluate,
                        onSelectQuizAnswer = onSelectQuizAnswer,
                        onNextQuizWord = onNextQuizWord,
                        onQuizSkip = onQuizSkip,
                        onSpeakWord = onSpeakWord,
                        isTtsReady = isTtsReady,
                        isTtsSpeaking = isTtsSpeaking
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
                    contentDescription = "学習画面を閉じる"
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
                text = "単語を読み込み中...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Active studying content with flash card and evaluation buttons,
 * or quiz mode content.
 */
@Composable
private fun StudyingContent(
    state: StudyUiState.Studying,
    onFlipCard: () -> Unit,
    onEvaluate: (EvaluationResult) -> Unit,
    onSelectQuizAnswer: (Int) -> Unit = {},
    onNextQuizWord: () -> Unit = {},
    onQuizSkip: () -> Unit = {},
    onSpeakWord: () -> Unit = {},
    isTtsReady: Boolean = false,
    isTtsSpeaking: Boolean = false
) {
    val currentWord = state.currentWord ?: return

    // Quiz mode rendering
    if (state.isQuizMode && state.quizOptions != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Mastery info row (same as flashcard mode)
            MasteryInfoRow(currentWord = currentWord)

            // Quiz content
            QuizContent(
                word = currentWord,
                quizOptions = state.quizOptions,
                selectedIndex = state.selectedAnswerIndex,
                isAnswered = state.isQuizAnswered,
                onSelectAnswer = onSelectQuizAnswer,
                onNext = onNextQuizWord,
                onSkip = onQuizSkip,
                isReversed = state.isReversed,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    // Flashcard mode rendering (existing behavior)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Mastery info row
        MasteryInfoRow(currentWord = currentWord)

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

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isFlipped) {
            // カードめくり後: 発音ボタン＋評価ボタン
            SpeakButton(
                onSpeakWord = onSpeakWord,
                isTtsReady = isTtsReady,
                isTtsSpeaking = isTtsSpeaking
            )
            Spacer(modifier = Modifier.height(12.dp))
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
 * Mastery info row showing mastery level and review count.
 * Shared between flashcard and quiz modes.
 */
@Composable
private fun MasteryInfoRow(currentWord: Word) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val masteryLevel = currentWord.masteryLevel
        val masteryColor = getMasteryColor(masteryLevel)
        val maxLevel = SrsCalculator.MAX_LEVEL
        val masteryLabel = when {
            masteryLevel <= 0 -> "新規"
            masteryLevel >= maxLevel -> "完璧"
            else -> "$masteryLevel/$maxLevel"
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
}

/**
 * Prominent speak button displayed outside the flash card.
 */
@Composable
private fun SpeakButton(
    onSpeakWord: () -> Unit,
    isTtsReady: Boolean,
    isTtsSpeaking: Boolean
) {
    Button(
        onClick = onSpeakWord,
        enabled = isTtsReady,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isTtsSpeaking) "再生中..." else "発音を聞く",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
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
                text = "エラー",
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
private fun StudyScreenQuizPreview() {
    EnglishWordTheme {
        StudyScreenContent(
            uiState = StudyUiState.Studying(
                words = listOf(
                    Word(
                        id = 1,
                        levelId = 1,
                        english = "Accomplish",
                        japanese = "達成する"
                    )
                ),
                currentIndex = 0,
                isFlipped = false,
                sessionId = 1,
                isQuizMode = true,
                quizOptions = QuizOptions(
                    options = listOf("達成する", "獲得する", "蓄積する", "適応する"),
                    correctIndex = 0
                )
            ),
            onFlipCard = {},
            onEvaluate = {},
            onToggleDirection = {},
            onNavigateBack = {},
            onSelectQuizAnswer = {},
            onNextQuizWord = {}
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
        in SrsCalculator.MAX_LEVEL..Int.MAX_VALUE -> MasteryLevel5
        else -> MasteryLevel1
    }
}
