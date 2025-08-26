package net.harutiro.uwbanchorsystem.feature.serial.api

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

class SerialApi {
    companion object {
        private const val TAG = "SerialApi"
    }

    private var usbIoManager: SerialInputOutputManager? = null
    private var port: UsbSerialPort? = null
    private var usbManager: UsbManager? = null

    /**
     * デバイスと接続し、シリアルポートをオープン
     *
     * @param baudRate 通信速度（デフォルト: 3000000）
     * @return 成功: true, 失敗: false
     */
    fun connectDevice(
        baudRate: Int = 3_000_000,
        context: Context,
    ): Result<Unit> {
        // 既存の接続をクリーンアップ
        stopListening()
        close()
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            return Result.failure(Exception("USBデバイスが見つかりません"))
        }

        val driver = availableDrivers.first()
        val connection =
            usbManager?.openDevice(driver.device)
                ?: return Result.failure(Exception("USB接続に失敗しました"))

        return try {
            port = driver.ports.first()
            port?.open(connection)
            port?.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            Log.d(TAG, "接続成功: maxPacketSize = ${port?.readEndpoint?.maxPacketSize}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * シリアル通信の受信を開始
     *
     * @param onLineRead 行単位で受信したデータのコールバック
     * @param onError エラー発生時のコールバック
     */
    fun startListening(
        onLineRead: (String) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        // ポートが初期化されていない場合はエラーを返す
        if (port == null) {
            onError?.invoke(IllegalStateException("USB接続が初期化されていません"))
            return
        }
        val buffer = StringBuilder()

        usbIoManager =
            SerialInputOutputManager(
                port,
                object : SerialInputOutputManager.Listener {
                    override fun onRunError(e: Exception) {
                        Log.e(TAG, "SerialIOManagerエラー: ${e.message}")
                        onError?.invoke(e)
                    }

                    override fun onNewData(data: ByteArray) {
                        val receivedText = String(data)
                        buffer.append(receivedText)

                        var newlineIndex = buffer.indexOf("\n")
                        while (newlineIndex != -1) {
                            val line = buffer.substring(0, newlineIndex).trimEnd('\r')
                            Log.d(TAG, "受信: $line")
                            onLineRead(line)
                            buffer.delete(0, newlineIndex + 1)
                            newlineIndex = buffer.indexOf("\n")
                        }
                    }
                },
            ).apply {
                start()
            }
    }

    /**
     * 通信の受信を停止
     */
    fun stopListening() {
        try {
            usbIoManager?.stop()
            usbIoManager?.listener = null
        } catch (e: Exception) {
            Log.w(TAG, "SerialInputOutputManager停止時にエラー: ${e.message}")
        } finally {
            usbIoManager = null
        }
    }

    /**
     * ポートを閉じてリソースを解放
     */
    fun close() {
        try {
            port?.close()
        } catch (e: Exception) {
            Log.w(TAG, "ポートクローズ時にエラー: ${e.message}")
        } finally {
            port = null
        }
    }
}
