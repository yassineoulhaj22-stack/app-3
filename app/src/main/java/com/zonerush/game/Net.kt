package com.zonerush.game

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Reseau local SANS internet (Bluetooth + Wi-Fi direct) via Nearby Connections.
 * Topologie en etoile: l'hote relaie les messages a tous les autres joueurs.
 * Premier octet du payload: 0 = texte (jeu), 1 = voix (PCM 16 kHz mono).
 */
class Net(private val ctx: Context, private val l: Listener) {

    interface Listener {
        fun onStatus(s: String)
        fun onPeers(n: Int)
        fun onText(m: String)
        fun onVoice(d: ByteArray)
    }

    private val client = Nearby.getConnectionsClient(ctx)
    private val peers = CopyOnWriteArraySet<String>()
    private val service = "com.zonerush.game"
    private var wanted = ""
    var isHost = false
        private set

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(id: String, info: ConnectionInfo) {
            client.acceptConnection(id, payloads)
        }

        override fun onConnectionResult(id: String, r: ConnectionResolution) {
            if (r.status.isSuccess) {
                peers.add(id)
                l.onPeers(peers.size)
            } else {
                l.onStatus("Connexion refusee, reessaie")
            }
        }

        override fun onDisconnected(id: String) {
            peers.remove(id)
            l.onPeers(peers.size)
        }
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(id: String, p: Payload) {
            val b = p.asBytes() ?: return
            if (b.isEmpty()) return
            if (isHost) {
                val others = peers.filter { it != id }
                if (others.isNotEmpty()) client.sendPayload(others, Payload.fromBytes(b))
            }
            if (b[0] == 1.toByte()) l.onVoice(b.copyOfRange(1, b.size))
            else l.onText(String(b, 1, b.size - 1, Charsets.UTF_8))
        }

        override fun onPayloadTransferUpdate(id: String, u: PayloadTransferUpdate) {}
    }

    fun host(code: String) {
        isHost = true
        val o = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        client.startAdvertising("ZR-$code", service, lifecycle, o)
            .addOnSuccessListener { l.onStatus("Salon $code ouvert") }
            .addOnFailureListener { l.onStatus("Erreur: ${it.message}") }
    }

    fun join(code: String) {
        isHost = false
        wanted = "ZR-$code"
        val o = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        val cb = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(id: String, info: DiscoveredEndpointInfo) {
                if (info.endpointName == wanted) {
                    client.stopDiscovery()
                    l.onStatus("Salon trouve, connexion...")
                    client.requestConnection(Store.name(ctx), id, lifecycle)
                }
            }

            override fun onEndpointLost(id: String) {}
        }
        client.startDiscovery(service, cb, o)
            .addOnSuccessListener { l.onStatus("Recherche du salon $code...") }
            .addOnFailureListener { l.onStatus("Erreur: ${it.message}") }
    }

    fun stopAdvertising() { client.stopAdvertising() }

    fun sendText(s: String) {
        if (peers.isEmpty()) return
        val body = s.toByteArray(Charsets.UTF_8)
        val b = ByteArray(body.size + 1)
        b[0] = 0
        System.arraycopy(body, 0, b, 1, body.size)
        client.sendPayload(peers.toList(), Payload.fromBytes(b))
    }

    fun sendVoice(pcm: ByteArray, n: Int) {
        if (peers.isEmpty()) return
        val b = ByteArray(n + 1)
        b[0] = 1
        System.arraycopy(pcm, 0, b, 1, n)
        client.sendPayload(peers.toList(), Payload.fromBytes(b))
    }

    fun close() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        peers.clear()
    }
}
