package com.solanasuper.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class P2PTransferManager(private val context: Context) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    
    // Explicitly disable Wi-Fi/LAN upgrades (Bluetooth only) to prevent crashes
    private val parameters = com.google.android.gms.nearby.connection.ConnectionOptions.Builder()
        .setDisruptiveUpgrade(false)
        .build()

    // Switch to P2P_STAR as requested for stabilization
    private val strategy = Strategy.P2P_STAR 
    
    private val serviceId = "com.solanasuper.p2p"
    private val TAG = "SovereignLifeOS"

    interface P2PCallback {
        fun onPeerFound(endpointId: String)
        fun onConnectionInitiated(endpointId: String, info: ConnectionInfo)
        fun onDataReceived(endpointId: String, data: ByteArray)
        fun onConnected(endpointId: String)
        fun onError(message: String)
        fun onDisconnected(endpointId: String)
    }

    var callback: P2PCallback? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "P2P: Connection initiated with $endpointId. Token: ${info.authenticationToken}")
            // Manual verification required
            callback?.onConnectionInitiated(endpointId, info)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Log.d(TAG, "P2P: Connected to $endpointId")
                callback?.onConnected(endpointId)
            } else {
                Log.e(TAG, "P2P: Connection failed to $endpointId: ${result.status.statusMessage}")
                callback?.onError("Connection failed: ${result.status.statusMessage}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "P2P: Disconnected from $endpointId")
            callback?.onDisconnected(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val data = payload.asBytes()
                if (data != null) {
                    callback?.onDataReceived(endpointId, data)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Track progress if needed
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                 Log.d(TAG, "P2P: Payload transfer success to $endpointId")
            }
        }
    }
    
    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    fun startAdvertising() {
        Log.d(TAG, "P2P: Starting Advertising...")
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            "SolanaSuperUser", 
            serviceId,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "P2P: Advertising started successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "P2P: Advertising failed!", e)
            callback?.onError("Advertising failed: ${e.localizedMessage}")
        }
    }

    fun startDiscovery() {
        Log.d(TAG, "P2P: Starting Discovery...")
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId,
            object : com.google.android.gms.nearby.connection.EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo) {
                    Log.d(TAG, "P2P: Peer Found: $endpointId (${info.endpointName})")
                    callback?.onPeerFound(endpointId)
                    // Auto-connect for prototype, using explicit ConnectionOptions
                    connectionsClient.requestConnection(
                        "SolanaSuperPeer", 
                        endpointId, 
                        connectionLifecycleCallback,
                        parameters
                    ).addOnFailureListener { e ->
                            Log.e(TAG, "P2P: Request connection failed", e)
                            callback?.onError("Request connection failed: ${e.localizedMessage}")
                        }
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "P2P: Peer Lost: $endpointId")
                }
            },
            options
        ).addOnSuccessListener {
            Log.d(TAG, "P2P: Discovery started successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "P2P: Discovery failed!", e)
            callback?.onError("Discovery failed: ${e.localizedMessage}")
        }
    }

    fun sendData(endpointId: String, data: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(data))
    }

    fun stop() {
        Log.d(TAG, "P2P: Stopping all operations")
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}
