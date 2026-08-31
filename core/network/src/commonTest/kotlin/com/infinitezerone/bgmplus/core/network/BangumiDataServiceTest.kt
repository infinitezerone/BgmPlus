package com.infinitezerone.bgmplus.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BangumiDataServiceTest {
    @Test
    fun getBangumiData_returnsSuccessAndEtag_on200() =
        runTest {
            val jsonBody =
                """
                {
                    "items": [
                        {
                            "title": "测试动画",
                            "sites": [{"site": "bangumi", "id": "1001"}]
                        }
                    ]
                }
                """.trimIndent()

            val engine =
                MockEngine { _ ->
                    respond(
                        content = jsonBody,
                        status = HttpStatusCode.OK,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType to listOf("application/json"),
                                HttpHeaders.ETag to listOf(""""test-etag-123""""),
                            ),
                    )
                }

            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val service = BangumiDataServiceImpl(client)
            val result = service.getBangumiData(etag = null)

            assertIs<BangumiDataResult.Success>(result)
            assertEquals(1, result.items.size)
            assertEquals("测试动画", result.items.first().title)
            assertEquals(""""test-etag-123"""", result.etag)
        }

    @Test
    fun getBangumiData_returnsNotModified_on304() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals(""""test-etag-123"""", request.headers[HttpHeaders.IfNoneMatch])
                    respond(
                        content = "",
                        status = HttpStatusCode.NotModified,
                    )
                }

            val client =
                HttpClient(engine) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }

            val service = BangumiDataServiceImpl(client)
            val result = service.getBangumiData(etag = """"test-etag-123"""")

            assertIs<BangumiDataResult.NotModified>(result)
        }
}
