package com.example.smartwindowsill

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.util.Log
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

val S_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c5c9c5c9")
val C_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-e0b3c3f4e4c5")
val DESC_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {
    private var gatt: BluetoothGatt? = null
    val rawSensorData = MutableStateFlow("Загрузка...")
    private val incomingBuffer = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private val EMIT_TIMEOUT_MS: Long = 400
    private val emitRunnable = Runnable { flushBufferOnTimeout() }

    fun connect() {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                rawSensorData.value = "Bluetooth not supported"
                return
            }

            // На Android 12+ требуется BLUETOOTH_CONNECT для доступа к bondedDevices
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                rawSensorData.value = "No BLUETOOTH_CONNECT permission"
                return
            }

            val dev = adapter.bondedDevices.find { it.name == "ESP32_SENSOR" }
            if (dev == null) {
                rawSensorData.value = "Device ESP32_SENSOR not paired"
                return
            }

            gatt = dev.connectGatt(context, false, callback)
        } catch (e: SecurityException) {
            rawSensorData.value = "Permission denied: ${e.message}"
        } catch (e: Exception) {
            rawSensorData.value = "Connect error: ${e.message}"
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                // Request a larger MTU because our payload is > 20 bytes
                g.requestMtu(512)
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(g, mtu, status)
            Log.d("BT", "MTU changed to $mtu status $status")
            // Discover services after MTU is negotiated
            g.discoverServices()
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val char = g.getService(S_UUID)?.getCharacteristic(C_UUID) ?: return
            g.setCharacteristicNotification(char, true)
            val desc = char.getDescriptor(DESC_UUID)
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(desc)
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, char: BluetoothGattCharacteristic) {
            val s = try {
                String(char.value, Charsets.UTF_8)
            } catch (e: Exception) {
                Log.w("BT", "Failed to decode characteristic bytes: ${e.message}")
                return
            }
            Log.d("BT", "onCharacteristicChanged chunk: $s")

            synchronized(incomingBuffer) {
                incomingBuffer.append(s)
                handler.removeCallbacks(emitRunnable)
                handler.postDelayed(emitRunnable, EMIT_TIMEOUT_MS)
            }
        }
    }

    private fun flushBufferOnTimeout() {
        synchronized(incomingBuffer) {
            if (incomingBuffer.isEmpty()) return

            // Если есть завершённый блок с Device UUID: и последующим \n — извлекаем и эмитим все полные блоки
            while (true) {
                val uuidIndex = incomingBuffer.indexOf("Device UUID:")
                if (uuidIndex >= 0) {
                    val endIndex = incomingBuffer.indexOf("\n", uuidIndex)
                    if (endIndex >= 0) {
                        val messageEnd = endIndex + 1
                        val message = incomingBuffer.substring(0, messageEnd)
                        Log.d("BT", "flushBufferOnTimeout full message: $message")
                        rawSensorData.value = message
                        incomingBuffer.delete(0, messageEnd)
                        continue
                    }
                }

                // Иначе эмитим текущее содержимое буфера как частичное сообщение
                val partial = incomingBuffer.toString()
                Log.d("BT", "flushBufferOnTimeout partial message: $partial")
                rawSensorData.value = partial
                incomingBuffer.setLength(0)
                break
            }
        }
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
    }
}