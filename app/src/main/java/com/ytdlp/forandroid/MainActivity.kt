package com.ytdlp.forandroid

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ytdlp.forandroid.ui.HistoryScreen
import com.ytdlp.forandroid.ui.HomeScreen
import com.ytdlp.forandroid.ui.HomeViewModel
import com.ytdlp.forandroid.ui.SettingsScreen
import com.ytdlp.forandroid.ui.theme.YtdlpTheme

private enum class Screen(val title: String) {
    HOME("ytdlp-for-android"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw edge-to-edge (required on targetSdk 35+); Scaffold's
        // innerPadding keeps content clear of status/nav bars.
        enableEdgeToEdge()
        vm.prefillUrl(extractSharedUrl(intent), autoFetch = true)
        setContent {
            val app = application as App
            val themeSettings by app.themeRepo.settings.collectAsState()
            var screenName by rememberSaveable { mutableStateOf(Screen.HOME.name) }
            val screen = Screen.valueOf(screenName)
            BackHandler(enabled = screen != Screen.HOME) { screenName = Screen.HOME.name }
            // F3: download progress lives in a notification, needs runtime consent on 33+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = LocalContext.current
                val requester = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requester.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            YtdlpTheme(themeSettings) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(screen.title) },
                                navigationIcon = {
                                    if (screen != Screen.HOME) {
                                        IconButton(onClick = { screenName = Screen.HOME.name }) {
                                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                },
                                actions = {
                                    if (screen == Screen.HOME) {
                                        IconButton(onClick = { screenName = Screen.HISTORY.name }) {
                                            Icon(Icons.Filled.History, contentDescription = "History")
                                        }
                                        IconButton(onClick = { screenName = Screen.SETTINGS.name }) {
                                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                                        }
                                    }
                                },
                            )
                        },
                    ) { innerPadding ->
                        when (screen) {
                            Screen.HISTORY -> HistoryScreen(
                                vm = vm,
                                modifier = Modifier.padding(innerPadding),
                                onRedownload = { screenName = Screen.HOME.name },
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                themeRepo = app.themeRepo,
                                modifier = Modifier.padding(innerPadding),
                            )
                            Screen.HOME -> HomeScreen(
                                vm = vm,
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.prefillUrl(extractSharedUrl(intent), autoFetch = true)
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_SEND) {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        if (intent.action == Intent.ACTION_VIEW) {
            return intent.dataString
        }
        return null
    }
}
