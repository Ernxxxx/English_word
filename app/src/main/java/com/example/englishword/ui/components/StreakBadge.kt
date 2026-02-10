package com.example.englishword.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.StreakOrange

/**
 * A badge component displaying the current study streak.
 * Shows a fire emoji and the number of consecutive days.
 *
 * @param streak The current streak in days
 * @param modifier Modifier for the component
 * @param showLabel Whether to show the label text (e.g., "days")
 * @param size The size variant of the badge
 */
@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    size: StreakBadgeSize = StreakBadgeSize.Medium
) {
    val isActive = streak > 0

    // Animate scale for active streak
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "streak_scale"
    )

    // Animate background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            StreakOrange.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "streak_background"
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) {
            StreakOrange
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        label = "streak_text_color"
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(size.cornerRadius),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = size.horizontalPadding,
                vertical = size.verticalPadding
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(size.spacing)
        ) {
            // Fire icon
            Icon(
                imageVector = Icons.Filled.Whatshot,
                contentDescription = "連続記録",
                tint = if (isActive) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(size.emojiSize.value.dp)
            )

            // Streak count
            Text(
                text = if (showLabel) {
                    "連続 ${streak}日"
                } else {
                    streak.toString()
                },
                color = textColor,
                fontSize = size.textSize,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * A Japanese-styled streak badge showing "X days in a row" in Japanese.
 *
 * @param streak The current streak in days
 * @param modifier Modifier for the component
 */
@Composable
fun StreakBadgeJapanese(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            StreakOrange.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "streak_background_jp"
    )

    val textColor by animateColorAsState(
        targetValue = if (isActive) {
            StreakOrange
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        label = "streak_text_color_jp"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Whatshot,
                contentDescription = "連続記録",
                tint = if (isActive) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "連続 ${streak}日目",
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * A larger streak display with gradient background for prominent display.
 *
 * @param streak The current streak in days
 * @param modifier Modifier for the component
 */
@Composable
fun StreakDisplay(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isActive = streak > 0

    Box(
        modifier = modifier
            .background(
                brush = if (isActive) {
                    Brush.linearGradient(
                        colors = listOf(
                            StreakOrange.copy(alpha = 0.2f),
                            StreakOrange.copy(alpha = 0.05f)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Whatshot,
                contentDescription = "連続記録",
                tint = if (isActive) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = streak.toString(),
                color = if (isActive) StreakOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "日",
                color = if (isActive) {
                    StreakOrange.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Size variants for the StreakBadge component.
 */
enum class StreakBadgeSize(
    val cornerRadius: androidx.compose.ui.unit.Dp,
    val horizontalPadding: androidx.compose.ui.unit.Dp,
    val verticalPadding: androidx.compose.ui.unit.Dp,
    val spacing: androidx.compose.ui.unit.Dp,
    val emojiSize: androidx.compose.ui.unit.TextUnit,
    val textSize: androidx.compose.ui.unit.TextUnit
) {
    Small(
        cornerRadius = 8.dp,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
        spacing = 4.dp,
        emojiSize = 12.sp,
        textSize = 12.sp
    ),
    Medium(
        cornerRadius = 12.dp,
        horizontalPadding = 12.dp,
        verticalPadding = 6.dp,
        spacing = 6.dp,
        emojiSize = 16.sp,
        textSize = 14.sp
    ),
    Large(
        cornerRadius = 16.dp,
        horizontalPadding = 16.dp,
        verticalPadding = 8.dp,
        spacing = 8.dp,
        emojiSize = 20.sp,
        textSize = 16.sp
    )
}

@Preview(showBackground = true)
@Composable
private fun StreakBadgePreview() {
    EnglishWordTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreakBadge(streak = 0, size = StreakBadgeSize.Small)
            StreakBadge(streak = 1, size = StreakBadgeSize.Medium)
            StreakBadge(streak = 7, size = StreakBadgeSize.Large)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakBadgeJapanesePreview() {
    EnglishWordTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StreakBadgeJapanese(streak = 0)
            StreakBadgeJapanese(streak = 5)
            StreakBadgeJapanese(streak = 30)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StreakDisplayPreview() {
    EnglishWordTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakDisplay(streak = 0)
            StreakDisplay(streak = 15)
        }
    }
}
