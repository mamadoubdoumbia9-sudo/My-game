package com.whalert.app.data

import com.whalert.app.BuildConfig
import com.whalert.app.core.NetResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * REAL HTTPS client for the optional WhAlert backend.
 *
 * IMPORTANT HONESTY RULES:
 *  - BuildConfig.API_BASE_URL comes from a build-time env var (never committed).
 *  - If no real backend URL was configured at build time, REMOTE_ENABLED=false and
 *    every remote call returns NetResult.Disabled — the UI then states clearly that
 *    remote sync is unavailable. We NEVER fake HTTP responses.
 *  - There is NO WhatsApp API here. WhatsApp does not publish a public reporting API;
 *    this client talks ONLY to the operator's own backend, if any.
 */
object RemoteClient {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Test seam: unit tests (MockWebServer) swap this to point at a local test TLS server.
     *  Production default is always the real HTTPS backend configured at build time. */
    internal var overrideBaseUrlForTest: String? = null

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val backendConfigured: Boolean get() = BuildConfig.REMOTE_ENABLED || overrideBaseUrlForTest != null

    @Serializable data class RegisterReq(val number: String, val email: String?)
    @Serializable data class LoginReq(val number: String)
    @Serializable data class ApiResp(val ok: Boolean, val token: String? = null, val message: String = "")
    @Serializable data class SyncCaseReq(val reference: String, val category: String, val status: String, val updatedAt: Long)
    @Serializable data class StatsResp(val ok: Boolean, val cases: Int = 0, val transmissions: Int = 0, val errors: Int = 0)

    private fun baseUrlOrError(): String? =
        (overrideBaseUrlForTest ?: BuildConfig.API_BASE_URL.takeIf { BuildConfig.REMOTE_ENABLED })?.also {
            // Production URLs are strictly HTTPS (enforced by network_security_config.xml too).
            // Cleartext loopback is tolerated ONLY for the in-process MockWebServer test seam,
            // never shipped: in release builds overrideBaseUrlForTest is always null.
            val isTestLoopback = overrideBaseUrlForTest != null &&
                (it.startsWith("http://127.0.0.1:") || it.startsWith("http://localhost:"))
            require(it.startsWith("https://") || isTestLoopback) {
                "SECURITY: backend URL must be HTTPS"
            }
        }

    private suspend fun <T> execute(block: suspend (String) -> NetResult<T>): NetResult<T> {
        val base = baseUrlOrError() ?: return NetResult.Disabled
        return block(base)
    }

    /** One reasonable retry on network failure; no infinite loops. */
    private suspend fun call(path: String, bodyJson: String?, method: String = "POST"): Pair<Int, String> {
        val base = overrideBaseUrlForTest ?: BuildConfig.API_BASE_URL
        val url = base.trimEnd('/') + path
        var attempt = 0
        var lastPair: Pair<Int, String> = -1 to "network error"
        while (attempt < 2) {
            attempt++
            try {
                val reqBody = bodyJson?.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url) // HTTPS enforced: base URL validated below + cleartext blocked by network config
                    .method(method, reqBody)
                    .apply { if (method == "GET") header("Accept", "application/json") }
                    .build()
                client.newCall(request).execute().use { resp ->
                    val text = (resp.body?.string() ?: "").take(4096)
                    if (resp.code in 200..299 || resp.code in 400..499) return resp.code to text
                    lastPair = resp.code to text
                }
            } catch (e: Exception) {
                lastPair = -1 to (e.javaClass.simpleName + ": " + sanitize(e.message ?: ""))
            }
        }
        return lastPair
    }

    private fun sanitize(s: String): String = s.filter { !it.isDigit() || s.length < 12 }.take(200)

    suspend fun register(number: String, email: String?): NetResult<String> = execute {
        val (code, body) = call("/api/v1/accounts", json.encodeToString(RegisterReq.serializer(), RegisterReq(number, email)))
        when {
            code == -1 -> NetResult.NetworkFailure(body)
            code in 200..299 -> {
                val r = runCatching { json.decodeFromString<ApiResp>(body) }.getOrNull()
                if (r?.ok == true && r.token != null) NetResult.Success(r.token)
                else NetResult.HttpError(code, "Réponse invalide du serveur.")
            }
            else -> NetResult.HttpError(code, "Le serveur a renvoyé HTTP $code.")
        }
    }

    suspend fun login(number: String): NetResult<String> = execute {
        val (code, body) = call("/api/v1/sessions", json.encodeToString(LoginReq.serializer(), LoginReq(number)), "POST")
        when {
            code == -1 -> NetResult.NetworkFailure(body)
            code in 200..299 -> {
                val r = runCatching { json.decodeFromString<ApiResp>(body) }.getOrNull()
                if (r?.ok == true && r.token != null) NetResult.Success(r.token)
                else NetResult.HttpError(code, "Réponse invalide du serveur.")
            }
            else -> NetResult.HttpError(code, "Le serveur a renvoyé HTTP $code.")
        }
    }

    suspend fun syncCase(reference: String, category: String, status: String): NetResult<Boolean> = execute {
        val (code, body) = call("/api/v1/cases", json.encodeToString(SyncCaseReq.serializer(), SyncCaseReq(reference, category, status, System.currentTimeMillis())))
        when {
            code == -1 -> NetResult.NetworkFailure(body)
            code in 200..299 -> NetResult.Success(true)
            else -> NetResult.HttpError(code, "HTTP $code")
        }
    }

    suspend fun fetchStats(): NetResult<StatsResp> = execute {
        val (code, body) = call("/api/v1/stats", null, "GET")
        when {
            code == -1 -> NetResult.NetworkFailure(body)
            code in 200..299 -> {
                val r = runCatching { json.decodeFromString<StatsResp>(body) }.getOrNull()
                if (r != null) NetResult.Success(r) else NetResult.HttpError(code, "JSON invalide")
            }
            else -> NetResult.HttpError(code, "HTTP $code")
        }
    }
}
