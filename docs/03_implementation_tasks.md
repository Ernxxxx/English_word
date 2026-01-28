# 実装タスク分解

## フェーズ2: 実装タスク一覧

### Step5: プロジェクト雛形（1-2時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 5-1 | Android Studioプロジェクト作成 | プロジェクト基盤 |
| 5-2 | build.gradle依存関係設定 | Compose/Room/Hilt/Navigation |
| 5-3 | パッケージ構成作成 | data/domain/ui層 |
| 5-4 | Hilt DIセットアップ | Application/Module |
| 5-5 | Room Database定義 | Entity/DAO/Database |
| 5-6 | Navigation設定 | NavGraph |
| 5-7 | テーマ設定 | カラー/タイポグラフィ/ダークモード |

---

### Step6: 機能実装（8-12時間）

#### 6-A: データ層（2時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-A-1 | Repository実装 | LevelRepository, WordRepository |
| 6-A-2 | UseCase実装 | 学習/統計用UseCase |
| 6-A-3 | 初期データ投入 | 150語のプリインストール |

#### 6-B: オンボーディング（1時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-B-1 | OnboardingScreen | 3ページのPager |
| 6-B-2 | OnboardingViewModel | 完了フラグ管理 |

#### 6-C: ホーム画面（2時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-C-1 | HomeScreen | レベル一覧表示 |
| 6-C-2 | LevelCard | 進捗バー付きカード |
| 6-C-3 | HomeViewModel | レベル/統計取得 |
| 6-C-4 | レベル追加ダイアログ | Free制限チェック |

#### 6-D: 学習画面（3時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-D-1 | StudyScreen | カード学習UI |
| 6-D-2 | FlashCard | 表裏反転アニメーション |
| 6-D-3 | EvaluationButtons | 覚えた/まだ/あとで |
| 6-D-4 | StudyViewModel | SRS処理/セッション管理 |
| 6-D-5 | StudyResultScreen | 完了サマリー |

#### 6-E: 単語管理画面（2時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-E-1 | WordListScreen | 単語一覧 |
| 6-E-2 | WordEditScreen | 追加/編集フォーム |
| 6-E-3 | WordViewModel | CRUD処理 |
| 6-E-4 | スワイプ削除 | SwipeToDismiss |

#### 6-F: 設定画面（1時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 6-F-1 | SettingsScreen | 設定一覧 |
| 6-F-2 | SettingsViewModel | 設定読み書き |
| 6-F-3 | ダークモード切替 | テーマ反映 |

---

### Step7: 課金・広告実装（4-6時間）

#### 7-A: Google Play Billing（3時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 7-A-1 | BillingClient設定 | 依存関係/初期化 |
| 7-A-2 | BillingRepository | 購入/確認処理 |
| 7-A-3 | PremiumManager | 購入状態管理 |
| 7-A-4 | PremiumScreen | 購入UI |
| 7-A-5 | Free制限チェック | レベル/単語数制限 |

#### 7-B: AdMob広告（1時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 7-B-1 | AdMob SDK設定 | 依存関係/App ID |
| 7-B-2 | BannerAd | ホーム下部バナー |
| 7-B-3 | InterstitialAd | 学習完了時 |
| 7-B-4 | Premium時非表示 | 広告除去 |

#### 7-C: ドキュメント（1時間）

| # | タスク | 成果物 |
|---|--------|--------|
| 7-C-1 | プライバシーポリシー | Webページ/アプリ内 |
| 7-C-2 | 利用規約 | Webページ |

---

## ファイル構成（予定）

```
app/src/main/java/com/example/englishword/
├── di/
│   ├── AppModule.kt
│   └── DatabaseModule.kt
├── data/
│   ├── local/
│   │   ├── entity/
│   │   │   ├── Level.kt
│   │   │   ├── Word.kt
│   │   │   ├── StudySession.kt
│   │   │   ├── StudyRecord.kt
│   │   │   ├── UserStats.kt
│   │   │   └── UserSettings.kt
│   │   ├── dao/
│   │   │   ├── LevelDao.kt
│   │   │   ├── WordDao.kt
│   │   │   ├── StudySessionDao.kt
│   │   │   ├── StudyRecordDao.kt
│   │   │   ├── UserStatsDao.kt
│   │   │   └── UserSettingsDao.kt
│   │   └── AppDatabase.kt
│   └── repository/
│       ├── LevelRepository.kt
│       ├── WordRepository.kt
│       ├── StudyRepository.kt
│       └── SettingsRepository.kt
├── domain/
│   ├── model/
│   │   ├── ReviewResult.kt
│   │   └── StudyState.kt
│   └── usecase/
│       ├── GetLevelsWithProgressUseCase.kt
│       ├── GetWordsForStudyUseCase.kt
│       ├── RecordStudyResultUseCase.kt
│       └── UpdateStreakUseCase.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/
│   │   └── NavGraph.kt
│   ├── components/
│   │   ├── FlashCard.kt
│   │   ├── LevelCard.kt
│   │   ├── ProgressBar.kt
│   │   └── EvaluationButtons.kt
│   ├── onboarding/
│   │   ├── OnboardingScreen.kt
│   │   └── OnboardingViewModel.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── study/
│   │   ├── StudyScreen.kt
│   │   ├── StudyResultScreen.kt
│   │   └── StudyViewModel.kt
│   ├── word/
│   │   ├── WordListScreen.kt
│   │   ├── WordEditScreen.kt
│   │   └── WordViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt
│       ├── PremiumScreen.kt
│       └── SettingsViewModel.kt
├── billing/
│   ├── BillingRepository.kt
│   └── PremiumManager.kt
├── ads/
│   └── AdManager.kt
├── util/
│   ├── SrsCalculator.kt
│   └── DateUtils.kt
└── EnglishWordApp.kt
```

---

## 実装順序（推奨）

```
[Phase 2-A: 基盤]
  Step5 → 6-A (プロジェクト + データ層)
     ↓
[Phase 2-B: コア機能]
  6-B → 6-C → 6-D (オンボーディング → ホーム → 学習)
     ↓
[Phase 2-C: 管理機能]
  6-E → 6-F (単語管理 → 設定)
     ↓
[Phase 2-D: 収益化]
  7-A → 7-B → 7-C (課金 → 広告 → ドキュメント)
```

---

## 工数サマリー

| ステップ | 見積もり |
|----------|----------|
| Step5: プロジェクト雛形 | 1-2時間 |
| Step6: 機能実装 | 8-12時間 |
| Step7: 課金・広告 | 4-6時間 |
| **合計** | **13-20時間** |
