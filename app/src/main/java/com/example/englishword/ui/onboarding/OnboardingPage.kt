package com.example.englishword.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.englishword.ui.theme.Blue40
import com.example.englishword.ui.theme.Blue80
import com.example.englishword.ui.theme.Purple40
import com.example.englishword.ui.theme.Purple80
import com.example.englishword.ui.theme.StreakOrange
import com.example.englishword.ui.theme.Teal40
import com.example.englishword.ui.theme.Teal80
import kotlinx.coroutines.delay

/**
 * Data class representing an onboarding page
 */
data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color
)

/**
 * Predefined onboarding pages for the English Word app
 */
object OnboardingPages {
    val pages = listOf(
        OnboardingPageData(
            icon = Icons.Default.School,
            title = "学年別に学習",
            description = "中学1年〜高校3年まで\n6,000語以上の英単語を収録\nあなたの学年に合わせて学習できます",
            primaryColor = Blue40,
            secondaryColor = Blue80
        ),
        OnboardingPageData(
            icon = Icons.Default.TouchApp,
            title = "カードで覚える",
            description = "カードをタップして意味を確認\n覚えた・もう一度の評価ボタンで\n記憶に定着させましょう",
            primaryColor = Teal40,
            secondaryColor = Teal80
        ),
        OnboardingPageData(
            icon = Icons.Default.LocalFireDepartment,
            title = "毎日続けよう",
            description = "連続学習日数(ストリーク)を\n積み重ねてモチベーションアップ\n毎日の習慣で確実に身につきます",
            primaryColor = StreakOrange,
            secondaryColor = Color(0xFFFFE0B2)
        )
    )
}

/**
 * Reusable composable for each onboarding page
 */
@Composable
fun OnboardingPage(
    pageData: OnboardingPageData,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(100)
            showContent = true
        } else {
            showContent = false
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with gradient background
        AnimatedVisibility(
            visible = showContent,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(iconScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                pageData.secondaryColor.copy(alpha = 0.3f),
                                pageData.secondaryColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    pageData.primaryColor,
                                    pageData.primaryColor.copy(alpha = 0.8f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pageData.icon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        AnimatedVisibility(
            visible = showContent,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Text(
                text = pageData.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        AnimatedVisibility(
            visible = showContent,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Text(
                text = pageData.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
            )
        }
    }
}
