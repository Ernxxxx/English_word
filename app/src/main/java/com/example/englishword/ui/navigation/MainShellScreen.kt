package com.example.englishword.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.ui.components.SimpleBannerAdView
import com.example.englishword.ui.home.HomeTab
import com.example.englishword.ui.settings.SettingsTab
import com.example.englishword.ui.stats.StatsTab
import com.example.englishword.ui.theme.PremiumGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val isPremium: StateFlow<Boolean> = settingsRepository.isPremium()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )
}

private enum class ShellTab(
    val icon: ImageVector,
    val label: String,
    val title: String
) {
    HOME(Icons.Default.Home, "ホーム", "英単語帳"),
    STATS(Icons.Default.BarChart, "統計", "学習統計"),
    SETTINGS(Icons.Default.Settings, "設定", "設定")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShellScreen(
    onNavigateToStudy: (Long) -> Unit,
    onNavigateToUnitTest: (Long) -> Unit,
    onNavigateToWordList: (Long) -> Unit,
    onNavigateToPremium: () -> Unit,
    shellViewModel: ShellViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val currentTab = ShellTab.entries.getOrElse(selectedTab) { ShellTab.entries.first() }
    val isPremium by shellViewModel.isPremium.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentTab.title,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (currentTab == ShellTab.HOME && !isPremium) {
                        IconButton(onClick = onNavigateToPremium) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "プレミアム",
                                tint = PremiumGold
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                if (!isPremium) {
                    SimpleBannerAdView(
                        isPremium = isPremium
                    )
                }
                NavigationBar {
                    ShellTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Crossfade provides animated transition between tabs.
        // Note: For true state preservation across tab switches, consider
        // using SaveableStateHolder or NavHost with bottom nav integration.
        Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
            when (tab) {
                ShellTab.HOME -> HomeTab(
                    onNavigateToStudy = onNavigateToStudy,
                    onNavigateToUnitTest = onNavigateToUnitTest,
                    onNavigateToWordList = onNavigateToWordList,
                    onNavigateToPremium = onNavigateToPremium,
                    modifier = Modifier.padding(paddingValues)
                )
                ShellTab.STATS -> StatsTab(
                    modifier = Modifier.padding(paddingValues)
                )
                ShellTab.SETTINGS -> SettingsTab(
                    onNavigateToPremium = onNavigateToPremium,
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
