package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.ChatViewModel
import com.example.ui.components.NotificationBannerPopup
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.CreateGroupScreen
import com.example.ui.screens.CreateStoryScreen
import com.example.ui.screens.GroupDetailsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.SearchAndContactsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.screens.VoiceCallScreen
import com.example.ui.theme.LChatTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LChatTheme {
                var authUser by remember { mutableStateOf(Firebase.auth.currentUser) }

                DisposableEffect(Unit) {
                    val authListener = FirebaseAuth.AuthStateListener { auth ->
                        authUser = auth.currentUser
                    }
                    Firebase.auth.addAuthStateListener(authListener)
                    onDispose {
                        Firebase.auth.removeAuthStateListener(authListener)
                    }
                }

                if (authUser == null) {
                    SignInScreen(
                        onSignInSuccess = {
                            authUser = Firebase.auth.currentUser
                        }
                    )
                } else {
                    val chatViewModel: ChatViewModel = viewModel()
                    LChatApp(chatViewModel = chatViewModel)
                }
            }
        }
    }
}

@Composable
fun LChatApp(
    chatViewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by chatViewModel.currentScreen.collectAsState()
    val notificationBanner by chatViewModel.notificationBanner.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> {
                        HomeScreen(viewModel = chatViewModel)
                    }
                    AppScreen.CHAT_DETAIL -> {
                        ChatDetailScreen(viewModel = chatViewModel)
                    }
                    AppScreen.VOICE_CALL -> {
                        VoiceCallScreen(viewModel = chatViewModel)
                    }
                    AppScreen.STORY_VIEWER -> {
                        StoryViewerScreen(viewModel = chatViewModel)
                    }
                    AppScreen.CREATE_STORY -> {
                        CreateStoryScreen(viewModel = chatViewModel)
                    }
                    AppScreen.CREATE_GROUP -> {
                        CreateGroupScreen(viewModel = chatViewModel)
                    }
                    AppScreen.GROUP_DETAILS -> {
                        GroupDetailsScreen(viewModel = chatViewModel)
                    }
                    AppScreen.SEARCH_CONTACTS -> {
                        SearchAndContactsScreen(viewModel = chatViewModel)
                    }
                    AppScreen.PROFILE_SETTINGS -> {
                        ProfileSettingsScreen(viewModel = chatViewModel)
                    }
                }
            }

            // Top push notification banner overlay
            if (notificationBanner.isVisible && currentScreen != AppScreen.VOICE_CALL) {
                NotificationBannerPopup(
                    banner = notificationBanner,
                    onOpenChat = { chatId ->
                        chatViewModel.openChat(chatId)
                    },
                    onDismiss = {
                        chatViewModel.dismissNotificationBanner()
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )
            }
        }
    }
}
