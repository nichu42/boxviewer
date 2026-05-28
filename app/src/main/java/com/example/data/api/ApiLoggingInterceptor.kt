package com.example.data.api

import com.example.util.ApiLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.nio.charset.Charset

class ApiLoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Only log and cache response JSON if logging is enabled
        if (!ApiLogger.isLoggingEnabled()) {
            return response
        }

        val url = request.url.toString()
        val responseBody = response.body

        if (responseBody != null) {
            try {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body
                val buffer = source.buffer
                
                val charset = responseBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                val rawJson = buffer.clone().readString(charset)
                
                // Truncate response to 1,000 characters
                val jsonSnippet = if (rawJson.length > 1000) {
                    rawJson.substring(0, 1000)
                } else {
                    rawJson
                }
                
                ApiLogger.responseCache[url] = jsonSnippet
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return response
    }
}
