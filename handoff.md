# Handoff - English_word

更新: 2026-02-13 14:10:00

## 今回やったこと
- 残タスクの実機検証を実施（`Pixel_8a_API_34` エミュレータ + `adb`）
- 学習開始時のモード選択ダイアログ表示を確認
  - `おまかせ（復習優先）`
  - `復習のみ`
  - `新規のみ`
- モード別出題の確認を実施
  - `復習のみ`: 期限到来語のみ出題されることを確認（`新規` が混ざらない）
  - `新規のみ`: 新規語ラベルで出題されることを確認
  - `おまかせ`: 先に復習語が出題され、その後に新規語へ切り替わることを確認
- 20問完了後の結果画面で `もう一度学習` を押し、無限ループせずモード選択へ戻ることを確認
- 検証のためにエミュレータ内 DB を一時調整（期限到来語/新規語の混在状態を作成）し、未完了セッションを削除して再現条件を固定

## 現在の状態
- ビルド: 今回は未実行（前回 handoff で `:app:assembleDebug` 成功）
- テスト: 今回は未実行（前回 handoff で `:app:testDebugUnitTest` 成功）
- 実機/エミュレータ確認:
  - `./gradlew :app:installDebug --no-daemon` 実行（終了コード 0）
  - `com.longs.englishword.debug` 起動・画面遷移・モード別出題を確認
- 未コミットの変更: あり（`WordDao.kt`, `WordRepository.kt`, `NavGraph.kt`, `StudyResultScreen.kt`, `StudyScreen.kt`, `StudyViewModel.kt`, `handoff.md`）+ 未追跡 `nul`

## 残りのタスク
- [ ] なし（本件の残タスクは完了）

## 注意点
- `Study` / `StudyResult` は HOME スコープの `StudyViewModel` を共有
- `adb` が PATH にない環境では `C:\Users\longs\AppData\Local\Android\Sdk\platform-tools\adb.exe` を明示指定
- ルートに未追跡ファイル `nul` があるため、コミット時に誤って追加しない
- 検証時にエミュレータ内 DB 状態を調整しているため、同条件を再現する場合は同様にデータ状態を揃える

## 関連ファイル
- `app/src/main/java/com/example/englishword/ui/study/StudyScreen.kt`
- `app/src/main/java/com/example/englishword/ui/study/StudyViewModel.kt`
- `app/src/main/java/com/example/englishword/data/repository/WordRepository.kt`
- `app/src/main/java/com/example/englishword/data/local/dao/WordDao.kt`
- `app/src/main/java/com/example/englishword/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/example/englishword/ui/study/StudyResultScreen.kt`
- `handoff.md`
