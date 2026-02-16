package com.example.englishword.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.englishword.ui.theme.AppDimens
import com.example.englishword.ui.theme.CorrectGreen

private val IncorrectRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitTestScreen(
    levelId: Long,
    onNavigateBack: () -> Unit,
    viewModel: UnitTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(levelId) {
        viewModel.load(levelId)
    }

    // Show count selector as bottom sheet
    if (uiState.isReadyToSelect) {
        ModalBottomSheet(
            onDismissRequest = onNavigateBack,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            CountSelectionSheet(
                availableCount = uiState.availableWordCount,
                onStart = { count -> viewModel.startTest(count) },
                onNavigateBack = onNavigateBack
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!uiState.isLoading && uiState.error == null && !uiState.isCompleted && uiState.words.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "${uiState.progress}/${uiState.totalCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val progress by animateFloatAsState(
                                targetValue = uiState.progress.toFloat() / uiState.totalCount,
                                animationSpec = tween(durationMillis = 300),
                                label = "testProgress"
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
                    } else {
                        Text(
                            text = "単語テスト",
                            style = MaterialTheme.typography.titleLarge
                        )
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
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                ErrorContent(
                    message = uiState.error.orEmpty(),
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.isCompleted -> {
                ResultContent(
                    score = uiState.score,
                    total = uiState.totalCount,
                    onRetry = viewModel::restart,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.isReadyToSelect -> {
                // Empty content behind the bottom sheet
            }

            else -> {
                QuestionContent(
                    uiState = uiState,
                    onSelectAnswer = viewModel::selectAnswer,
                    onNextQuestion = viewModel::nextQuestion,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun CountSelectionSheet(
    availableCount: Int,
    onStart: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    // Build options: 10, 20, 30 (if available), and always "全問"
    val options = buildList {
        if (availableCount > 10) add(10)
        if (availableCount > 20) add(20)
        if (availableCount > 30) add(30)
        add(availableCount)
    }.distinct()

    var selectedCount by remember { mutableIntStateOf(options.first().coerceAtMost(availableCount)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpacingXl, vertical = AppDimens.SpacingSm),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "テストの問題数を選択",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "このユニットには${availableCount}語あります",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        options.forEach { count ->
            val isAll = count == availableCount
            val label = if (isAll && count > 30) "全問 (${count}語)" else "${count}問"
            val isSelected = selectedCount == count

            val borderColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.RadiusLg))
                    .clickable { selectedCount = count },
                shape = RoundedCornerShape(AppDimens.RadiusLg),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor
                ),
                colors = CardDefaults.outlinedCardColors(containerColor = containerColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onStart(selectedCount) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.RadiusLg)
        ) {
            Text(
                text = "${selectedCount}問でテスト開始",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("戻る")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuestionContent(
    uiState: UnitTestUiState,
    onSelectAnswer: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWord = uiState.currentWord ?: return
    val options = uiState.quizOptions ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentWord.english,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (!uiState.isAnswered) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "日本語の意味を選んでください",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Feedback banner (shown after answering)
        if (uiState.isAnswered) {
            FeedbackBanner(
                isCorrect = uiState.isCorrect == true,
                correctAnswer = currentWord.japanese
            )
        }

        // Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.options.forEachIndexed { index, option ->
                OptionButton(
                    text = option,
                    isAnswered = uiState.isAnswered,
                    isCorrectOption = index == options.correctIndex,
                    isSelected = uiState.selectedIndex == index,
                    onClick = { onSelectAnswer(index) }
                )
            }
        }

        // Next button
        if (uiState.isAnswered) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onNextQuestion,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (uiState.progress == uiState.totalCount) "結果を見る" else "次へ",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackBanner(
    isCorrect: Boolean,
    correctAnswer: String
) {
    val backgroundColor = if (isCorrect) {
        CorrectGreen.copy(alpha = 0.12f)
    } else {
        IncorrectRed.copy(alpha = 0.12f)
    }
    val contentColor = if (isCorrect) CorrectGreen else IncorrectRed
    val icon = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel
    val label = if (isCorrect) "正解!" else "不正解"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (!isCorrect) {
                    Text(
                        text = "正解: $correctAnswer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    isAnswered: Boolean,
    isCorrectOption: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val targetContainerColor = when {
        !isAnswered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isCorrectOption -> CorrectGreen.copy(alpha = 0.15f)
        isSelected -> IncorrectRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(300),
        label = "optionColor"
    )

    val borderColor = when {
        !isAnswered -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        isCorrectOption -> CorrectGreen
        isSelected -> IncorrectRed
        else -> Color.Transparent
    }

    val textColor = when {
        !isAnswered -> MaterialTheme.colorScheme.onSurface
        isCorrectOption -> CorrectGreen
        isSelected -> IncorrectRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    OutlinedButton(
        onClick = onClick,
        enabled = !isAnswered,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isAnswered && (isCorrectOption || isSelected)) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (isAnswered && isCorrectOption) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = CorrectGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (isAnswered && isSelected) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = IncorrectRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontWeight = if (isAnswered && (isCorrectOption || isSelected)) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "テストを開始できません",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateBack) {
            Text("戻る")
        }
    }
}

@Composable
private fun ResultContent(
    score: Int,
    total: Int,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) (score * 100) / total else 0
    val resultColor = when {
        percent >= 80 -> CorrectGreen
        percent >= 50 -> Color(0xFFFFA726)
        else -> IncorrectRed
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "テスト完了",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Score circle-like display
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = resultColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$score / $total 正解",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                percent == 100 -> "完璧!"
                percent >= 80 -> "素晴らしい!"
                percent >= 50 -> "もう少し!"
                else -> "復習しましょう"
            },
            style = MaterialTheme.typography.titleSmall,
            color = resultColor,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onNavigateBack) {
                Text("ホームに戻る")
            }
            Button(onClick = onRetry) {
                Text("もう一度テスト")
            }
        }
    }
}
