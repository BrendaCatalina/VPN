package com.example.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import com.example.vpn.ui.theme.VPNTheme
import com.example.vpn.vpn.VpnServiceImpl

class MainActivity : ComponentActivity() {

    private val VPN_REQUEST_CODE = 0x0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VPNTheme {
                Surface {
                    Button(onClick = {
                        val intent = VpnService.prepare(this)
                        if (intent != null) {
                            startActivityForResult(intent, VPN_REQUEST_CODE)
                        } else {
                            startVpnService()
                        }
                    }) {
                        Text("Iniciar VPN")
                    }
                }
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, VpnServiceImpl::class.java)
        startService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startVpnService()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
