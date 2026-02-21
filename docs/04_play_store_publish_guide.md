# Google Play ストア公開手順ガイド

## 事前準備チェックリスト

### 1. Google Play Developer アカウント

- [ ] Google Play Console アカウント作成（$25 一回払い）
- [ ] 本人確認完了
- [ ] 支払いプロファイル設定（課金収益受け取り用）

### 2. 必要なアセット準備

#### アプリアイコン

- [ ] 512 x 512 px PNG（32bit、アルファ付き可）
- [ ] 丸型マスク対応デザイン推奨

#### スクリーンショット（最低2枚、最大8枚）

- [ ] スマートフォン: 16:9 または 9:16
  - 最小: 320px、最大: 3840px
  - 推奨: 1080 x 1920 px
- [ ] 撮影する画面:
  1. ホーム画面（レベル一覧）
  2. 単語カード学習（表面）
  3. 単語カード学習（裏面）
  4. 学習結果画面
  5. 単語追加画面
  6. 設定画面
  7. Premium画面
  8. オンボーディング

#### フィーチャーグラフィック

- [ ] 1024 x 500 px PNG/JPG
- [ ] アプリ名とキャッチコピーを含むデザイン

---

## Step 1: 署名キーの作成

### キーストア作成（Android Studio）

```bash
# コマンドラインで作成
keytool -genkey -v -keystore english-word-release.keystore -alias english-word -keyalg RSA -keysize 2048 -validity 10000

# 入力情報
# - キーストアパスワード: [安全なパスワード]
# - 名前: [開発者名]
# - 組織単位: [部署名、個人なら空欄可]
# - 組織名: [会社名、個人なら空欄可]
# - 市区町村: [例: Tokyo]
# - 都道府県: [例: Tokyo]
# - 国コード: JP
```

### app/build.gradle.kts に署名設定追加

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../english-word-release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "english-word"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 重要: キーストアのバックアップ

- [ ] キーストアファイルを安全な場所にバックアップ
- [ ] パスワードを別の場所に保管
- [ ] **紛失するとアプリ更新不可能**

---

## Step 2: リリースビルド作成

### Android Studio でビルド

1. Build → Generate Signed Bundle / APK
2. Android App Bundle (AAB) を選択
3. キーストア情報を入力
4. release を選択
5. Finish

### コマンドラインでビルド

```bash
# Windows
gradlew bundleRelease

# Mac/Linux
./gradlew bundleRelease

# 出力先: app/build/outputs/bundle/release/app-release.aab
```

---

## Step 3: Google Play Console 設定

### 3.1 アプリの作成

1. Google Play Console にログイン
2. 「アプリを作成」をクリック
3. 入力情報:
   - アプリ名: `英単語帳 - レベル別学習（日本語訳付き）`
   - デフォルト言語: 日本語
   - アプリまたはゲーム: アプリ
   - 無料または有料: 無料（アプリ内課金あり）
   - デベロッパープログラムポリシーに同意

### 3.2 ダッシュボードのタスク完了

#### ストアの設定

- [ ] アプリのカテゴリ: 教育
- [ ] 連絡先の詳細: メールアドレス
- [ ] 外部マーケティング: オプション

#### メインのストア掲載情報

- [ ] アプリ名
- [ ] 簡単な説明（80文字）
- [ ] 詳しい説明（4000文字）
- [ ] アプリアイコン
- [ ] フィーチャーグラフィック
- [ ] スクリーンショット

#### アプリのコンテンツ

- [ ] プライバシーポリシー URL
- [ ] アプリのアクセス権（制限なし）
- [ ] 広告: はい、広告が含まれる
- [ ] コンテンツのレーティング（IARC質問票）
- [ ] ターゲットユーザー: 13歳以上
- [ ] ニュースアプリ: いいえ
- [ ] COVID-19関連: いいえ
- [ ] データセーフティ

---

## Step 4: 定期購入の設定

### Play Console → 収益化 → 定期購入

