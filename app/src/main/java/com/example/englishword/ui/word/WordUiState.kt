package com.example.englishword.ui.word

import com.example.englishword.data.local.entity.Level
import com.example.englishword.data.local.entity.Word

/**
 * UI state for word list screen
 */
data class WordListUiState(
    val words: List<Word> = emptyList(),
    val filteredWords: List<Word> = emptyList(),
    val level: Level? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isPremium: Boolean = false
)

/**
 * UI state for word edit screen
 */
data class WordEditUiState(
    val wordId: Long? = null,
    val levelId: Long = 0,
    val english: String = "",
    val japanese: String = "",
    val exampleEn: String = "",
    val exampleJa: String = "",
    val selectedLevelId: Long = 0,
    val levels: List<Level> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isPremium: Boolean = false,
    val totalWordCount: Int = 0,
    val maxFreeWords: Int = 200,
    val englishError: String? = null,
    val japaneseError: String? = null,
    val saveError: String? = null,
    val saveSuccess: Boolean = false,
    val deleteSuccess: Boolean = false
) {
    val isValid: Boolean
        get() = english.isNotBlank() && japanese.isNotBlank() && englishError == null && japaneseError == null

    val canAddMoreWords: Boolean
        get() = isPremium || totalWordCount < maxFreeWords

    val remainingFreeWords: Int
        get() = (maxFreeWords - totalWordCount).coerceAtLeast(0)
}

/**
 * Events for word list screen
 */
sealed class WordListEvent {
    data class SearchQueryChanged(val query: String) : WordListEvent()
    data class DeleteWord(val word: Word) : WordListEvent()
    object ClearError : WordListEvent()
    object Refresh : WordListEvent()
}

/**
 * Events for word edit screen
 */
sealed class WordEditEvent {
    data class EnglishChanged(val value: String) : WordEditEvent()
    data class JapaneseChanged(val value: String) : WordEditEvent()
    data class ExampleEnChanged(val value: String) : WordEditEvent()
    data class ExampleJaChanged(val value: String) : WordEditEvent()
    data class LevelChanged(val levelId: Long) : WordEditEvent()
    object Save : WordEditEvent()
    object Delete : WordEditEvent()
    object ClearError : WordEditEvent()
    object ClearSuccess : WordEditEvent()
}
