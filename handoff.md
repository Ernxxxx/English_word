# Handoff - English_word

更新: 2026-02-25 13:21

## 今回やったこと
- 設定画面の利用規約/プライバシーポリシーURLを本番値へ変更
  - `app/src/main/java/com/example/englishword/ui/settings/SettingsScreen.kt`
  - `https://ernxxxx.github.io/English_word/terms_of_service.html`
  - `https://ernxxxx.github.io/English_word/privacy_policy.html`
- 設定画面のサポートメールを変更
  - `support@example.com` -> `longsandao@gmail.com`
  - `app/src/main/java/com/example/englishword/ui/settings/SettingsScreen.kt`
- ポリシー文書の連絡先プレースホルダーを実値へ更新
  - `docs/privacy_policy.html`
  - `docs/terms_of_service.html`
  - `docs/privacy_policy.md`
  - `docs/terms_of_service.md`

## 現在の状態
- ビルド: `./gradlew :app:compileDebugKotlin --no-daemon --no-configuration-cache` 成功
- テスト: 未実行
- 未コミット変更: あり（上記5ファイル + `handoff.md`）

## 残りのタスク
- [ ] GitHub Pages を有効化して `privacy_policy.html` / `terms_of_service.html` を公開
- [ ] 公開後に設定画面から実際にリンク遷移できることを実機確認
- [ ] Play Console のプライバシーポリシーURL設定を公開URLで更新

## 注意点
- 現在設定したURLは `https://ernxxxx.github.io/English_word/...` を想定しているが、現時点では 404（未公開）
- `SettingsScreen.kt` は同等の設定項目が2か所（`SettingsTab` と `SettingsScreen`）あるため、両方更新済み

## 関連ファイル
- `app/src/main/java/com/example/englishword/ui/settings/SettingsScreen.kt`
- `docs/privacy_policy.html`
- `docs/terms_of_service.html`
- `docs/privacy_policy.md`
- `docs/terms_of_service.md`
- `handoff.md`
