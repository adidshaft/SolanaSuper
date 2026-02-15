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
    private val strategy = Strategy.P2P_CLUSTER // Supports M-to-N, good for small groups/mesh
    private val serviceId = "com.solanasuper.p2p"
    private val TAG = "SovereignLifeOS"

    // Simplified callback interface
    interface P2PCallback {
        fun onPeerFound(endpointId: String)
        fun onDataReceived(endpointId: String, data: ByteArray)
        fun onConnected(endpointId: String)
        fun onError(message: String)
    }

    var callback: P2PCallback? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "P2P: Connection initiated with $endpointId")
            // Automatically accept for prototype
            connectionsClient.acceptConnection(endpointId, payloadCallback)
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
        }
    }

    fun startAdvertising() {
        Log.d(TAG, "P2P: Starting Advertising...")
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            "SolanaSuperUser", // Should be random alias
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
                    // Auto-connect for prototype
                    connectionsClient.requestConnection("SolanaSuperPeer", endpointId, connectionLifecycleCallback)
                        .addOnFailureListener { e ->
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
