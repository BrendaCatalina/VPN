package com.example.vpn.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VpnServiceImpl : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile
    private var isRunning = false
    private val blockedDomains = listOf("youtube.com", "www.youtube.com") // Lista estática de dominios bloqueados

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val builder = Builder()

        builder.setSession("Mi VPN")
            .addAddress("10.0.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        isRunning = true

        // Usar ExecutorService para manejar el hilo
        vpnExecutor.submit {
            val fd = vpnInterface?.fileDescriptor ?: return@submit
            val input = FileInputStream(fd)
            val output = FileOutputStream(fd)
            val buffer = ByteArray(32767)

            while (isRunning) {
                try {
                    val length = input.read(buffer)
                    if (length > 0) {
                        val packetData = ByteArray(length)
                        System.arraycopy(buffer, 0, packetData, 0, length)

                        // Decodificamos la consulta DNS
                        val domain = extractDomainFromDnsRequest(packetData)
                        if (blockedDomains.contains(domain)) {
                            println("⚠️ Bloqueando acceso a $domain")
                            forwardDnsQuery(packetData, output, isBlocked = true)
                        } else {
                            forwardDnsQuery(packetData, output, isBlocked = false)
                        }
                    }
                } catch (e: IOException) {
                    if (isRunning) {
                        e.printStackTrace()  // Log adicional en caso de error
                    }
                }
            }

            input.close()
            output.close()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        vpnExecutor.shutdownNow()  // Cierra los hilos en ejecución
        try {
            vpnInterface?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        vpnInterface = null
    }

    // Función para extraer el dominio de la consulta DNS
    private fun extractDomainFromDnsRequest(packet: ByteArray): String {
        // Parseo básico del paquete DNS (asumiendo formato estándar)
        val domainBytes = packet.drop(12).takeWhile { it != 0x00.toByte() }
        return String(domainBytes.toByteArray())
    }

    // Función para crear una respuesta DNS falsa (bloquear acceso)
    private fun buildFakeDnsResponse(request: ByteArray): ByteArray {
        val transactionId = request.copyOfRange(0, 2)
        val flags = byteArrayOf(0x81.toByte(), 0x80.toByte())
        val questions = request.copyOfRange(4, 6)
        val answerRRs = byteArrayOf(0x00, 0x01)
        val authorityRRs = byteArrayOf(0x00, 0x00)
        val additionalRRs = byteArrayOf(0x00, 0x00)

        val header = transactionId + flags + questions + answerRRs + authorityRRs + additionalRRs
        val dnsStart = 28
        val questionLen = request.size - dnsStart
        val question = request.copyOfRange(dnsStart, request.size)

        val fakeIp = byteArrayOf(127, 0, 0, 1)

        val answer = byteArrayOf(
            0xC0.toByte(), 0x0C,
            0x00, 0x01,
            0x00, 0x01,
            0x00, 0x00, 0x00, 0x3C,
            0x00, 0x04
        ) + fakeIp

        return header + question + answer
    }

    // Función para reenviar la consulta DNS
    private fun forwardDnsQuery(packet: ByteArray, output: FileOutputStream, isBlocked: Boolean) {
        try {
            if (isBlocked) {
                val response = buildFakeDnsResponse(packet)
                output.write(response)
                output.flush()
            } else {
                val socket = DatagramSocket()
                val dnsPacket = DatagramPacket(packet, packet.size, InetSocketAddress("8.8.8.8", 53))
                socket.send(dnsPacket)

                val buffer = ByteArray(512)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)
                output.write(buffer, 0, responsePacket.length)
                output.flush()
                socket.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