1. 「定期購入を作成」をクリック
2. 設定内容:
   - 商品ID: `premium_monthly`
   - 名前: `Premium（月額）`
   - 説明: `広告なし、無制限の単語登録`
   - 請求対象期間: 1か月
   - 基本プランを追加:
     - 価格: ¥300 JPY
     - 更新タイプ: 自動更新
   - 猶予期間: 3日間
   - アカウントの保留: 30日間

### テスト用ライセンス

1. 設定 → ライセンステスト
2. テスターのGmailアドレスを追加
3. ライセンス応答: `RESPOND_NORMALLY`

---

## Step 5: 内部テストトラック

### 5.1 テスターの設定

1. テスト → 内部テスト
2. テスターを管理 → メーリングリストを作成
3. テスターのメールアドレスを追加

### 5.2 リリースの作成

1. 「新しいリリースを作成」
2. App Bundle をアップロード
3. リリース名: `1.0.0 (1)`
4. リリースノートを入力
5. 「リリースを確認」→「内部テストへの公開を開始」

### 5.3 テスト実施

- [ ] インストール確認
- [ ] 全画面遷移確認
- [ ] 学習フロー確認
- [ ] 課金フロー確認（テストカード使用）
- [ ] 広告表示確認
- [ ] クラッシュなし確認

---

## Step 6: クローズドテスト（オプション）

### より広範囲なテスト

1. テスト → クローズドテスト
2. トラックを作成（例: アルファ版）
3. テスターを100-1000人に拡大
4. フィードバック収集

---

## Step 7: 製品版リリース

### 7.1 審査提出

1. 製品版 → 「新しいリリースを作成」
2. 内部テストからプロモート or 新規アップロード
3. リリースノート入力
4. 「審査に送信」

### 7.2 審査期間

- 新規アプリ: 通常3-7日
- 更新: 通常1-3日
- リジェクト時は理由を確認して修正

### 7.3 公開完了

- 審査通過後、自動または手動で公開
- Google Play で検索可能に

---

## Step 8: AdMob 本番設定

### 8.1 AdMob アカウント設定

1. AdMob にログイン
2. アプリを追加（パッケージ名で検索）
3. Play ストアにリンク

### 8.2 広告ユニット作成

1. バナー広告ユニット作成
2. インタースティシャル広告ユニット作成
3. 広告ユニットIDをメモ

### 8.3 本番広告IDに変更

```kotlin
// ads/AdManager.kt のIDを本番用に変更
object AdUnitIds {
    const val BANNER = "ca-app-pub-XXXX/YYYY"        // 本番ID
    const val INTERSTITIAL = "ca-app-pub-XXXX/ZZZZ" // 本番ID
}
```

### 8.4 app-ads.txt

1. ウェブサイトに `app-ads.txt` を設置
2. AdMob から取得した内容を記載
3. URL を Play Console に登録

---

## リリース後チェックリスト

### 即日確認

- [ ] Google Play でアプリが見つかる
- [ ] インストールできる
- [ ] 課金できる（本番テスト）
- [ ] 広告が表示される

### 1週間以内

- [ ] クラッシュレポート確認（Play Console → 品質 → Android Vitals）
- [ ] レビュー確認・返信
- [ ] ダウンロード数確認
- [ ] 収益確認

---

## トラブルシューティング

### 審査リジェクト対応

| 理由                     | 対応           |
| ------------------------ | -------------- |
| プライバシーポリシーなし | URLを追加      |
| 広告の不適切な配置       | 配置を修正     |
| クラッシュ               | 修正してビルド |
| メタデータ違反           | 説明文を修正   |

### 課金エラー

| エラー              | 対応               |
| ------------------- | ------------------ |
| BILLING_UNAVAILABLE | 端末のPlay設定確認 |
| ITEM_ALREADY_OWNED  | 購入復元処理確認   |
| DEVELOPER_ERROR     | 商品ID確認         |

---

## 次のステップ

Step 9 では以下を設定:

- KPI設定・ダッシュボード
- ASO最適化
- 継続的改善サイクル
