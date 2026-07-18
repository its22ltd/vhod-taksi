package com.its22.vhodtaksi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID

/**
 * Изпращане на данни към 80мм термо принтер по Bluetooth Classic (RFCOMM / SPP).
 * Повечето евтини 80мм принтери работят точно по този профил.
 */
object BtPrinter {
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun printBytes(mac: String, data: ByteArray) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw Exception("Устройството няма Bluetooth")
        if (mac.isBlank()) throw Exception("Не е избран принтер (вижте Настройки)")
        if (!adapter.isEnabled) throw Exception("Bluetooth е изключен")

        val device: BluetoothDevice = adapter.getRemoteDevice(mac)
        var socket: BluetoothSocket? = null
        try {
            adapter.cancelDiscovery()
            socket = device.createRfcommSocketToServiceRecord(SPP)
            socket.connect()
            val out: OutputStream = socket.outputStream
            // изпращаме на порции, за да не препълним буфера на принтера
            var offset = 0
            val chunk = 512
            while (offset < data.size) {
                val len = minOf(chunk, data.size - offset)
                out.write(data, offset, len)
                out.flush()
                offset += len
                Thread.sleep(20)
            }
            Thread.sleep(400)
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // игнорираме
            }
        }
    }
}
