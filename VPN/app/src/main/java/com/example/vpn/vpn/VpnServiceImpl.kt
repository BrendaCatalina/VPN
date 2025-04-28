package com.example.vpn.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.content.Intent
import java.io.IOException

class VpnServiceImpl : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()

        builder.setSession("Mi VPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
