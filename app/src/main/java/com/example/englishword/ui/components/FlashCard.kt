package com.example.englishword.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.englishword.data.local.entity.Word
import com.example.englishword.ui.theme.EnglishWordTheme

/**
 * FlashCard component with flip animation.
 * Shows English word on front, Japanese translation and examples on back.
 * isReversed: true = Japanese→English mode
 */
@Composable
fun FlashCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
    onSpeakClick: (() -> Unit)? = null,
    isTtsReady: Boolean = false,
    isSpeaking: Boolean = false
) {
    // Animation for the flip effect
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "cardFlip"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onFlip)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp,
            pressedElevation = 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front side
                if (isReversed) {
                    FlashCardJapaneseFront(word = word, modifier = Modifier.fillMaxSize())
                } else {
                    FlashCardFront(
                        word = word,
                        modifier = Modifier.fillMaxSize(),
                        onSpeakClick = onSpeakClick,
                        isTtsReady = isTtsReady,
                        isSpeaking = isSpeaking
                    )
                }
            } else {
                // Back side
                if (isReversed) {
                    FlashCardEnglishBack(
                        word = word,
                        modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                        onSpeakClick = onSpeakClick,
                        isTtsReady = isTtsReady,
                        isSpeaking = isSpeaking
                    )
                } else {
                    FlashCardBack(
                        word = word,
                        modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f },
                        onSpeakClick = onSpeakClick,
                        isTtsReady = isTtsReady,
                        isSpeaking = isSpeaking
                    )
                }
            }
        }
    }
}

/**
 * Front side of the flash card showing English word.
 */
@Composable
private fun FlashCardFront(
    word: Word,
    modifier: Modifier = Modifier,
    onSpeakClick: (() -> Unit)? = null,
    isTtsReady: Boolean = false,
    isSpeaking: Boolean = false
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (onSpeakClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onSpeakClick,
                        enabled = isTtsReady,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "発音",
                            tint = if (isTtsReady)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "タップして答えを確認",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Front side for reversed mode (Japanese→English): shows Japanese
 */
@Composable
private fun FlashCardJapaneseFront(
    word: Word,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = word.japanese,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "タップして答えを確認",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Back side for reversed mode (Japanese→English): shows English
 */
@Composable
private fun FlashCardEnglishBack(
    word: Word,
    modifier: Modifier = Modifier,
    onSpeakClick: (() -> Unit)? = null,
    isTtsReady: Boolean = false,
    isSpeaking: Boolean = false
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = word.japanese,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (onSpeakClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSpeakClick,
                        enabled = isTtsReady,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "発音",
                            tint = if (isTtsReady)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (!word.exampleEn.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = word.exampleEn,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Back side of the flash card showing Japanese translation and examples.
 */
@Composable
private fun FlashCardBack(
    word: Word,
    modifier: Modifier = Modifier,
    onSpeakClick: (() -> Unit)? = null,
    isTtsReady: Boolean = false,
    isSpeaking: Boolean = false
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // English word with speaker button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.english,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (onSpeakClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSpeakClick,
                        enabled = isTtsReady,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "発音",
                            tint = if (isTtsReady)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Japanese translation
            Text(
                text = word.japanese,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Example sentences (if available)
            if (!word.exampleEn.isNullOrBlank() || !word.exampleJa.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // English example
                if (!word.exampleEn.isNullOrBlank()) {
                    Text(
                        text = word.exampleEn,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Japanese example
                if (!word.exampleJa.isNullOrBlank()) {
                    Text(
                        text = word.exampleJa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashCardFrontPreview() {
    EnglishWordTheme {
        FlashCard(
            word = Word(
                id = 1,
                levelId = 1,
                english = "Accomplish",
                japanese = "達成する",
                exampleEn = "She accomplished her goal.",
                exampleJa = "彼女は目標を達成した。"
            ),
            isFlipped = false,
            onFlip = {},
            modifier = Modifier.height(400.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashCardBackPreview() {
    EnglishWordTheme {
        FlashCard(
            word = Word(
                id = 1,
                levelId = 1,
                english = "Accomplish",
                japanese = "達成する",
                exampleEn = "She accomplished her goal.",
                exampleJa = "彼女は目標を達成した。"
            ),
            isFlipped = true,
            onFlip = {},
            modifier = Modifier.height(400.dp)
        )
    }
}
