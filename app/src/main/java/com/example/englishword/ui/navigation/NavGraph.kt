package com.example.englishword.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.englishword.ui.onboarding.OnboardingScreen
import com.example.englishword.ui.settings.PremiumScreen
import com.example.englishword.ui.study.StudyResultScreen
import com.example.englishword.ui.study.StudyScreen
import com.example.englishword.ui.study.StudyViewModel
import com.example.englishword.ui.test.UnitTestScreen
import com.example.englishword.ui.word.WordEditScreen
import com.example.englishword.ui.word.WordListScreen

/**
 * Navigation routes for the English Word app
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STUDY = "study/{levelId}"
    const val UNIT_TEST = "unitTest/{levelId}"
    const val STUDY_RESULT = "studyResult/{sessionId}"
    const val WORD_LIST = "wordList/{levelId}"
    const val WORD_EDIT = "wordEdit/{levelId}?wordId={wordId}"
    const val SETTINGS = "settings"
    const val PREMIUM = "premium"
    const val STATS = "stats"

    // Helper functions to create routes with arguments
    fun study(levelId: Long): String = "study/$levelId"
    fun unitTest(levelId: Long): String = "unitTest/$levelId"
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
        composable(
            route = Routes.ONBOARDING,
            enterTransition = { fadeIn(tween(500)) },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            OnboardingScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Home screen (MainShellScreen with bottom navigation)
        composable(
            route = Routes.HOME,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            MainShellScreen(
                onNavigateToStudy = { levelId ->
                    navController.navigate(Routes.study(levelId))
                },
                onNavigateToUnitTest = { levelId ->
                    navController.navigate(Routes.unitTest(levelId))
                },
                onNavigateToWordList = { levelId ->
                    navController.navigate(Routes.wordList(levelId))
                },
                onNavigateToPremium = {
                    navController.navigate(Routes.PREMIUM) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Study screen
        composable(
            route = Routes.STUDY,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) },
            arguments = listOf(
                navArgument(NavArgs.LEVEL_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getLong(NavArgs.LEVEL_ID) ?: 0L
            val studyViewModel: StudyViewModel = hiltViewModel()
            StudyScreen(
                levelId = levelId,
                onNavigateToResult = { sessionId ->
                    navController.navigate(Routes.studyResult(sessionId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
                viewModel = studyViewModel
            )
        }

        // Unit test screen
        composable(
            route = Routes.UNIT_TEST,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) },
            arguments = listOf(
                navArgument(NavArgs.LEVEL_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getLong(NavArgs.LEVEL_ID) ?: 0L
            UnitTestScreen(
                levelId = levelId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Study result screen
        composable(
            route = Routes.STUDY_RESULT,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) },
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
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToStudy = { levelId ->
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
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) },
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
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150)) },
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

        // Premium screen
        composable(
            route = Routes.PREMIUM,
            enterTransition = { slideInVertically(tween(300)) { it / 2 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutVertically(tween(300)) { it / 2 } + fadeOut(tween(150)) }
        ) {
            PremiumScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Note: Stats and Settings screens are now embedded in MainShellScreen's bottom tabs
    }
}
