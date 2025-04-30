package com.example.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vpn.ui.theme.VPNTheme
import com.example.vpn.vpn.VpnServiceImpl

class MainActivity : ComponentActivity() {

    private val VPN_REQUEST_CODE = 0x0F
    private var isVpnRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VPNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        isVpnRunning = isVpnRunning,
                        onStartVpn = {
                            val intent = VpnService.prepare(this)
                            if (intent != null) {
                                startActivityForResult(intent, VPN_REQUEST_CODE)
                            } else {
                                startVpnService()
                            }
                        },
                        onStopVpn = {
                            stopVpnService()
                        }
                    )
                }
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, VpnServiceImpl::class.java)
        startService(intent)
        isVpnRunning = true
        showToast("VPN iniciada")
    }

    private fun stopVpnService() {
        val intent = Intent(this, VpnServiceImpl::class.java)
        stopService(intent)
        isVpnRunning = false
        showToast("VPN detenida")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

@Composable
fun MainScreen(
    isVpnRunning: Boolean,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Control de VPN", style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onStartVpn, enabled = !isVpnRunning) {
            Text("Iniciar VPN")
        }
        Button(onClick = onStopVpn, enabled = isVpnRunning) {
            Text("Detener VPN")
        }
        // Simple texto en lugar del botón de bloquear YouTube
        Text("YouTube está bloqueado cuando la VPN está activa.")
    }
}
