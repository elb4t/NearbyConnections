package es.elb4t.nearbyconnections

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log




class WifiUtils(context: Context) {
    var wifiManager: WifiManager? = null
    var wifiConfig: WifiConfiguration? = null
    private val TAG = "WifiUtils"
    var context: Context? = context

    init {
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiConfig = WifiConfiguration()
    }

    fun listNetworks() {
        val redes = wifiManager?.configuredNetworks
        Log.i(TAG, "Lista de redes configuradas:\n " + redes.toString())
    }

    fun getConnectionInfo(): String {
        Log.i(TAG, "Red actual: " + wifiManager?.getConnectionInfo().toString())
        return wifiManager?.connectionInfo?.ssid + ", " +
                wifiManager?.connectionInfo?.linkSpeed + " Mbps, (RSSI: " +
                wifiManager?.connectionInfo?.rssi + ")"
    }

    fun removeAllAPs() {
        // Solo se pueden eliminar las redes que haya creado esta app!!
        // Si el resultado es false, no se ha eliminado
        val redes = wifiManager?.configuredNetworks
        wifiManager?.disconnect()
        for (red in redes!!) {
            Log.i(TAG, "Intento de eliminar red " + red.SSID + " con " + "resultado " + wifiManager?.removeNetwork(red.networkId))
        }
        wifiManager?.reconnect()
    }

    fun connectToAP(networkSSID: String, networkPasskey: String): Int {
        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        for (result in wifiManager.scanResults) {
            if (result.SSID == networkSSID) {
                val securityMode = getScanResultSecurity(result)
                val wifiConfiguration = createAPConfiguration(networkSSID, networkPasskey, securityMode)
                val res = wifiManager.addNetwork(wifiConfiguration)
                Log.i(TAG, "Intento de añadir red: $res")
                val b = wifiManager.enableNetwork(res, true)
                Log.i(TAG, "Intento de activar red: $b")
                wifiManager.isWifiEnabled = true
                val changeHappen = wifiManager.saveConfiguration()
                if (res != -1 && changeHappen) {
                    Log.i(TAG, "Cambio de red correcto: $networkSSID")
                } else {
                    Log.i(TAG, "Cambio de red erróneo.")
                }
                return res
            }
        }
        return -1
    }

    private fun getScanResultSecurity(scanResult: ScanResult): String {
        val cap = scanResult.capabilities
        val securityModes = arrayOf("WEP", "PSK", "EAP")
        for (i in securityModes.indices.reversed()) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i]
            }
        }
        return "OPEN"
    }

    private fun createAPConfiguration(networkSSID:String, networkPasskey:String,securityMode:String): WifiConfiguration? {
        val wifiConfiguration = WifiConfiguration()
        wifiConfiguration.SSID = "\"" + networkSSID + "\""
        if (securityMode.equals("OPEN", ignoreCase = true)) {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        } else if (securityMode.equals("WEP", ignoreCase = true)) {
            wifiConfiguration.wepKeys[0] = "\"" + networkPasskey + "\""
            wifiConfiguration.wepTxKeyIndex = 0
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
        } else if (securityMode.equals("PSK", ignoreCase = true)) {
            wifiConfiguration.preSharedKey = "\"" + networkPasskey + "\""
            wifiConfiguration.hiddenSSID = true
            wifiConfiguration.status = WifiConfiguration.Status.ENABLED
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
        } else {
            Log.i(TAG, "Modo de seguridad no soportado: $securityMode")
            return null
        }
        return wifiConfiguration
    }
}