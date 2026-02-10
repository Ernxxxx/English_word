package com.example.englishword.ui.settings

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import com.example.englishword.billing.BillingRepository
import com.example.englishword.billing.PremiumManager
import com.example.englishword.ui.theme.PremiumGold
import com.example.englishword.ui.theme.PremiumOrange
import com.example.englishword.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumManager: PremiumManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    /** Exposes current network connectivity state. */
    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    init {
        observePremiumStatus()
        observePurchaseState()
        observeErrors()
        loadProducts()
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.isPremium.collect { isPremium ->
                _uiState.value = _uiState.value.copy(isPremium = isPremium)
            }
        }
    }

    private fun observePurchaseState() {
        viewModelScope.launch {
            premiumManager.purchaseState.collect { state ->
                when (state) {
                    is BillingRepository.PurchaseState.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is BillingRepository.PurchaseState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isPremium = true,
                            purchaseSuccess = true,
                            error = null
                        )
                    }
                    is BillingRepository.PurchaseState.Pending -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "購入処理中です。完了までお待ちください。"
                        )
                    }
                    is BillingRepository.PurchaseState.Cancelled -> {
                        _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                    }
                    is BillingRepository.PurchaseState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = getErrorMessage(state.code, state.message)
                        )
                    }
                    is BillingRepository.PurchaseState.Idle -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun observeErrors() {
        viewModelScope.launch {
            premiumManager.error.collectLatest { errorMessage ->
                _uiState.value = _uiState.value.copy(error = errorMessage)
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            premiumManager.queryProducts()
            val price = premiumManager.getPremiumPrice()
            if (price != "N/A") {
                _uiState.value = _uiState.value.copy(formattedPrice = price)
            }
        }
    }

    fun purchasePremium(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            premiumManager.purchasePremium(activity)
        }
    }

    fun restorePurchase() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val restored = premiumManager.restorePurchases()
            if (!restored) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "復元可能な購入が見つかりませんでした"
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        premiumManager.resetPurchaseState()
    }

    private fun getErrorMessage(code: Int, message: String): String {
        return when (code) {
            1 -> "購入がキャンセルされました"
            2 -> "サービスが利用できません"
            3 -> "課金サービスが利用できません"
            4 -> "この商品は購入できません"
            5 -> "開発者エラー"
            6 -> "エラーが発生しました"
            7 -> "すでにこの商品を所有しています"
            8 -> "この商品は所有されていません"
            else -> "購入に失敗しました: $message"
        }
    }
}

data class PremiumUiState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = false,
    val purchaseSuccess: Boolean = false,
    val error: String? = null,
    val formattedPrice: String = "月額 ¥300"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    var isPurchasing by remember { mutableStateOf(false) }

    // Reset isPurchasing when loading finishes (purchase completed, cancelled, or errored)
    LaunchedEffect(uiState.isLoading, uiState.error, uiState.purchaseSuccess) {
        if (!uiState.isLoading) {
            isPurchasing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("プレミアム") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium Badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PremiumGold,
                                PremiumOrange
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "プレミアム",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isPremium) {
                // Already Premium
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Premiumメンバーです",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Text(
                    text = "すべての機能を解放しよう",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Features
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    PremiumFeatureItem(
                        text = "広告なし",
                        description = "学習に集中できます"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumFeatureItem(
                        text = "6,000語以上収録",
                        description = "中学1年〜高校3年の英単語"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumFeatureItem(
                        text = "学習統計グラフ",
                        description = "週間・月間の進捗を可視化"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PremiumFeatureItem(
                        text = "復習リマインダー",
                        description = "最適なタイミングで通知"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!uiState.isPremium) {
                // Price
                Text(
                    text = uiState.formattedPrice,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Purchase Button
                Button(
                    onClick = {
                        if (!isPurchasing) {
                            if (!isConnected) {
                                Toast.makeText(
                                    context,
                                    "ネットワークに接続されていません。接続を確認してから再試行してください。",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@Button
                            }
                            isPurchasing = true
                            activity?.let { viewModel.purchasePremium(it) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .animateContentSize(),
                    enabled = !uiState.isLoading && !isPurchasing && activity != null
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "購入する",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Restore Button
                TextButton(
                    onClick = { viewModel.restorePurchase() },
                    enabled = !uiState.isLoading
                ) {
                    Text("購入を復元")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "いつでもキャンセル可能\nサブスクリプションはGoogle Playから管理できます",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Error message
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("閉じる")
                        }
                    }
                }
            }

            // Success message
            if (uiState.purchaseSuccess && uiState.isPremium) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "購入が完了しました！",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    text: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = PremiumGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
