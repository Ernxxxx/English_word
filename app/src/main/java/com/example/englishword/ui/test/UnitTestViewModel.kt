package com.example.englishword.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.data.local.entity.Word
import com.example.englishword.data.repository.WordRepository
import com.example.englishword.ui.quiz.QuizGenerator
import com.example.englishword.ui.quiz.QuizOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

data class UnitTestUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val allWordsInLevel: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val quizOptions: QuizOptions? = null,
    val selectedIndex: Int? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean? = null,
    val score: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val availableWordCount: Int = 0,
    val isReadyToSelect: Boolean = false
) {
    val currentWord: Word?
        get() = words.getOrNull(currentIndex)

    val totalCount: Int
        get() = words.size

    val progress: Int
        get() = if (words.isEmpty()) 0 else (currentIndex + 1).coerceAtMost(words.size)
}

@HiltViewModel
class UnitTestViewModel @Inject constructor(
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitTestUiState())
    val uiState: StateFlow<UnitTestUiState> = _uiState.asStateFlow()

    private var loadedLevelId: Long? = null
    private var cachedAllWords: List<Word> = emptyList()

    /**
     * Phase 1: Load word count and show the count selector.
     */
    fun load(levelId: Long) {
        val current = _uiState.value
        if (loadedLevelId == levelId && (current.words.isNotEmpty() || current.isCompleted)) return

        loadedLevelId = levelId
        viewModelScope.launch {
            _uiState.value = UnitTestUiState(isLoading = true)

            val allWords = wordRepository.getWordsByLevelSync(levelId)
            if (allWords.isEmpty()) {
                _uiState.value = UnitTestUiState(
                    isLoading = false,
                    error = "このユニットに単語がありません"
                )
                return@launch
            }

            if (!QuizGenerator.canGenerateQuiz(allWords)) {
                _uiState.value = UnitTestUiState(
                    isLoading = false,
                    error = "単語テストには異なる選択肢が4つ以上必要です"
                )
                return@launch
            }

            cachedAllWords = allWords
            _uiState.value = UnitTestUiState(
                isLoading = false,
                availableWordCount = allWords.size,
                isReadyToSelect = true
            )
        }
    }

    /**
     * Phase 2: Start test with the selected question count.
     */
    fun startTest(limit: Int) {
        val allWords = cachedAllWords
        if (allWords.isEmpty()) return

        val testWords = allWords.shuffled().take(limit)
        val firstWord = testWords.first()
        val firstOptions = generateOptions(firstWord, allWords)

        if (firstOptions == null) {
            _uiState.value = UnitTestUiState(
                isLoading = false,
                error = "テスト問題の生成に失敗しました"
            )
            return
        }

        _uiState.value = UnitTestUiState(
            isLoading = false,
            words = testWords,
            allWordsInLevel = allWords,
            currentIndex = 0,
            quizOptions = firstOptions,
            selectedIndex = null,
            isAnswered = false,
            isCorrect = null,
            score = 0,
            isCompleted = false,
            error = null,
            availableWordCount = allWords.size,
            isReadyToSelect = false
        )
    }

    fun selectAnswer(index: Int) {
        val state = _uiState.value
        if (state.isLoading || state.isCompleted || state.isAnswered) return

        val currentWord = state.currentWord ?: return
        val options = state.quizOptions ?: return
        val correct = index == options.correctIndex

        _uiState.update {
            it.copy(
                selectedIndex = index,
                isAnswered = true,
                isCorrect = correct,
                score = if (correct) it.score + 1 else it.score
            )
        }

        if (correct && !currentWord.isAcquired) {
            viewModelScope.launch(NonCancellable) {
                wordRepository.markWordAcquired(currentWord.id)
            }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.isLoading || state.isCompleted || !state.isAnswered) return

        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.words.size) {
            _uiState.update { it.copy(isCompleted = true, error = null) }
            return
        }

        val nextWord = state.words[nextIndex]
        val nextOptions = generateOptions(nextWord, state.allWordsInLevel)
        if (nextOptions == null) {
            _uiState.update {
                it.copy(
                    error = "次の問題の生成に失敗しました",
                    isCompleted = true
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                currentIndex = nextIndex,
                quizOptions = nextOptions,
                selectedIndex = null,
                isAnswered = false,
                isCorrect = null
            )
        }
    }

    fun restart() {
        val levelId = loadedLevelId ?: return
        loadedLevelId = null
        load(levelId)
    }

    private fun generateOptions(
        word: Word,
        allWordsInLevel: List<Word>
    ): QuizOptions? {
        return QuizGenerator.generateOptions(
            correctWord = word,
            allWordsInLevel = allWordsInLevel,
            allWords = emptyList(),
            isReversed = false
        )
    }
}
