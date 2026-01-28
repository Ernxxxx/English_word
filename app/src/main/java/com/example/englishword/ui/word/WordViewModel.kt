package com.example.englishword.ui.word

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.local.entity.Word
import com.example.englishword.data.repository.LevelRepository
import com.example.englishword.data.repository.SettingsRepository
import com.example.englishword.data.repository.WordRepository
import com.example.englishword.ui.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Word List screen
 */
@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val levelRepository: LevelRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelId: Long = savedStateHandle.get<Long>(NavArgs.LEVEL_ID) ?: 0L

    private val _uiState = MutableStateFlow(WordListUiState())
    val uiState: StateFlow<WordListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load level info
                levelRepository.getLevelById(levelId).collectLatest { level ->
                    _uiState.update { it.copy(level = level) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "レベル情報の読み込みに失敗しました") }
            }
        }

        viewModelScope.launch {
            try {
                // Load words for this level
                wordRepository.getWordsByLevel(levelId).collectLatest { words ->
                    val searchQuery = _uiState.value.searchQuery
                    _uiState.update { state ->
                        state.copy(
                            words = words,
                            filteredWords = filterWords(words, searchQuery),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "単語の読み込みに失敗しました") }
            }
        }
    }

    fun onEvent(event: WordListEvent) {
        when (event) {
            is WordListEvent.SearchQueryChanged -> {
                _uiState.update { state ->
                    state.copy(
                        searchQuery = event.query,
                        filteredWords = filterWords(state.words, event.query)
                    )
                }
            }
            is WordListEvent.DeleteWord -> {
                deleteWord(event.word)
            }
            WordListEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            WordListEvent.Refresh -> {
                loadData()
            }
        }
    }

    private fun filterWords(words: List<Word>, query: String): List<Word> {
        if (query.isBlank()) return words
        val lowerQuery = query.lowercase()
        return words.filter { word ->
            word.english.lowercase().contains(lowerQuery) ||
            word.japanese.contains(query)
        }
    }

    private fun deleteWord(word: Word) {
        viewModelScope.launch {
            try {
                val success = wordRepository.deleteWord(word)
                if (!success) {
                    _uiState.update { it.copy(error = "単語の削除に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "単語の削除に失敗しました") }
            }
        }
    }
}

/**
 * ViewModel for Word Edit screen
 */
@HiltViewModel
class WordEditViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val levelRepository: LevelRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val levelId: Long = savedStateHandle.get<Long>(NavArgs.LEVEL_ID) ?: 0L
    private val wordId: Long? = savedStateHandle.get<Long>(NavArgs.WORD_ID)?.let {
        if (it == -1L) null else it
    }

    private val _uiState = MutableStateFlow(WordEditUiState(
        levelId = levelId,
        selectedLevelId = levelId,
        isEditMode = wordId != null,
        wordId = wordId
    ))
    val uiState: StateFlow<WordEditUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load premium status
                val isPremium = settingsRepository.isPremiumSync()

                // Load total word count
                val totalWordCount = wordRepository.getTotalWordCount().first()

                // Load all levels
                val levels = levelRepository.getAllLevels().first()

                _uiState.update { state ->
                    state.copy(
                        isPremium = isPremium,
                        totalWordCount = totalWordCount,
                        levels = levels,
                        isLoading = false
                    )
                }

                // If editing, load the word
                if (wordId != null) {
                    val word = wordRepository.getWordByIdSync(wordId)
                    if (word != null) {
                        _uiState.update { state ->
                            state.copy(
                                english = word.english,
                                japanese = word.japanese,
                                exampleEn = word.exampleEn ?: "",
                                exampleJa = word.exampleJa ?: "",
                                selectedLevelId = word.levelId
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, saveError = "データの読み込みに失敗しました") }
            }
        }
    }

    fun onEvent(event: WordEditEvent) {
        when (event) {
            is WordEditEvent.EnglishChanged -> {
                _uiState.update { state ->
                    state.copy(
                        english = event.value,
                        englishError = validateEnglish(event.value)
                    )
                }
            }
            is WordEditEvent.JapaneseChanged -> {
                _uiState.update { state ->
                    state.copy(
                        japanese = event.value,
                        japaneseError = validateJapanese(event.value)
                    )
                }
            }
            is WordEditEvent.ExampleEnChanged -> {
                _uiState.update { it.copy(exampleEn = event.value) }
            }
            is WordEditEvent.ExampleJaChanged -> {
                _uiState.update { it.copy(exampleJa = event.value) }
            }
            is WordEditEvent.LevelChanged -> {
                _uiState.update { it.copy(selectedLevelId = event.levelId) }
            }
            WordEditEvent.Save -> {
                saveWord()
            }
            WordEditEvent.Delete -> {
                deleteWord()
            }
            WordEditEvent.ClearError -> {
                _uiState.update { it.copy(saveError = null) }
            }
            WordEditEvent.ClearSuccess -> {
                _uiState.update { it.copy(saveSuccess = false, deleteSuccess = false) }
            }
        }
    }

    private fun validateEnglish(value: String): String? {
        return when {
            value.isBlank() -> "英単語を入力してください"
            value.length > 100 -> "100文字以内で入力してください"
            else -> null
        }
    }

    private fun validateJapanese(value: String): String? {
        return when {
            value.isBlank() -> "日本語訳を入力してください"
            value.length > 200 -> "200文字以内で入力してください"
            else -> null
        }
    }

    private fun saveWord() {
        val state = _uiState.value

        // Validate
        val englishError = validateEnglish(state.english)
        val japaneseError = validateJapanese(state.japanese)

        if (englishError != null || japaneseError != null) {
            _uiState.update { it.copy(englishError = englishError, japaneseError = japaneseError) }
            return
        }

        // Check free tier limit for new words
        if (!state.isEditMode && !state.canAddMoreWords) {
            _uiState.update {
                it.copy(saveError = "無料版では${state.maxFreeWords}語まで登録できます。プレミアム版にアップグレードしてください。")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = null) }

            try {
                val success = if (state.isEditMode && state.wordId != null) {
                    // Update existing word
                    val existingWord = wordRepository.getWordByIdSync(state.wordId)
                    if (existingWord != null) {
                        val updatedWord = existingWord.copy(
                            english = state.english.trim(),
                            japanese = state.japanese.trim(),
                            exampleEn = state.exampleEn.trim().takeIf { it.isNotEmpty() },
                            exampleJa = state.exampleJa.trim().takeIf { it.isNotEmpty() },
                            levelId = state.selectedLevelId
                        )
                        wordRepository.updateWord(updatedWord)
                    } else {
                        false
                    }
                } else {
                    // Insert new word
                    val newWordId = wordRepository.insertWord(
                        levelId = state.selectedLevelId,
                        english = state.english.trim(),
                        japanese = state.japanese.trim(),
                        exampleEn = state.exampleEn.trim().takeIf { it.isNotEmpty() },
                        exampleJa = state.exampleJa.trim().takeIf { it.isNotEmpty() }
                    )
                    newWordId != -1L
                }

                if (success) {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                } else {
                    _uiState.update { it.copy(isSaving = false, saveError = "保存に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveError = "保存に失敗しました: ${e.message}") }
            }
        }
    }

    private fun deleteWord() {
        val wordId = _uiState.value.wordId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            try {
                val success = wordRepository.deleteWord(wordId)
                if (success) {
                    _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                } else {
                    _uiState.update { it.copy(isDeleting = false, saveError = "削除に失敗しました") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, saveError = "削除に失敗しました: ${e.message}") }
            }
        }
    }
}
