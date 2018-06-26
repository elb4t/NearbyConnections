package es.elb4t.nearbyconnections

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import java.io.UnsupportedEncodingException


class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1

    // Consejo: utiliza como SERVICE_ID el nombre de tu paquete
    private val SERVICE_ID = "es.elb4t.nearbyconnections"
    private val TAG = "Mobile:"
    var botonLED: Button? = null
    var textview: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textview = findViewById(R.id.textView1)
        botonLED = findViewById(R.id.buttonLED)

        botonLED?.setOnClickListener {
            Log.i(TAG, "Boton presionado")
            startDiscovery()
            textview?.text = "Buscando..."
        }

        // Comprobación de permisos peligrosos
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        }
    }

    // Gestión de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permisos concedidos")
                } else {
                    Log.i(TAG, "Permisos denegados")
                    textview?.text = "Debe aceptar los permisos para comenzar"
                    botonLED?.isEnabled = false
                }
                return
            }
        }
    }

    private fun startDiscovery() {
        Nearby.getConnectionsClient(this).startDiscovery(
                SERVICE_ID,
                mEndpointDiscoveryCallback,
                DiscoveryOptions(Strategy.P2P_STAR))
                .addOnSuccessListener {
                    Log.i(TAG, "Estamos en modo descubrimiento!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Modo descubrimiento no iniciado.", e)
                }
    }

    private fun stopDiscovery() {
        Nearby.getConnectionsClient(this).stopDiscovery()
        Log.i(TAG, "Se ha detenido el modo descubrimiento.")
    }

    private val mEndpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.i(TAG, "Descubierto dispositivo con Id: $endpointId")
            textview?.text = "Descubierto: ${discoveredEndpointInfo.endpointName}"
            stopDiscovery()
            // Iniciamos la conexión con al anunciante "Nearby LED"
            Log.i(TAG, "Conectando...")
            Nearby.getConnectionsClient(applicationContext)
                    .requestConnection(
                            "Nearby LED",
                            endpointId,
                            mConnectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.i(TAG, "Solicitud lanzada, falta que ambos lados acepten")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error en solicitud de conexión", e)
                        textview?.text = "Desconectado"
                    }
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    private val mConnectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            // Aceptamos la conexión automáticamente en ambos lados.
            Log.i(TAG, "Aceptando conexión entrante sin autenticación")
            Nearby.getConnectionsClient(applicationContext)
                    .acceptConnection(endpointId, mPayloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "Estamos conectados!")
                    textview?.text = "Conectado"
                    sendData(endpointId, "SWITCH")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.i(TAG, "Conexión rechazada por uno o ambos lados")
                    textview?.text = "Desconectado"
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.i(TAG, "Conexión perdida antes de poder ser aceptada")
                    textview?.text = "Desconectado"
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Desconexión del endpoint, no se pueden intercambiar más datos.")
            textview?.text = "Desconectado"
        }
    }

    private val mPayloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            // Payload recibido
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Actualizaciones sobre el proceso de transferencia
        }
    }

    private fun sendData(endpointId: String, mensaje: String) {
        textview?.text = "Transfiriendo..."
        var data: Payload? = null
        try {
            data = Payload.fromBytes(mensaje.toByteArray(charset("UTF-8")))
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Error en la codificación del mensaje.", e)
        }

        Nearby.getConnectionsClient(this).sendPayload(endpointId, data!!)
        Log.i(TAG, "Mensaje enviado.")
    }
}
