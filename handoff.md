# Handoff - English_word

更新: 2026-02-13 17:15

## 今回やったこと

### Phase 1: MainShellScreen + BottomNavigation
- `MainShellScreen.kt` 新規作成: 3タブ（ホーム/統計/設定）のボトムナビゲーション
- `NavGraph.kt`: HOME ルートを MainShellScreen に差し替え、STATS/SETTINGS スタンドアロン削除
- `HomeScreen.kt`: HomeTab composable 追加（Scaffold なし、コンテンツのみ）
- `StatsScreen.kt`: StatsTab composable 追加
- `SettingsScreen.kt`: SettingsTab composable 追加
- Banner 広告を MainShellScreen のボトムタブ上に統合

### Phase 2: ChildLevelCard 2行レイアウト
- 1行 → 2行に変更（情報行 + アクションボタン行）
- LinearProgressIndicator 追加
- アイコン付きボタン（単語一覧/テスト）

### 学習画面改善
- StudyScreen: モード選択カード UI（復習/新規/まとめて）
- 復習モードのバグ修正
- BottomSheet の半展開修正

### 単語テスト機能
- `UnitTestScreen.kt` / `UnitTestViewModel.kt` 新規作成
- 解放条件: 全単語を1回以上学習済み（reviewCount >= 1）
- 正解時に `markWordAcquired` + `masteryLevel = MAX_LEVEL` に更新
- 問数選択 UI、フィードバック表示

### 統計画面改善
- 熟練度分布を6段階 → 3カテゴリに簡素化（未学習/学習中/習得済み）
- MasterySummaryBar 削除（ドーナツチャートと重複）
- weight(0f) クラッシュ修正（`.coerceAtLeast(0.001f)`）
- 取得単語の表示を `isAcquired` ベースに変更

### 親レベル進捗修正
- `LevelWithProgress.kt`: `totalInProgressCount` 追加
- `progressFraction` / `progressPercent` に学習中の単語も含めるよう修正

### DB 更新
- `AppDatabase` version 8 → 9（`isAcquired` 列 + index）
- `WordDao.markWordAcquired`: `masteryLevel` も同時更新するよう変更

### UI文字列トークン化
- HomeScreen, StudyScreen, LevelCard, FlashCard, EvaluationButtons, StreakBadge, StatsScreen 等のハードコード文字列を整理

## 現在の状態
- ビルド: `./gradlew :app:installDebug --no-daemon` 成功済み
- 実機起動: `com.longs.englishword.debug` で動作確認済み
- 未コミット: 26ファイル変更 + 4ファイル新規（下記参照）
- テスト: 未実行

## 未解決の問題

### markWordAcquired が効いていない可能性
- ユーザー報告: 「6問正解したけど習得にならない」
- WordDao に修正済み（`masteryLevel = CASE WHEN ... THEN :maxMasteryLevel`）
- WordRepository にデバッグログ追加済み
- **要確認**: `adb logcat | grep WordRepo` でログを確認し、実際に更新されているか検証
- デバッグログ（`Log.d`）は確認後に削除すること

## 残りのタスク
- [ ] markWordAcquired のデバッグログ確認 → 原因特定
- [ ] デバッグログ削除（WordRepository.kt の2箇所）
- [ ] 実機で全体動作確認
- [ ] コミット（26変更 + 4新規ファイル）

## 注意点
- パッケージ名: デバッグビルドは `com.longs.englishword.debug`（`applicationIdSuffix = ".debug"`）
- bash パス: `cd C:\...` は使えない、`cd /c/...` を使う
- ルートに `nul` ファイルあり（Windows の NUL デバイス名の誤作成）、コミットに含めない
- 「習得（SRS Lv5）」と「取得（isAcquired）」は別指標だったが、テスト正解時に両方更新するよう変更済み

## 関連ファイル

### 新規
- `app/src/main/java/com/example/englishword/ui/navigation/MainShellScreen.kt`
- `app/src/main/java/com/example/englishword/ui/test/UnitTestScreen.kt`
- `app/src/main/java/com/example/englishword/ui/test/UnitTestViewModel.kt`
- `app/src/main/java/com/example/englishword/ui/components/CommonStates.kt`
- `app/src/main/java/com/example/englishword/ui/theme/Dimens.kt`

### 主要変更
- `app/src/main/java/com/example/englishword/ui/home/HomeScreen.kt`（+398行の大幅改修）
- `app/src/main/java/com/example/englishword/ui/study/StudyScreen.kt`（モード選択 UI）
- `app/src/main/java/com/example/englishword/ui/stats/StatsCharts.kt`（3カテゴリ化）
- `app/src/main/java/com/example/englishword/ui/stats/StatsViewModel.kt`（分布集約ロジック）
- `app/src/main/java/com/example/englishword/data/local/dao/WordDao.kt`（markWordAcquired 修正）
- `app/src/main/java/com/example/englishword/data/repository/WordRepository.kt`（デバッグログ）
- `app/src/main/java/com/example/englishword/domain/model/LevelWithProgress.kt`（親進捗修正）
- `app/src/main/java/com/example/englishword/ui/navigation/NavGraph.kt`（ボトムナビ対応）

### 削除
- `app/src/main/java/com/example/englishword/ui/components/AdBanner.kt`（MainShellScreen に統合）
