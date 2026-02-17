package com.solanasuper.network

import com.solanasuper.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IpfsUploader {
    
    suspend fun uploadJson(data: JSONObject): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Use QuickNode IPFS Gateway
                // Note: QuickNode IPFS upload endpoint is usually https://api.quicknode.com/ipfs/rest/v1/s3/put_object 
                // BUT user config says QUICKNODE_IPFS_URL=https://your-custom-gateway.mypinata.cloud/ipfs/
                // This suggests the config is a GATEWAY (read-only), not an API (write).
                // However, user instruction: "perform an HTTP POST to BuildConfig.QUICKNODE_IPFS_URL to pin it".
                // We will follow the instruction strictly, but robustly handle if it fails.
                // It's possible the user set up a custom pinning service proxy.
                
                val endpoint = BuildConfig.QUICKNODE_IPFS_URL
                if (endpoint.isEmpty() || endpoint.contains("ipfs.io")) {
                     com.solanasuper.utils.AppLogger.w("IpfsUploader", "IPFS Upload skipped: No valid endpoint configured.")
                     return@withContext null
                }
                
                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                
                // AUTHENTICATION
                val jwt = BuildConfig.IPFS_JWT
                if (jwt.isNotEmpty()) {
                    connection.setRequestProperty("Authorization", "Bearer $jwt")
                } else {
                    // Legacy/Open: Check if User provided keys in URL or relies on open gateway (rarely works for uploads)
                    com.solanasuper.utils.AppLogger.w("IpfsUploader", "No IPFS_JWT found. Upload may fail on secure gateways.")
                }
                
                connection.outputStream.use { 
                    it.write(data.toString().toByteArray())
                }
                
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    // Parse CID. Common formats:
                    // Pinata: { "IpfsHash": "Qm...", ... }
                    // QuickNode: { "pin": { "cid": "Qm..." } } or similar
                    // Fallback: Check for "Qm..." pattern in response string.
                    
                    val cidRegex = Regex("Qm[a-zA-Z0-9]{44}")
                    val match = cidRegex.find(response)
                    
                    if (match != null) {
                        return@withContext match.value
                    } else {
                        // Try typical JSON fields
                        val json = JSONObject(response)
                        if (json.has("IpfsHash")) return@withContext json.getString("IpfsHash")
                        if (json.has("cid")) return@withContext json.getString("cid")
                        null
                    }
                } else {
                    com.solanasuper.utils.AppLogger.e("IpfsUploader", "Upload Failed: $responseCode")
                    null
                }
            } catch (e: Exception) {
                com.solanasuper.utils.AppLogger.e("IpfsUploader", "Exception during upload", e)
                null
            }
        }
    }
}
