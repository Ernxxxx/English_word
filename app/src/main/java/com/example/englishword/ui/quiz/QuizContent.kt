package com.example.englishword.ui.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishword.data.local.entity.Word
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.CorrectGreenLight
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.IncorrectRed
import com.example.englishword.ui.theme.IncorrectRedLight

/**
 * State of an individual quiz option button.
 */
private enum class QuizOptionState {
    /** Default unselected state */
    DEFAULT,
    /** Selected and correct */
    CORRECT,
    /** Selected and wrong */
    WRONG,
    /** Not selected but revealed as correct (after wrong answer) */
    REVEALED_CORRECT,
    /** Disabled after answer */
    DISABLED
}

/**
 * Main quiz content composable.
 * Displays the English word question and 4-choice answers in a 2x2 grid.
 *
 * @param word The current word being tested
 * @param quizOptions The generated quiz options
 * @param selectedIndex The index of the user's selected answer (null if not yet answered)
 * @param isAnswered Whether the user has already answered
 * @param onSelectAnswer Callback when user selects an answer
 * @param onNext Callback when user taps "Next" to move to next word
 * @param isReversed Whether the quiz is in reverse mode (Japanese question -> English answers)
 */
@Composable
fun QuizContent(
    word: Word,
    quizOptions: QuizOptions,
    selectedIndex: Int?,
    isAnswered: Boolean,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit,
    isReversed: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Question card
        QuizQuestionCard(
            word = word,
            isReversed = isReversed,
            isAnswered = isAnswered,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2x2 answer grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Row 1: Options 0 and 1
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuizOptionButton(
                    text = quizOptions.options[0],
                    state = getOptionState(0, selectedIndex, quizOptions.correctIndex, isAnswered),
                    onClick = { onSelectAnswer(0) },
                    enabled = !isAnswered,
                    modifier = Modifier.weight(1f)
                )
                QuizOptionButton(
                    text = quizOptions.options[1],
                    state = getOptionState(1, selectedIndex, quizOptions.correctIndex, isAnswered),
                    onClick = { onSelectAnswer(1) },
                    enabled = !isAnswered,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Options 2 and 3
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuizOptionButton(
                    text = quizOptions.options[2],
                    state = getOptionState(2, selectedIndex, quizOptions.correctIndex, isAnswered),
                    onClick = { onSelectAnswer(2) },
                    enabled = !isAnswered,
                    modifier = Modifier.weight(1f)
                )
                QuizOptionButton(
                    text = quizOptions.options[3],
                    state = getOptionState(3, selectedIndex, quizOptions.correctIndex, isAnswered),
                    onClick = { onSelectAnswer(3) },
                    enabled = !isAnswered,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "Next" button - shown after answering
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn(animationSpec = tween(300)) +
                    slideInVertically(
                        animationSpec = tween(300),
                        initialOffsetY = { it / 2 }
                    )
        ) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "次へ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Placeholder for button height when not visible
        if (!isAnswered) {
            Spacer(modifier = Modifier.height(56.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Question card showing the English word (or Japanese if reversed).
 */
@Composable
private fun QuizQuestionCard(
    word: Word,
    isReversed: Boolean,
    isAnswered: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main question text
                Text(
                    text = if (isReversed) word.japanese else word.english,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle hint
                Text(
                    text = if (isReversed) "英語の意味を選んでください" else "日本語の意味を選んでください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Show the answer after responding (for learning purposes)
                if (isAnswered) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isReversed) word.english else word.japanese,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = CorrectGreen,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Individual quiz option button with color state animation.
 */
@Composable
private fun QuizOptionButton(
    text: String,
    state: QuizOptionState,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            QuizOptionState.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
            QuizOptionState.CORRECT -> CorrectGreenLight
            QuizOptionState.WRONG -> IncorrectRedLight
            QuizOptionState.REVEALED_CORRECT -> CorrectGreenLight
            QuizOptionState.DISABLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "optionBgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when (state) {
            QuizOptionState.DEFAULT -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            QuizOptionState.CORRECT -> CorrectGreen
            QuizOptionState.WRONG -> IncorrectRed
            QuizOptionState.REVEALED_CORRECT -> CorrectGreen
            QuizOptionState.DISABLED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "optionBorderColor"
    )

    val textColor by animateColorAsState(
        targetValue = when (state) {
            QuizOptionState.DEFAULT -> MaterialTheme.colorScheme.onSurface
            QuizOptionState.CORRECT -> CorrectGreen
            QuizOptionState.WRONG -> IncorrectRed
            QuizOptionState.REVEALED_CORRECT -> CorrectGreen
            QuizOptionState.DISABLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "optionTextColor"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = textColor,
            disabledContainerColor = backgroundColor,
            disabledContentColor = textColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Determines the visual state of a quiz option button.
 */
private fun getOptionState(
    optionIndex: Int,
    selectedIndex: Int?,
    correctIndex: Int,
    isAnswered: Boolean
): QuizOptionState {
    if (!isAnswered) return QuizOptionState.DEFAULT

    return when {
        // This option was selected and is correct
        optionIndex == selectedIndex && optionIndex == correctIndex -> QuizOptionState.CORRECT
        // This option was selected but is wrong
        optionIndex == selectedIndex && optionIndex != correctIndex -> QuizOptionState.WRONG
        // This option was not selected but is the correct answer (reveal it)
        optionIndex != selectedIndex && optionIndex == correctIndex -> QuizOptionState.REVEALED_CORRECT
        // All other options
        else -> QuizOptionState.DISABLED
    }
}

// ==================== Previews ====================

@Preview(showBackground = true)
@Composable
private fun QuizContentDefaultPreview() {
    EnglishWordTheme {
        QuizContent(
            word = Word(
                id = 1,
                levelId = 1,
                english = "Accomplish",
                japanese = "達成する"
            ),
            quizOptions = QuizOptions(
                options = listOf("達成する", "獲得する", "蓄積する", "適応する"),
                correctIndex = 0
            ),
            selectedIndex = null,
            isAnswered = false,
            onSelectAnswer = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizContentCorrectPreview() {
    EnglishWordTheme {
        QuizContent(
            word = Word(
                id = 1,
                levelId = 1,
                english = "Accomplish",
                japanese = "達成する"
            ),
            quizOptions = QuizOptions(
                options = listOf("達成する", "獲得する", "蓄積する", "適応する"),
                correctIndex = 0
            ),
            selectedIndex = 0,
            isAnswered = true,
            onSelectAnswer = {},
            onNext = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuizContentWrongPreview() {
    EnglishWordTheme {
        QuizContent(
            word = Word(
                id = 1,
                levelId = 1,
                english = "Accomplish",
                japanese = "達成する"
            ),
            quizOptions = QuizOptions(
                options = listOf("達成する", "獲得する", "蓄積する", "適応する"),
                correctIndex = 0
            ),
            selectedIndex = 2,
            isAnswered = true,
            onSelectAnswer = {},
            onNext = {}
        )
    }
}
