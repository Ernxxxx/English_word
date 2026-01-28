# DB設計書

## ER図

```
┌─────────────┐       ┌─────────────────┐
│   Level     │       │      Word       │
├─────────────┤       ├─────────────────┤
│ id (PK)     │──────<│ id (PK)         │
│ name        │       │ levelId (FK)    │
│ orderIndex  │       │ english         │
│ createdAt   │       │ japanese        │
│             │       │ exampleEn       │
│             │       │ exampleJa       │
│             │       │ masteryLevel    │
│             │       │ nextReviewAt    │
│             │       │ reviewCount     │
│             │       │ createdAt       │
│             │       │ updatedAt       │
└─────────────┘       └─────────────────┘

┌─────────────────┐       ┌─────────────────┐
│  StudySession   │       │   StudyRecord   │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │──────<│ id (PK)         │
│ levelId (FK)    │       │ sessionId (FK)  │
│ startedAt       │       │ wordId (FK)     │
│ completedAt     │       │ result          │
│ wordCount       │       │ reviewedAt      │
│ masteredCount   │       │                 │
└─────────────────┘       └─────────────────┘

┌─────────────────┐       ┌─────────────────┐
│   UserStats     │       │  UserSettings   │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ date (UNIQUE)   │       │ key             │
│ studiedCount    │       │ value           │
│ streak          │       │                 │
│ lastStudyDate   │       │                 │
└─────────────────┘       └─────────────────┘
```

---

## Entity定義

### 1. Level（レベル）

| カラム | 型 | 説明 |
|--------|-----|------|
| id | Long (PK) | 自動生成 |
| name | String | レベル名（基礎、標準、上級） |
| orderIndex | Int | 表示順 |
| createdAt | Long | 作成日時（epoch ms） |

```kotlin
@Entity(tableName = "levels")
data class Level(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val orderIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)
```

---

### 2. Word（単語）

| カラム | 型 | 説明 |
|--------|-----|------|
| id | Long (PK) | 自動生成 |
| levelId | Long (FK) | 所属レベル |
| english | String | 英単語 |
| japanese | String | 日本語訳 |
| exampleEn | String? | 例文（英語） |
| exampleJa | String? | 例文（日本語） |
| masteryLevel | Int | 習熟度 0-5 |
| nextReviewAt | Long | 次回復習日時 |
| reviewCount | Int | 復習回数 |
| createdAt | Long | 作成日時 |
| updatedAt | Long | 更新日時 |

```kotlin
@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = Level::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("levelId")]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val levelId: Long,
    val english: String,
    val japanese: String,
    val exampleEn: String? = null,
    val exampleJa: String? = null,
    val masteryLevel: Int = 0,        // 0:未学習, 1-5:習熟度
    val nextReviewAt: Long = 0,
    val reviewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

---

### 3. StudySession（学習セッション）

| カラム | 型 | 説明 |
|--------|-----|------|
| id | Long (PK) | 自動生成 |
| levelId | Long (FK) | 学習したレベル |
| startedAt | Long | 開始日時 |
| completedAt | Long? | 完了日時 |
| wordCount | Int | 学習単語数 |
| masteredCount | Int | 覚えた数 |

```kotlin
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Level::class,
            parentColumns = ["id"],
            childColumns = ["levelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("levelId")]
)
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val levelId: Long,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val wordCount: Int = 0,
    val masteredCount: Int = 0
)
```

---

### 4. StudyRecord（学習記録）

| カラム | 型 | 説明 |
|--------|-----|------|
| id | Long (PK) | 自動生成 |
| sessionId | Long (FK) | セッションID |
| wordId | Long (FK) | 単語ID |
| result | Int | 結果（0:まだ, 1:あとで, 2:覚えた） |
| reviewedAt | Long | 回答日時 |

```kotlin
@Entity(
    tableName = "study_records",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Word::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("wordId")]
)
data class StudyRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val wordId: Long,
    val result: Int,  // 0:AGAIN, 1:LATER, 2:KNOWN
    val reviewedAt: Long = System.currentTimeMillis()
)

// 結果の定数
object ReviewResult {
    const val AGAIN = 0   // まだ
    const val LATER = 1   // あとで
    const val KNOWN = 2   // 覚えた
}
```

---

### 5. UserStats（日別統計）

| カラム | 型 | 説明 |
|--------|-----|------|
| id | Long (PK) | 自動生成 |
| date | String (UNIQUE) | 日付 (yyyy-MM-dd) |
| studiedCount | Int | 学習単語数 |
| streak | Int | その時点の連続日数 |
| lastStudyDate | String? | 最終学習日 |

```kotlin
@Entity(
    tableName = "user_stats",
    indices = [Index(value = ["date"], unique = true)]
)
data class UserStats(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,              // yyyy-MM-dd
    val studiedCount: Int = 0,
    val streak: Int = 0,
    val lastStudyDate: String? = null
)
```

---

### 6. UserSettings（設定）

| カラム | 型 | 説明 |
|--------|-----|------|
| key | String (PK) | 設定キー |
| value | String | 設定値 |

```kotlin
@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey
    val key: String,
    val value: String
)

