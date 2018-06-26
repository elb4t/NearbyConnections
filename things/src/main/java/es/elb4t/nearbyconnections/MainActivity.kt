package es.elb4t.nearbyconnections

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import java.io.IOException


class MainActivity : Activity() {
    private val SERVICE_ID = "es.elb4t.nearbyconnections"
    private val TAG = "Things:"
    private val PIN_LED = "BCM18"
    var mLedGpio: Gpio? = null
    private var ledStatus: Boolean? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configuración del LED
        ledStatus = false
        val service = PeripheralManager.getInstance()

        try {
            mLedGpio = service.openGpio(PIN_LED)
            mLedGpio?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        } catch (e: IOException) {
            Log.e(TAG, "Error en el API PeripheralIO", e)
        }
        // Arrancamos modo anunciante
        startAdvertising()
    }

    private fun startAdvertising() {
        Nearby.getConnectionsClient(this).startAdvertising(
                "Nearby LED",
                SERVICE_ID,
                mConnectionLifecycleCallback,
                AdvertisingOptions(Strategy.P2P_STAR))
                .addOnSuccessListener {
                    Log.i(TAG, "Estamos en modo anunciante!")
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error al comenzar el modo anunciante", it)
                }
    }

    private fun stopAdvertising() {
        Nearby.getConnectionsClient(this).stopAdvertising()
        Log.i(TAG, "Detenido el modo anunciante!")
    }

    private val mConnectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Nearby.getConnectionsClient(applicationContext)
                    .acceptConnection(endpointId, mPayloadCallback)
            Log.i(TAG, "Aceptando conexión entrante sin autenticación")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "Estamos conectados!")
                    stopAdvertising()
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> Log.i(TAG, "Conexión rechazada por uno o ambos lados")
                ConnectionsStatusCodes.STATUS_ERROR -> Log.i(TAG, "Conexión perdida antes de ser aceptada")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Desconexión del endpoint, no se pueden intercambiar más datos.")
            startAdvertising()
        }
    }

    private val mPayloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val message = String(payload.asBytes()!!)
            Log.i(TAG, "Se ha recibido una transferencia desde ($endpointId) con el siguiente contenido: $message")
            disconnect(endpointId)
            when (message) {
                "SWITCH" -> switchLED()
                else -> Log.w(TAG, "No existe una acción asociada a este mensaje.")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Actualizaciones sobre el proceso de transferencia
        }
    }

    fun switchLED() {
        try {
            if (ledStatus!!) {
                mLedGpio?.value = false
                ledStatus = false
                Log.i(TAG, "LED OFF")
            } else {
                mLedGpio?.value = true
                ledStatus = true
                Log.i(TAG, "LED ON")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error en el API PeripheralIO", e)
        }
    }

    protected fun disconnect(endpointId: String) {
        Nearby.getConnectionsClient(this).disconnectFromEndpoint(endpointId)
        Log.i(TAG, "Desconectado del endpoint ($endpointId).")
        startAdvertising()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertising()
        if (mLedGpio != null) {
            try {
                mLedGpio?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error en el API PeripheralIO", e)
            } finally {
                mLedGpio = null
            }
        }
    }
}
