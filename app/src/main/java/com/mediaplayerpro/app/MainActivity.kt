package com.mediaplayerpro.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayerpro.app.ui.MainViewModel
import com.mediaplayerpro.app.ui.Screen
import com.mediaplayerpro.app.ui.screens.LibraryScreen
import com.mediaplayerpro.app.ui.screens.NowPlayingScreen
import com.mediaplayerpro.app.ui.screens.VideoScreen
import com.mediaplayerpro.app.ui.theme.AppColors
import com.mediaplayerpro.app.ui.theme.MediaPlayerProTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.onPermissionResult(hasMediaPermission())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            MediaPlayerProTheme {
                AppRoot(
                    vm = vm,
                    onRequestPermission = ::requestMediaPermission,
                    onOpenSettings = ::openAppSettings,
                    onEnterPip = ::enterPip
                )
            }
        }

        if (hasMediaPermission()) vm.onPermissionResult(true) else requestMediaPermission()
    }

    override fun onResume() {
        super.onResume()
        val granted = hasMediaPermission()
        if (granted != vm.hasPermission.value) vm.onPermissionResult(granted)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { vm.playExternal(it, intent.type) }
        }
    }

    // ---------------- Quyền ----------------

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.POST_NOTIFICATIONS
        ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun hasMediaPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            granted(Manifest.permission.READ_MEDIA_AUDIO) || granted(Manifest.permission.READ_MEDIA_VIDEO)
        else granted(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun requestMediaPermission() = permissionLauncher.launch(requiredPermissions())

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    // ---------------- Picture-in-Picture ----------------

    private fun enterPip() {
        val s = vm.player.value
        val raw = if (s.videoWidth > 0 && s.videoHeight > 0) s.videoWidth.toFloat() / s.videoHeight else 16f / 9f
        val ratio = when {
            raw > 2.39f -> Rational(239, 100)
            raw < 1f / 2.39f -> Rational(100, 239)
            s.videoWidth > 0 && s.videoHeight > 0 -> Rational(s.videoWidth, s.videoHeight)
            else -> Rational(16, 9)
        }
        runCatching {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build())
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.screen.value == Screen.VIDEO && vm.player.value.isPlaying) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        vm.inPip.value = isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        // Video: dừng khi app xuống nền (trừ PiP). Nhạc: tiếp tục phát nền.
        if (vm.screen.value == Screen.VIDEO) vm.pause()
    }
}

@Composable
private fun AppRoot(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnterPip: () -> Unit,
) {
    val screen by vm.screen.collectAsStateWithLifecycle()
    Surface(color = AppColors.Bg, modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = screen, label = "screen") { s ->
            when (s) {
                Screen.LIBRARY -> LibraryScreen(vm, onRequestPermission, onOpenSettings)
                Screen.NOW_PLAYING -> NowPlayingScreen(vm)
                Screen.VIDEO -> VideoScreen(vm, onEnterPip)
            }
        }
    }
}