// 設定キー
object SettingsKey {
    const val DAILY_GOAL = "daily_goal"           // 1日の目標
    const val DARK_MODE = "dark_mode"             // ダークモード
    const val IS_PREMIUM = "is_premium"           // Premium状態
    const val ONBOARDING_DONE = "onboarding_done" // オンボーディング完了
    const val NOTIFICATION = "notification"        // 通知設定
}
```

---

## DAO定義

### LevelDao

```kotlin
@Dao
interface LevelDao {
    @Query("SELECT * FROM levels ORDER BY orderIndex ASC")
    fun getAllLevels(): Flow<List<Level>>

    @Query("SELECT * FROM levels WHERE id = :id")
    suspend fun getById(id: Long): Level?

    @Query("SELECT COUNT(*) FROM levels")
    suspend fun getCount(): Int

    @Insert
    suspend fun insert(level: Level): Long

    @Update
    suspend fun update(level: Level)

    @Delete
    suspend fun delete(level: Level)
}
```

### WordDao

```kotlin
@Dao
interface WordDao {
    @Query("SELECT * FROM words WHERE levelId = :levelId ORDER BY id ASC")
    fun getWordsByLevel(levelId: Long): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun getById(id: Long): Word?

    @Query("SELECT COUNT(*) FROM words WHERE levelId = :levelId")
    suspend fun getCountByLevel(levelId: Long): Int

    @Query("SELECT COUNT(*) FROM words WHERE levelId = :levelId AND masteryLevel >= 3")
    suspend fun getMasteredCountByLevel(levelId: Long): Int

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getTotalCount(): Int

    // 学習用: 復習が必要な単語を取得（SRS）
    @Query("""
        SELECT * FROM words
        WHERE levelId = :levelId
        AND nextReviewAt <= :now
        ORDER BY masteryLevel ASC, nextReviewAt ASC
        LIMIT :limit
    """)
    suspend fun getWordsForReview(levelId: Long, now: Long, limit: Int): List<Word>

    @Insert
    suspend fun insert(word: Word): Long

    @Insert
    suspend fun insertAll(words: List<Word>)

    @Update
    suspend fun update(word: Word)

    @Delete
    suspend fun delete(word: Word)
}
```

### StudySessionDao

```kotlin
@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: Long): StudySession?

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC LIMIT 10")
    fun getRecentSessions(): Flow<List<StudySession>>

    @Insert
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)
}
```

### StudyRecordDao

```kotlin
@Dao
interface StudyRecordDao {
    @Query("SELECT * FROM study_records WHERE sessionId = :sessionId")
    suspend fun getBySession(sessionId: Long): List<StudyRecord>

    @Insert
    suspend fun insert(record: StudyRecord): Long
}
```

### UserStatsDao

```kotlin
@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE date = :date")
    suspend fun getByDate(date: String): UserStats?

    @Query("SELECT * FROM user_stats ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): UserStats?

    @Query("SELECT SUM(studiedCount) FROM user_stats WHERE date = :date")
    suspend fun getTodayStudiedCount(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStats)
}
```

### UserSettingsDao

```kotlin
@Dao
interface UserSettingsDao {
    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM user_settings WHERE `key` = :key")
    fun getValueFlow(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(settings: UserSettings)
}
```

---

## Database定義

```kotlin
@Database(
    entities = [
        Level::class,
        Word::class,
        StudySession::class,
        StudyRecord::class,
        UserStats::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
    abstract fun wordDao(): WordDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun userSettingsDao(): UserSettingsDao
}
```

---

## SRSロジック（簡易版）

```kotlin
object SrsCalculator {
    // masteryLevel別の次回復習間隔（時間）
    private val intervals = listOf(
        0,      // 0: 即時
        1,      // 1: 1時間後
        8,      // 2: 8時間後
        24,     // 3: 1日後
        72,     // 4: 3日後
        168     // 5: 7日後
    )

    fun calculateNextReview(currentLevel: Int, result: Int): Pair<Int, Long> {
        val newLevel = when (result) {
            ReviewResult.AGAIN -> maxOf(0, currentLevel - 1)
            ReviewResult.LATER -> currentLevel  // 変更なし
            ReviewResult.KNOWN -> minOf(5, currentLevel + 1)
            else -> currentLevel
        }

        val hoursUntilNext = intervals.getOrElse(newLevel) { 168 }
        val nextReviewAt = System.currentTimeMillis() + (hoursUntilNext * 60 * 60 * 1000L)

        return Pair(newLevel, nextReviewAt)
    }
}
```
