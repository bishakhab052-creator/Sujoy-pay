package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserEntity
import com.example.data.repository.PendingOtpChallenge
import com.example.data.repository.RepoResult
import com.example.data.repository.SujoyPayRepository
import com.example.security.SessionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSignUpMode: Boolean = false,
    val mobileInput: String = "",
    val fullNameInput: String = "",
    val emailInput: String = "",
    val mpinInput: String = "",
    val otpInput: String = "",
    val pendingChallenge: PendingOtpChallenge? = null,
    val resendSecondsLeft: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val repository: SujoyPayRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = repository.sessionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SessionState()
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
        }
    }

    fun toggleAuthMode(isSignUp: Boolean) {
        _uiState.update {
            it.copy(
                isSignUpMode = isSignUp,
                pendingChallenge = null,
                otpInput = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onMobileChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(mobileInput = digits, errorMessage = null) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullNameInput = value.take(50), errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(emailInput = value.take(64), errorMessage = null) }
    }

    fun onMpinChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(mpinInput = digits, errorMessage = null) }
    }

    fun onOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpInput = digits, errorMessage = null) }
    }

    fun requestOtp() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (
                val res = repository.requestOtp(
                    mobileRaw = current.mobileInput,
                    fullName = current.fullNameInput,
                    email = current.emailInput,
                    isSignUp = current.isSignUpMode
                )
            ) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingChallenge = res.data,
                            otpInput = "",
                            infoMessage = "Sandbox OTP generated: ${res.data.sandboxOtpCode}"
                        )
                    }
                    startResendCountdown()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun verifyOtpAndProceed() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (
                val res = repository.verifyOtpAndLogin(
                    mobileRaw = current.mobileInput,
                    enteredOtp = current.otpInput,
                    mpinForNewUser = current.mpinInput
                )
            ) {
                is RepoResult.Success<UserEntity> -> {
                    timerJob?.cancel()
                    _uiState.value = AuthUiState()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun loginSandboxDemo(isAdmin: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.loginDemoAccount(isAdmin)) {
                is RepoResult.Success -> {
                    _uiState.value = AuthUiState()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun resetOtpStep() {
        timerJob?.cancel()
        _uiState.update { it.copy(pendingChallenge = null, otpInput = "", errorMessage = null, infoMessage = null) }
    }

    fun logout(userId: String?) {
        viewModelScope.launch {
            repository.logout(userId)
            _uiState.value = AuthUiState()
        }
    }

    fun setDarkMode(dark: Boolean?) {
        viewModelScope.launch {
            repository.setDarkMode(dark)
        }
    }

    private fun startResendCountdown() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (sec in 30 downTo 0) {
                _uiState.update { it.copy(resendSecondsLeft = sec) }
                if (sec > 0) delay(1000L)
            }
        }
    }
}
