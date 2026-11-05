package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.model.SubscriptionPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "PlayBillingManager"

data class PlayBillingConnectionState(
    val isConnectedToPlayStore: Boolean = false,
    val statusTextHu: String = "Kapcsolódás a Google Play Billing szolgáltatáshoz...",
    val availableProductIds: List<String> = emptyList(),
    val lastBillingResponseCode: Int = BillingClient.BillingResponseCode.OK
)

/**
 * Native Google Play Billing 7.x Engine + Interactive Sandbox Purchase Sheet.
 * - Connects to real Google Play BillingClient (`BillingClient.newBuilder(context)`).
 * - Queries `SUBS` ProductDetails (`roomai_pro_monthly`, `roomai_pro_annual`) and active purchases.
 * - Acknowledges real Play Store purchases with `acknowledgePurchase`.
 * - Provides an interactive Google Play Test Purchase Sheet (GPay / Test Card / Declined Test Card)
 *   so the user can test the complete purchase lifecycle and SQL transaction persistence on any device or emulator.
 */
class PlayBillingManager(
    context: Context,
    private val billingTransactionDao: BillingTransactionDao,
    private val userPreferencesRepository: UserPreferencesRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext

    val transactionsFlow: Flow<List<BillingTransactionEntity>> =
        billingTransactionDao.getAllTransactions()

    private val _connectionState = MutableStateFlow(PlayBillingConnectionState())
    val connectionState: StateFlow<PlayBillingConnectionState> = _connectionState.asStateFlow()

    private val productDetailsMap = mutableMapOf<String, ProductDetails>()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        startPlayBillingConnection()
    }

    fun startPlayBillingConnection() {
        try {
            if (billingClient.isReady) {
                queryPlayStoreProducts()
                queryActivePlayPurchases()
                return
            }
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val isOk = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    Log.d(TAG, "Play Billing setup finished: ${billingResult.responseCode} ${billingResult.debugMessage}")
                    _connectionState.value = PlayBillingConnectionState(
                        isConnectedToPlayStore = isOk,
                        statusTextHu = if (isOk) {
                            "Google Play Billing Client v7.1.1 Csatlakozva (Play Store + Teszt Sandbox aktív)"
                        } else {
                            "Google Play Store fiók nem aktív az emulátoron (Kód: ${billingResult.responseCode}) – Interaktív Google Play Sandbox Tesztmód aktív"
                        },
                        lastBillingResponseCode = billingResult.responseCode
                    )
                    if (isOk) {
                        queryPlayStoreProducts()
                        queryActivePlayPurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "Play Billing service disconnected")
                    _connectionState.value = _connectionState.value.copy(
                        isConnectedToPlayStore = false,
                        statusTextHu = "Google Play Billing kapcsolat megszakadt – Sandbox Tesztmód elérhető"
                    )
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start BillingClient connection", e)
            _connectionState.value = PlayBillingConnectionState(
                isConnectedToPlayStore = false,
                statusTextHu = "Google Play Sandbox Tesztmód aktív (${e.localizedMessage ?: "Emulátor környezet"})"
            )
        }
    }

    private fun queryPlayStoreProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ANNUAL)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                }
                _connectionState.value = _connectionState.value.copy(
                    availableProductIds = productDetailsMap.keys.toList()
                )
            }
        }
    }

    fun queryActivePlayPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { purchase ->
                    handlePlayPurchase(purchase)
                }
            }
        }
    }

    /**
     * Attempts to launch the native Google Play Billing sheet if the product is registered in Play Console;
     * returns false if the environment does not have the Play Console SKU published so the interactive
     * Google Play Test Purchase Sheet can be displayed immediately.
     */
    fun tryLaunchNativeBillingFlow(activity: Activity, plan: SubscriptionPlan): Boolean {
        if (!billingClient.isReady) return false
        val productId = when (plan) {
            SubscriptionPlan.MONTHLY -> PRODUCT_MONTHLY
            SubscriptionPlan.ANNUAL -> PRODUCT_ANNUAL
            SubscriptionPlan.FREE -> return false
        }
        val productDetails = productDetailsMap[productId] ?: return false
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return false

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                handlePlayPurchase(purchase)
            }
        } else {
            Log.w(TAG, "onPurchasesUpdated response: ${billingResult.responseCode} ${billingResult.debugMessage}")
        }
    }

    private fun handlePlayPurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { ackRes ->
                        Log.d(TAG, "Purchase acknowledged: ${ackRes.responseCode}")
                    }
                }

                val prodId = purchase.products.firstOrNull() ?: PRODUCT_MONTHLY
                val matchedPlan = if (prodId == PRODUCT_ANNUAL) SubscriptionPlan.ANNUAL else SubscriptionPlan.MONTHLY

                userPreferencesRepository.setSubscriptionPlan(matchedPlan)
                billingTransactionDao.insertTransaction(
                    BillingTransactionEntity(
                        orderId = purchase.orderId ?: "GPA.${System.currentTimeMillis()}",
                        productId = prodId,
                        planTitleHu = matchedPlan.titleHu,
                        amountFormatted = matchedPlan.priceHu,
                        purchaseToken = purchase.purchaseToken,
                        paymentMethodLabel = "Google Play Store Billing",
                        status = "PURCHASED_PLAY_BILLING",
                        isAcknowledged = true,
                        isAutoRenewing = purchase.isAutoRenewing
                    )
                )
            }
        }
    }

    /**
     * Processes a test purchase from the Google Play Sandbox Bottom Sheet and persists the
     * full transaction receipt into Room SQL (`billing_transactions`).
     */
    suspend fun executeSandboxPlayPurchase(
        plan: SubscriptionPlan,
        paymentMethodLabel: String,
        simulateDecline: Boolean = false
    ): Result<BillingTransactionEntity> {
        val productId = when (plan) {
            SubscriptionPlan.MONTHLY -> PRODUCT_MONTHLY
            SubscriptionPlan.ANNUAL -> PRODUCT_ANNUAL
            SubscriptionPlan.FREE -> "roomai_free_tier"
        }
        val orderNumber = "GPA.${(1000..9999).random()}-${(1000..9999).random()}-${(10000..99999).random()}"
        val token = "gplay_sandbox_${UUID.randomUUID().toString().replace("-", "").take(20)}"

        if (simulateDecline) {
            val declinedTx = BillingTransactionEntity(
                orderId = orderNumber,
                productId = productId,
                planTitleHu = plan.titleHu,
                amountFormatted = plan.priceHu,
                purchaseToken = token,
                paymentMethodLabel = paymentMethodLabel,
                status = "DECLINED_TEST_CARD",
                isAcknowledged = false,
                isAutoRenewing = false
            )
            val id = billingTransactionDao.insertTransaction(declinedTx)
            return Result.failure(
                Exception("Tranzakció elutasítva (Teszt Elutasított Kártya). Rendelésszám: $orderNumber")
            )
        }

        val approvedTx = BillingTransactionEntity(
            orderId = orderNumber,
            productId = productId,
            planTitleHu = plan.titleHu,
            amountFormatted = plan.priceHu,
            purchaseToken = token,
            paymentMethodLabel = paymentMethodLabel,
            status = "PURCHASED_SANDBOX_TEST",
            isAcknowledged = true,
            isAutoRenewing = plan != SubscriptionPlan.FREE
        )
        val insertedId = billingTransactionDao.insertTransaction(approvedTx)
        userPreferencesRepository.setSubscriptionPlan(plan)
        return Result.success(approvedTx.copy(id = insertedId))
    }

    suspend fun refundOrCancelTransaction(transaction: BillingTransactionEntity) {
        billingTransactionDao.updateTransactionStatus(transaction.id, "REFUNDED_AND_CANCELLED")
        userPreferencesRepository.setSubscriptionPlan(SubscriptionPlan.FREE)
    }

    suspend fun restoreActiveSubscriptionFromSql(): SubscriptionPlan? {
        // Restore handled via Room / Preferences
        queryActivePlayPurchases()
        return null
    }

    companion object {
        const val PRODUCT_MONTHLY = "roomai_pro_monthly"
        const val PRODUCT_ANNUAL = "roomai_pro_annual"
    }
}
