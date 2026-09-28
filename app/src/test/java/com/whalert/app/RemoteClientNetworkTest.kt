package com.whalert.app

import com.whalert.app.core.NetResult
import com.whalert.app.data.RemoteClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * REAL network tests against a local MockWebServer (test-only; the shipped app talks
 * exclusively to the operator's HTTPS backend, or reports Disabled honestly).
 *
 * RemoteClient.call() is synchronous internally, so we can drive it from a plain
 * blocking coroutine runner here without extra test dependencies.
 */
class RemoteClientNetworkTest {

    private lateinit var server: MockWebServer

    private fun <T> runBlocking(caller: suspend () -> NetResult<T>): T? {
        var result: NetResult<T>? = null
        val t = Thread { result = kotlinx.coroutines.runBlocking { caller() } }
        t.start(); t.join(30_000)
        return (result as? NetResult.Success)?.data
    }

    private fun <T> runRaw(caller: suspend () -> NetResult<T>): NetResult<T>? {
        var result: NetResult<T>? = null
        val t = Thread { result = kotlinx.coroutines.runBlocking { caller() } }
        t.start(); t.join(30_000)
        return result
    }

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        RemoteClient.overrideBaseUrlForTest = server.url("/").toString().removeSuffix("/")
    }

    @After fun tearDown() {
        RemoteClient.overrideBaseUrlForTest = null
        server.shutdown()
    }

    @Test
    fun `register success returns token`() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody("""{"ok":true,"token":"tok-123","message":""}"""))
        val token = runBlocking { RemoteClient.register("+22378123456", "a@b.org") }
        assertEquals("tok-123", token)
        val rec = server.takeRequest()
        assertEquals("POST", rec.method)
        assertEquals("/api/v1/accounts", rec.path)
        assertTrue(rec.body.readUtf8().contains("+22378123456"))
    }

    @Test
    fun `http 500 surfaces as HttpError not fake success`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom")) // retry attempt
        val r = runRaw { RemoteClient.login("+22378123456") }
        assertTrue("expected HttpError, got $r", r is NetResult.HttpError)
        assertEquals(500, (r as NetResult.HttpError).code)
    }

    @Test
    fun `invalid json is rejected — never fabricated data`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>not json</html>"))
        val r = runRaw { RemoteClient.login("+22378123456") }
        assertTrue(r is NetResult.HttpError)
    }

    @Test
    fun `connection refused becomes NetworkFailure with real reason`() {
        val deadPort = server.port
        server.shutdown()
        RemoteClient.overrideBaseUrlForTest = "http://localhost:$deadPort"
        val r = runRaw { RemoteClient.fetchStats() }
        assertTrue("expected NetworkFailure, got $r", r is NetResult.NetworkFailure)
    }

    @Test
    fun `syncCase posts reference and status`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val ok = runBlocking { RemoteClient.syncCase("WHA-1", "spam", "SUBMITTED") }
        assertEquals(true, ok)
        val rec = server.takeRequest()
        assertEquals("/api/v1/cases", rec.path)
        assertTrue(rec.body.readUtf8().contains("WHA-1"))
    }
}
