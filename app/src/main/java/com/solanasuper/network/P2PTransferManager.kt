package com.solanasuper.network

import android.content.Context
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

    // Simplified callback interface
    interface P2PCallback {
        fun onPeerFound(endpointId: String)
        fun onDataReceived(endpointId: String, data: ByteArray)
        fun onConnected(endpointId: String)
    }

    var callback: P2PCallback? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatically accept for prototype
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                callback?.onConnected(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            // Handle disconnection
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
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            "SolanaSuperUser", // Should be random alias
            serviceId,
            connectionLifecycleCallback,
            options
        )
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId,
            object : com.google.android.gms.nearby.connection.EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo) {
                    callback?.onPeerFound(endpointId)
                    // Auto-connect for prototype
                    connectionsClient.requestConnection("SolanaSuperPeer", endpointId, connectionLifecycleCallback)
                }

                override fun onEndpointLost(endpointId: String) {}
            },
            options
        )
    }

    fun sendData(endpointId: String, data: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(data))
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}
