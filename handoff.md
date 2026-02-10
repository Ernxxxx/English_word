# Handoff - English_word

更新: 2026-02-10 19:33:53

## 今回やったこと
- Phase 2 対応: リリース設定を強化
- `app/build.gradle.kts` で `RELEASE_APPLICATION_ID` を導入し、`applicationId` を `com.longs.englishword`（デフォルト）へ変更
- `app/build.gradle.kts` でリリース時の AdMob ID 必須化（`ADMOB_*_RELEASE` が空・テストIDならビルド失敗）
- `app/build.gradle.kts` でリリース時の `keystore.properties` 必須化
- `AndroidManifest.xml` の AdMob App ID を `${admobAppId}` プレースホルダ化
- `AdManager.kt` を `BuildConfig.ADMOB_*` 参照へ変更（デバッグ/リリース自動切替）
- `gradle.properties` に本番 AdMob ID を反映
  - `ADMOB_APP_ID_RELEASE=ca-app-pub-8894698859594740~4740287093`
  - `ADMOB_BANNER_AD_UNIT_ID_RELEASE=ca-app-pub-8894698859594740/3359030460`
  - `ADMOB_INTERSTITIAL_AD_UNIT_ID_RELEASE=ca-app-pub-8894698859594740/3167458771`
  - `ADMOB_REWARDED_AD_UNIT_ID_RELEASE=ca-app-pub-8894698859594740/4088814318`
- Phase 3 対応: セキュリティ強化
- `UnlockRepository.kt` に時刻改ざん耐性を追加（単調増加の trusted time を導入し、解除期限/日次上限判定に使用）
- `AndroidManifest.xml` で `android:allowBackup="false"` に変更
- `backup_rules.xml` / `data_extraction_rules.xml` で DB・SharedPreferences・files をバックアップ除外
- Phase 4 対応: UX改善
- `HomeScreen.kt` / `StudyScreen.kt` / `StudyResultScreen.kt` / `HomeViewModel.kt` / `StudyViewModel.kt` / `PremiumScreen.kt` の英語文言を日本語化
- `StatsViewModel.kt` に `error` 状態を追加し、`StatsScreen.kt` にエラー表示＋再試行UIを追加
- 二重タップ防止を追加
- `EvaluationButtons.kt` に 300ms デバウンスを追加
- `HomeScreen.kt` の削除確認/広告視聴ボタンに再タップ防止を追加
- `StudyResultScreen.kt` の操作ボタンに再タップ防止を追加
- Phase 5 対応: コード品質改善
- `LevelRepository.reorderLevels()` を `database.withTransaction` でトランザクション化
- `MAX_LEVEL` ハードコード統一
- `WordDao.kt` の mastered 判定クエリを `maxMasteryLevel` 引数化
- `WordRepository.kt` / `StatsViewModel.kt` / `StudyScreen.kt` で `SrsCalculator.MAX_LEVEL` を使用するよう統一

## 現在の状態
- ビルド: `./gradlew :app:assembleRelease -x test` 成功
- テスト: `./gradlew testDebugUnitTest` 成功
- 未コミットの変更: あり（今回の実装ファイル一式 + 未追跡 `nul`）

## 残りのタスク
- [ ] 変更をレビューしてコミット
- [ ] 必要なら `RELEASE_APPLICATION_ID` を最終値へ調整
- [ ] Play Console / AdMob 連携の最終確認

## 注意点
- リリースタスク実行時は AdMob 本番ID未設定だと意図的にビルド失敗する仕様
- リリースタスク実行時は `keystore.properties` が無いと意図的にビルド失敗する仕様
- `UnlockRepository` は trusted time を `user_settings` に保持するため、端末時刻を巻き戻しても解除期限/日次上限が後退しない
- ルートに `nul` の未追跡ファイルがあるため、後続作業時に誤操作へ注意

## 関連ファイル
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/example/englishword/ads/AdManager.kt`
- `app/src/main/java/com/example/englishword/data/repository/UnlockRepository.kt`
- `app/src/main/java/com/example/englishword/data/repository/LevelRepository.kt`
- `app/src/main/java/com/example/englishword/data/local/dao/WordDao.kt`
- `app/src/main/java/com/example/englishword/data/repository/WordRepository.kt`
- `app/src/main/java/com/example/englishword/ui/stats/StatsViewModel.kt`
- `app/src/main/java/com/example/englishword/ui/stats/StatsScreen.kt`
- `app/src/main/java/com/example/englishword/ui/components/EvaluationButtons.kt`
- `app/src/main/java/com/example/englishword/ui/home/HomeScreen.kt`
- `app/src/main/java/com/example/englishword/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/example/englishword/ui/study/StudyScreen.kt`
- `app/src/main/java/com/example/englishword/ui/study/StudyViewModel.kt`
- `app/src/main/java/com/example/englishword/ui/study/StudyResultScreen.kt`
- `app/src/main/java/com/example/englishword/ui/components/LevelCard.kt`
- `app/src/main/java/com/example/englishword/ui/components/StreakBadge.kt`
- `app/src/main/java/com/example/englishword/ui/settings/PremiumScreen.kt`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `gradle.properties`
- `handoff.md`
