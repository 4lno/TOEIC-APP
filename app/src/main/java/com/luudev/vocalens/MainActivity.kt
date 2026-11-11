package com.luudev.vocalens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModelProvider
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import com.luudev.vocalens.presentation.welcome.WelcomeScreen
import com.luudev.vocalens.presentation.welcome.WelcomeViewModel
import com.luudev.vocalens.presentation.login.LoginScreen
import com.luudev.vocalens.presentation.login.LoginViewModel
import com.luudev.vocalens.presentation.register.RegisterScreen
import com.luudev.vocalens.presentation.register.RegisterViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.luudev.vocalens.data.repository.DemoDashboardRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetDashboard
import com.luudev.vocalens.presentation.navigation.MainNavigation
import com.luudev.vocalens.presentation.home.HomeViewModel
import androidx.lifecycle.createSavedStateHandle
import com.luudev.vocalens.data.repository.LocalVocabularyRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetSavedVocabulary
import com.luudev.vocalens.domain.usecase.DeleteVocabularyWord
import com.luudev.vocalens.presentation.vocabulary.VocabularyViewModel
import com.luudev.vocalens.data.repository.DemoPracticeRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetPracticeLessons
import com.luudev.vocalens.presentation.practice.PracticeViewModel
import androidx.compose.runtime.LaunchedEffect
import com.luudev.vocalens.data.repository.LocalProfileRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetProfile
import com.luudev.vocalens.domain.usecase.SaveProfile
import com.luudev.vocalens.presentation.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.luudev.vocalens.data.repository.FirebaseAuthRepositoryImpl
import com.luudev.vocalens.domain.usecase.GetCurrentUserUseCase
import com.luudev.vocalens.domain.usecase.LoginUseCase
import com.luudev.vocalens.domain.usecase.LogoutUseCase
import com.luudev.vocalens.domain.usecase.RegisterUseCase
import com.luudev.vocalens.domain.usecase.SendPasswordResetUseCase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        val viewModel = ViewModelProvider(this)[WelcomeViewModel::class.java]
        val authRepository = FirebaseAuthRepositoryImpl(FirebaseAuth.getInstance())
        val loginViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                LoginViewModel(createSavedStateHandle(), LoginUseCase(authRepository),
                    SendPasswordResetUseCase(authRepository), LogoutUseCase(authRepository), GetCurrentUserUseCase(authRepository))
            }
        })[LoginViewModel::class.java]
        val registerViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { RegisterViewModel(createSavedStateHandle(), RegisterUseCase(authRepository)) }
        })[RegisterViewModel::class.java]
        val homeViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { HomeViewModel(GetDashboard(DemoDashboardRepositoryImpl())) }
        })[HomeViewModel::class.java]
        val vocabularyViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                val repository = LocalVocabularyRepositoryImpl(getSharedPreferences("vocabulary", MODE_PRIVATE))
                VocabularyViewModel(createSavedStateHandle(), GetSavedVocabulary(repository), DeleteVocabularyWord(repository))
            }
        })[VocabularyViewModel::class.java]
        val practiceViewModel = ViewModelProvider(this, viewModelFactory {
            initializer { PracticeViewModel(createSavedStateHandle(), GetPracticeLessons(DemoPracticeRepositoryImpl())) }
        })[PracticeViewModel::class.java]
        val profileViewModel = ViewModelProvider(this, viewModelFactory {
            initializer {
                val repository = LocalProfileRepositoryImpl(getSharedPreferences("profile", MODE_PRIVATE))
                ProfileViewModel(GetProfile(repository), SaveProfile(repository))
            }
        })[ProfileViewModel::class.java]
        setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                LaunchedEffect(registerViewModel.uiState.registered) {
                    if (registerViewModel.uiState.registered) {
                        loginViewModel.onRegistrationSucceeded()
                        registerViewModel.clearPasswords()
                    }
                }
                LaunchedEffect(profileViewModel.uiState.profile) {
                    homeViewModel.onProfileUpdated(profileViewModel.uiState.profile)
                }
                BackHandler(enabled = viewModel.uiState.showLogin && !loginViewModel.uiState.showHome) {
                    if (loginViewModel.uiState.isLoading || registerViewModel.uiState.isLoading) {
                        // Keep the request on screen until its result is handled.
                    } else if (loginViewModel.uiState.showRegistration) {
                        registerViewModel.clearPasswords()
                        loginViewModel.onBackFromRegistration()
                    } else {
                        viewModel.onBackToWelcome()
                    }
                }
                if (loginViewModel.uiState.showHome) {
                    MainNavigation(
                        homeUiState = homeViewModel.uiState,
                        onHomeAction = homeViewModel::onAction,
                        onDismissHomeAction = homeViewModel::onDismissAction,
                        vocabularyViewModel = vocabularyViewModel,
                        practiceViewModel = practiceViewModel,
                        profileViewModel = profileViewModel,
                        onLogoutConfirmed = {
                            profileViewModel.onDismissDialog()
                            homeViewModel.onDismissAction()
                            registerViewModel.clearPasswords()
                            loginViewModel.onLogout()
                            viewModel.onStartClick()
                        }
                    )
                } else if (viewModel.uiState.showLogin && loginViewModel.uiState.showRegistration) {
                    RegisterScreen(
                        uiState = registerViewModel.uiState,
                        onNameChange = registerViewModel::onNameChange,
                        onEmailChange = registerViewModel::onEmailChange,
                        onPasswordChange = registerViewModel::onPasswordChange,
                        onConfirmPasswordChange = registerViewModel::onConfirmPasswordChange,
                        onTogglePassword = registerViewModel::onTogglePassword,
                        onCreateAccountClick = registerViewModel::onCreateAccountClick,
                        onLoginClick = {
                            registerViewModel.clearPasswords()
                            loginViewModel.onBackFromRegistration()
                        },
                        onNoticeDismissed = registerViewModel::onNoticeDismissed
                    )
                } else if (viewModel.uiState.showLogin) {
                    LoginScreen(
                        uiState = loginViewModel.uiState,
                        onEmailChange = loginViewModel::onEmailChange,
                        onPasswordChange = loginViewModel::onPasswordChange,
                        onTogglePassword = loginViewModel::onTogglePassword,
                        onLoginClick = loginViewModel::onLoginClick,
                        onForgotPasswordClick = loginViewModel::onForgotPasswordClick,
                        onRegisterClick = loginViewModel::onRegisterClick,
                        onNoticeDismissed = loginViewModel::onNoticeDismissed
                    )
                } else {
                    WelcomeScreen(onStartClick = viewModel::onStartClick)
                }
            }
        }
    }
}
