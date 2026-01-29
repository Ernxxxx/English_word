package com.example.englishword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.ui.navigation.EnglishWordNavHost
import com.example.englishword.ui.theme.EnglishWordTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // ダークモード設定を監視
            val darkModeSetting by settingsRepository.getDarkMode().collectAsState(initial = SettingsRepository.DARK_MODE_SYSTEM)
            val isSystemDark = isSystemInDarkTheme()

            // 設定に基づいてダークモードを決定
            val isDarkTheme = when (darkModeSetting) {
                SettingsRepository.DARK_MODE_LIGHT -> false
                SettingsRepository.DARK_MODE_DARK -> true
                else -> isSystemDark // DARK_MODE_SYSTEM
            }

            EnglishWordTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    EnglishWordNavHost(navController = navController)
                }
            }
        }
    }
}
