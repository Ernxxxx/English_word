package com.example.englishword.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishword.domain.model.Level
import com.example.englishword.domain.model.LevelWithProgress
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.EnglishWordTheme

/**
 * A card component displaying a level with its progress information.
 *
 * @param levelWithProgress The level data with progress information
 * @param onClick Called when the card is clicked (navigate to study)
 * @param onWordListClick Called when "Word List" menu item is clicked
 * @param onDeleteClick Called when "Delete" menu item is clicked
 * @param modifier Modifier for the component
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LevelCard(
    levelWithProgress: LevelWithProgress,
    onClick: () -> Unit,
    onWordListClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    // Animate progress
    val animatedProgress by animateFloatAsState(
        targetValue = levelWithProgress.progressFraction,
        animationSpec = tween(durationMillis = 500),
        label = "progress_animation"
    )

    // Get icon based on level index
    val icon = getLevelIcon(levelWithProgress.level.displayOrder)

    // Determine progress color
    val progressColor by animateColorAsState(
        targetValue = when {
            levelWithProgress.isCompleted -> CorrectGreen
            levelWithProgress.progressFraction > 0.5f -> MaterialTheme.colorScheme.primary
            levelWithProgress.progressFraction > 0.2f -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        },
        label = "progress_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with icon, name, and menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level icon
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = progressColor.copy(alpha = 0.15f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Level name and completion status
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = levelWithProgress.level.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (levelWithProgress.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Completed",
                                tint = CorrectGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Progress text
                    Text(
                        text = if (levelWithProgress.isEmpty) {
                            "No words yet"
                        } else {
                            "${levelWithProgress.masteredCount} / ${levelWithProgress.wordCount} words mastered"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // More menu button
                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Word List") },
                            onClick = {
                                showMenu = false
                                onWordListClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.List,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            if (!levelWithProgress.isEmpty) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Percentage text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = levelWithProgress.percentText,
                        style = MaterialTheme.typography.labelMedium,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )

                    if (levelWithProgress.learningCount > 0) {
                        Text(
                            text = "${levelWithProgress.learningCount} to go",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap to add words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Get an icon for a level based on its index.
 */
private fun getLevelIcon(index: Int): String {
    val icons = listOf(
        "\uD83D\uDCD8", // Green book
        "\uD83D\uDCD7", // Orange book
        "\uD83D\uDCD9", // Blue book
        "\uD83D\uDCDA", // Books
        "\uD83D\uDCD3", // Notebook
        "\uD83D\uDCD5", // Closed book
        "\uD83D\uDCD6", // Open book
        "\uD83D\uDCDD", // Memo
        "\u2728",       // Sparkles
        "\uD83C\uDF1F"  // Star
    )
    return icons[index % icons.size]
}

/**
 * A compact version of the level card for smaller displays.
 */
@Composable
fun LevelCardCompact(
    levelWithProgress: LevelWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getLevelIcon(levelWithProgress.level.displayOrder)

    val progressColor by animateColorAsState(
        targetValue = when {
            levelWithProgress.isCompleted -> CorrectGreen
            else -> MaterialTheme.colorScheme.primary
        },
        label = "progress_color_compact"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = levelWithProgress.level.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = levelWithProgress.progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = levelWithProgress.percentText,
                style = MaterialTheme.typography.labelMedium,
                color = progressColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelCardPreview() {
    EnglishWordTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LevelCard(
                levelWithProgress = LevelWithProgress(
                    level = Level(id = 1, name = "TOEFL Basic", displayOrder = 0),
                    wordCount = 100,
                    masteredCount = 75
                ),
                onClick = {},
                onWordListClick = {},
                onDeleteClick = {}
            )

            LevelCard(
                levelWithProgress = LevelWithProgress(
                    level = Level(id = 2, name = "GRE Advanced", displayOrder = 1),
                    wordCount = 50,
                    masteredCount = 50
                ),
                onClick = {},
                onWordListClick = {},
                onDeleteClick = {}
            )

            LevelCard(
                levelWithProgress = LevelWithProgress(
                    level = Level(id = 3, name = "Empty Level", displayOrder = 2),
                    wordCount = 0,
                    masteredCount = 0
                ),
                onClick = {},
                onWordListClick = {},
                onDeleteClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LevelCardCompactPreview() {
    EnglishWordTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LevelCardCompact(
                levelWithProgress = LevelWithProgress(
                    level = Level(id = 1, name = "TOEFL Basic", displayOrder = 0),
                    wordCount = 100,
                    masteredCount = 75
                ),
                onClick = {}
            )

            LevelCardCompact(
                levelWithProgress = LevelWithProgress(
                    level = Level(id = 2, name = "GRE Advanced", displayOrder = 1),
                    wordCount = 50,
                    masteredCount = 50
                ),
                onClick = {}
            )
        }
    }
}
