package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ComplaintEntity
import com.example.data.local.PaymentSettingsEntity
import com.example.data.local.TransactionEntity
import com.example.data.local.UserEntity
import com.example.data.repository.InitiatedAddMoneyOrder
import com.example.data.repository.RepoResult
import com.example.data.repository.SujoyPayRepository
import com.example.util.Formatters
import com.example.util.UpiAppChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WalletUiState(
    val isBusy: Boolean = false,
    val activeGatewayOrder: InitiatedAddMoneyOrder? = null,
    val gatewayLaunchNote: String? = null,
    val latestCompletedTxn: TransactionEntity? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val scannedUpiHandle: String = "",
    val scannedPayeeName: String = "",
    val scannedAmountRupees: String = "",
    val scannedNote: String = ""
)

class WalletViewModel(
    private val repository: SujoyPayRepository,
    private val currentUserId: String
) : ViewModel() {

    val user: StateFlow<UserEntity?> = repository.observeUser(currentUserId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    val transactions: StateFlow<List<TransactionEntity>> = repository.observeUserTransactions(currentUserId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val complaints: StateFlow<List<ComplaintEntity>> = repository.observeUserComplaints(currentUserId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    val settings: StateFlow<PaymentSettingsEntity?> = repository.paymentSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PaymentSettingsEntity()
    )

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    fun toggleBalanceVisibility(currentlyHidden: Boolean) {
        viewModelScope.launch {
            repository.setHideBalance(!currentlyHidden)
        }
    }

    fun clearBanners() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun dismissCompletedReceipt() {
        _uiState.update { it.copy(latestCompletedTxn = null) }
    }

    fun setScannedUpiTarget(handle: String, name: String, amount: String, note: String) {
        _uiState.update {
            it.copy(
                scannedUpiHandle = handle,
                scannedPayeeName = name,
                scannedAmountRupees = amount,
                scannedNote = note
            )
        }
    }

    fun clearScannedUpiTarget() {
        _uiState.update {
            it.copy(scannedUpiHandle = "", scannedPayeeName = "", scannedAmountRupees = "", scannedNote = "")
        }
    }

    // --- Add Money (2-phase gateway order + verified callback) ---

    fun initiateAddMoney(
        amountText: String,
        channel: UpiAppChannel,
        onOrderReadyForUpiLaunch: (InitiatedAddMoneyOrder) -> Unit
    ) {
        val paise = Formatters.parseRupeesToPaise(amountText)
        if (paise == null || paise < 10_00L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount (minimum ₹10.00).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null, successMessage = null) }
            when (val res = repository.initiateAddMoneyOrder(currentUserId, paise, channel)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            activeGatewayOrder = res.data,
                            gatewayLaunchNote = null
                        )
                    }
                    onOrderReadyForUpiLaunch(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun setGatewayLaunchNote(note: String) {
        _uiState.update { it.copy(gatewayLaunchNote = note) }
    }

    fun completeAddMoneyVerification(orderId: String, authorized: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (val res = repository.verifyAndSettleAddMoney(orderId, authorized)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            activeGatewayOrder = null,
                            gatewayLaunchNote = null,
                            latestCompletedTxn = res.data,
                            successMessage = "Verified payment confirmed! Wallet credited ${Formatters.formatInr(res.data.amountPaise)}."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            activeGatewayOrder = null,
                            gatewayLaunchNote = null,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun dismissActiveGatewayOrder() {
        _uiState.update { it.copy(activeGatewayOrder = null, gatewayLaunchNote = null) }
    }

    // --- Send Money ---

    fun sendMoney(
        recipientHandle: String,
        recipientName: String,
        amountText: String,
        note: String,
        mpin: String,
        onSuccess: () -> Unit = {}
    ) {
        val paise = Formatters.parseRupeesToPaise(amountText)
        if (paise == null || paise < 100L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount (minimum ₹1.00).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (val res = repository.sendMoney(currentUserId, recipientHandle, recipientName, paise, note, mpin)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            latestCompletedTxn = res.data,
                            successMessage = "Sent ${Formatters.formatInr(paise)} successfully!"
                        )
                    }
                    clearScannedUpiTarget()
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    // --- Withdraw Money ---

    fun withdrawMoney(
        accountNumber: String,
        ifsc: String,
        accountHolder: String,
        amountText: String,
        mpin: String
    ) {
        val paise = Formatters.parseRupeesToPaise(amountText)
        if (paise == null || paise < 100_00L) {
            _uiState.update { it.copy(errorMessage = "Minimum bank withdrawal is ₹100.00.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (val res = repository.withdrawMoney(currentUserId, accountNumber, ifsc, accountHolder, paise, mpin)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            latestCompletedTxn = res.data,
                            successMessage = "IMPS withdrawal of ${Formatters.formatInr(paise)} initiated!"
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    // --- Recharge & Bills ---

    fun payBillOrRecharge(
        type: String,
        providerName: String,
        consumerId: String,
        amountText: String,
        planNote: String,
        mpin: String
    ) {
        val paise = Formatters.parseRupeesToPaise(amountText)
        if (paise == null || paise < 10_00L) {
            _uiState.update { it.copy(errorMessage = "Enter a valid bill/recharge amount (min ₹10.00).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (
                val res = repository.payRechargeOrBill(
                    userId = currentUserId,
                    type = type,
                    providerName = providerName,
                    consumerOrMobileId = consumerId,
                    amountPaise = paise,
                    planOrNote = planNote,
                    mpin = mpin
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            latestCompletedTxn = res.data,
                            successMessage = "$providerName payment of ${Formatters.formatInr(paise)} successful!"
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    // --- Profile & Complaints ---

    fun updateBankDetails(bankName: String, account: String, ifsc: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (val res = repository.updateBankInfo(currentUserId, bankName, account, ifsc)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, successMessage = "Bank payout details encrypted & saved.") }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun updateMpin(currentPin: String, newPin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (val res = repository.updateMpin(currentUserId, currentPin, newPin)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, successMessage = "Security MPIN updated successfully.") }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun submitDisputeComplaint(
        transaction: TransactionEntity,
        category: String,
        description: String,
        requestRefund: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            when (
                val res = repository.submitComplaint(
                    userId = currentUserId,
                    transaction = transaction,
                    category = category,
                    description = description,
                    requestRefund = requestRefund
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            successMessage = "Dispute ticket ${res.data.id} submitted to SUJOY PAY Compliance."
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBusy = false, errorMessage = res.message) }
                }
            }
        }
    }
}
