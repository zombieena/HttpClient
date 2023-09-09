/**
 * Copyright 2023 bps-e.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bps_e

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * ```AndroidManifest.xml
 * <manifest>
 *     <uses-permission android:name="android.permission.INTERNET" />
 * ```
 */
class HttpClient {
    private suspend fun sendRequest(
        url: String,
        timeout: Int = 5 * 1000,
        onError: (e: Exception) -> Unit,
        onCompleted: (responseCode: Int, responseData: ByteArray) -> Unit
    ) = coroutineScope {
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = timeout
                    readTimeout = timeout

                    // connect()はgetResponseCode()を使用する場合は省略可 ※ここではresponseCode
                    connect()
                    yield()

                    val stream = if (responseCode == HttpURLConnection.HTTP_OK) inputStream else errorStream
                    val responseData = ByteArrayOutputStream()
                    try {
                        val buf = ByteArray(1024)
                        var size: Int
                        while (stream.read(buf).also { size = it } != -1) {
                            responseData.write(buf, 0, size)
                        }
                        responseData.flush()
                    }
                    catch (e: Exception) {
                        throw e
                    }
                    finally {
                        // JDK7からAutoCloseableになったためclose()不要
                        onCompleted(responseCode, responseData.toByteArray())
                        connection.disconnect()
                    }
                }
            }
            catch (e: Exception) {
                onError(e)
            }
        }
    }

    @Suppress("FunctionName")
    companion object {
        const val OK = HttpURLConnection.HTTP_OK

        suspend fun Get(
            url: String,
            timeout: Int = 5 * 1000,
            onError: (e: Exception) -> Unit = {},
            onCompleted: (responseCode: Int, responseData: ByteArray) -> Unit
        ) {
            HttpClient().sendRequest(url, timeout = timeout, onError = onError, onCompleted = onCompleted)
        }

        /**
         * (GET)
         * responseDataをJsonで返すWebApiに対して任意のdata classに変更して返す
         */
        suspend inline fun <reified T> Api(
            api: String,
            noinline onError: (e: Exception) -> Unit,
            noinline onCompleted: (T) -> Unit
        ) {
            Get(api, onError = onError) { code, data ->
                try {
                    if (code == HttpURLConnection.HTTP_OK) {
                        val json = String(data, Charsets.UTF_8)
                        val result = Json.decodeFromString<T>(json)
                        onCompleted(result)
                    }
                    else {
                        onError(java.lang.Exception("$code"))
                    }
                }
                catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }
}