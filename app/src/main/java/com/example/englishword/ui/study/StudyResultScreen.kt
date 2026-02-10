package com.example.englishword.ui.study

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishword.ads.AdManager
import com.example.englishword.ui.components.BannerAdView
import com.example.englishword.ui.theme.CorrectGreen
import com.example.englishword.ui.theme.EnglishWordTheme
import com.example.englishword.ui.theme.IncorrectRed
import com.example.englishword.ui.theme.StreakOrange
import kotlinx.coroutines.delay

/**
 * Study result screen showing session statistics.
 */
@Composable
fun StudyResultScreen(
    sessionId: Long,
    onNavigateToHome: () -> Unit,
    onNavigateToStudy: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPremium by viewModel.adManager.isPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Added: Track if interstitial ad has been shown for this session
    var adShown by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is StudyUiState.Completed -> {
            // Added: Show interstitial ad when result screen appears (with frequency cap)
            LaunchedEffect(state.sessionId) {
                if (!adShown && activity != null) {
                    viewModel.adManager.showInterstitialWithFrequencyCap(
                        activity = activity,
                        onComplete = {
                            adShown = true
                        }
                    )
                }
            }

            StudyResultContent(
                state = state,
                isPremium = isPremium,
                onNavigateToHome = onNavigateToHome,
                onRetry = { onNavigateToStudy(state.levelId) },
                modifier = modifier
            )
        }
        else -> {
            // Show loading if state is not yet available
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * Stateless result content composable.
 */
@Composable
fun StudyResultContent(
    state: StudyUiState.Completed,
    isPremium: Boolean = false,
    onNavigateToHome: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Banner ad at bottom (hidden for premium users)
            if (!isPremium) {
                BannerAdView(
                    adUnitId = AdManager.BANNER_AD_UNIT_ID,
                    isPremium = isPremium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Celebration header
                AnimatedVisibility(
                    visible = showContent,
                    enter = scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn()
                ) {
                    CelebrationHeader()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Stats card
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 200
                        )
                    )
                ) {
                    StatsCard(
                        totalCount = state.totalCount,
                        knownCount = state.knownCount,
                        againCount = state.againCount,
                        laterCount = state.laterCount
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Streak display
                AnimatedVisibility(
                    visible = showContent && state.streak > 0,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 400,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 400
                        )
                    )
                ) {
                    StreakDisplay(streak = state.streak)
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action buttons
                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 600,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 600
                        )
                    )
                ) {
                    ActionButtons(
                        onNavigateToHome = onNavigateToHome,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

/**
 * Celebration header with emoji and title.
 */
@Composable
private fun CelebrationHeader() {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "お疲れさまでした！",
            fontSize = 64.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "学習完了！",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Stats card showing study results.
 */
@Composable
private fun StatsCard(
    totalCount: Int,
    knownCount: Int,
    againCount: Int,
    laterCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "今日の学習",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${totalCount}語",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Check,
                    label = "覚えた",
                    count = knownCount,
                    color = CorrectGreen
                )
                StatItem(
                    icon = Icons.Default.Close,
                    label = "要復習",
                    count = againCount,
                    color = IncorrectRed
                )
                StatItem(
                    icon = Icons.Default.Refresh,
                    label = "あとで",
                    count = laterCount,
                    color = StreakOrange
                )
            }
        }
    }
}

/**
 * Individual stat item.
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Streak display with fire icon.
 */
@Composable
private fun StreakDisplay(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = StreakOrange.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "連続記録",
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${streak}日継続中！",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StreakOrange
            )
        }
    }
}

/**
 * Action buttons for navigation.
 */
@Composable
private fun ActionButtons(
    onNavigateToHome: () -> Unit,
    onRetry: () -> Unit
) {
    var actionLocked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                if (!actionLocked) {
                    actionLocked = true
                    onNavigateToHome()
                }
            },
            enabled = !actionLocked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ホームに戻る",
                style = MaterialTheme.typography.titleMedium
            )
        }

        OutlinedButton(
            onClick = {
                if (!actionLocked) {
                    actionLocked = true
                    onRetry()
                }
            },
            enabled = !actionLocked,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "もう一度学習",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyResultContentPreview() {
    EnglishWordTheme {
        StudyResultContent(
            state = StudyUiState.Completed(
                sessionId = 1,
                levelId = 1,
                totalCount = 20,
                knownCount = 15,
                againCount = 3,
                laterCount = 2,
                streak = 5
            ),
            onNavigateToHome = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyResultContentNoStreakPreview() {
    EnglishWordTheme {
        StudyResultContent(
            state = StudyUiState.Completed(
                sessionId = 1,
                levelId = 1,
                totalCount = 10,
                knownCount = 8,
                againCount = 2,
                laterCount = 0,
                streak = 0
            ),
            onNavigateToHome = {},
            onRetry = {}
        )
    }
}
