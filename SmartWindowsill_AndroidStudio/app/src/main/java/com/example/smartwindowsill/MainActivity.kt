package com.example.smartwindowsill

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.smartwindowsill.ui.theme.SmartWindowsillTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        val allGranted = permissions.all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val granted = perms.values.all { it }
            if (granted) {
                vm.startConnect()
            }
            setContent {
                SmartWindowsillTheme {
                    PlantListScreen(mainViewModel = vm)
                }
            }
        }

        if (allGranted) {
            vm.startConnect()
            setContent {
                SmartWindowsillTheme {
                    PlantListScreen(mainViewModel = vm)
                }
            }
        } else {
            // запрос разрешений; setContent будет вызван в callback
            launcher.launch(permissions.toTypedArray())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    SmartWindowsillTheme {
        PlantListScreen()
    }
}