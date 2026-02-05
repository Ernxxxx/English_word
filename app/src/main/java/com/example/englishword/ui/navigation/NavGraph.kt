package com.example.englishword.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.englishword.ui.home.HomeScreen
import com.example.englishword.ui.onboarding.OnboardingScreen
import com.example.englishword.ui.settings.PremiumScreen
import com.example.englishword.ui.settings.SettingsScreen
import com.example.englishword.ui.study.StudyResultScreen
import com.example.englishword.ui.study.StudyScreen
import com.example.englishword.ui.word.WordEditScreen
import com.example.englishword.ui.word.WordListScreen

/**
 * Navigation routes for the English Word app
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STUDY = "study/{levelId}"
    const val STUDY_RESULT = "studyResult/{sessionId}"
    const val WORD_LIST = "wordList/{levelId}"
    const val WORD_EDIT = "wordEdit/{levelId}?wordId={wordId}"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium"

    // Helper functions to create routes with arguments
    fun study(levelId: Long): String = "study/$levelId"
    fun studyResult(sessionId: Long): String = "studyResult/$sessionId"
    fun wordList(levelId: Long): String = "wordList/$levelId"
    fun wordEdit(levelId: Long, wordId: Long? = null): String {
        return if (wordId != null) {
            "wordEdit/$levelId?wordId=$wordId"
        } else {
            "wordEdit/$levelId"
        }
    }
}

/**
 * Navigation arguments
 */
object NavArgs {
    const val LEVEL_ID = "levelId"
    const val SESSION_ID = "sessionId"
    const val WORD_ID = "wordId"
}

/**
 * Main navigation graph for the English Word app
 */
@Composable
fun EnglishWordNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ONBOARDING
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Onboarding screen
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Home screen
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToStudy = { levelId ->
                    navController.navigate(Routes.study(levelId))
                },
                onNavigateToWordList = { levelId ->
                    navController.navigate(Routes.wordList(levelId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToPremium = {
                    navController.navigate(Routes.PREMIUM)
                }
            )
        }

        // Study screen
        composable(
            route = Routes.STUDY,
            arguments = listOf(
                navArgument(NavArgs.LEVEL_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getLong(NavArgs.LEVEL_ID) ?: 0L
            StudyScreen(
                levelId = levelId,
                onNavigateToResult = { sessionId ->
                    navController.navigate(Routes.studyResult(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Study result screen
        composable(
            route = Routes.STUDY_RESULT,
            arguments = listOf(
                navArgument(NavArgs.SESSION_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(NavArgs.SESSION_ID) ?: 0L
            StudyResultScreen(
                sessionId = sessionId,
                onNavigateToHome = {
                    // Clear entire back stack and navigate to HOME
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToStudy = { levelId ->
                    // Keep HOME in back stack, replace current screen with Study
                    navController.navigate(Routes.study(levelId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // Word list screen
        composable(
            route = Routes.WORD_LIST,
            arguments = listOf(
                navArgument(NavArgs.LEVEL_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getLong(NavArgs.LEVEL_ID) ?: 0L
            WordListScreen(
                levelId = levelId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Word edit screen (create or edit)
        composable(
            route = Routes.WORD_EDIT,
            arguments = listOf(
                navArgument(NavArgs.LEVEL_ID) {
                    type = NavType.LongType
                },
                navArgument(NavArgs.WORD_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getLong(NavArgs.LEVEL_ID) ?: 0L
            val wordId = backStackEntry.arguments?.getLong(NavArgs.WORD_ID)?.let {
                if (it == -1L) null else it
            }
            WordEditScreen(
                levelId = levelId,
                wordId = wordId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Settings screen
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPremium = {
                    navController.navigate(Routes.PREMIUM)
                }
            )
        }

        // Premium screen
        composable(Routes.PREMIUM) {
            PremiumScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Navigation helper extension functions
 */
fun NavHostController.navigateToHome() {
    navigate(Routes.HOME) {
        popUpTo(Routes.ONBOARDING) { inclusive = true }
    }
}

fun NavHostController.navigateToStudy(levelId: Long) {
    navigate(Routes.study(levelId))
}

fun NavHostController.navigateToStudyResult(sessionId: Long) {
    navigate(Routes.studyResult(sessionId)) {
        popUpTo(Routes.HOME)
    }
}

fun NavHostController.navigateToWordList(levelId: Long) {
    navigate(Routes.wordList(levelId))
}

fun NavHostController.navigateToWordEdit(levelId: Long, wordId: Long? = null) {
    navigate(Routes.wordEdit(levelId, wordId))
}

fun NavHostController.navigateToSettings() {
    navigate(Routes.SETTINGS)
}

fun NavHostController.navigateToPremium() {
    navigate(Routes.PREMIUM)
}
