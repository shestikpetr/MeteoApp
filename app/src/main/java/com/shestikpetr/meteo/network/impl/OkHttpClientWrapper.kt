package com.shestikpetr.meteo.network.impl

import android.util.Log
import com.shestikpetr.meteo.network.interfaces.HttpClient
import com.shestikpetr.meteo.network.interfaces.HttpCallback
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp3 implementation of HttpClient interface.
 * This implementation wraps OkHttp3 functionality and provides the necessary
 * interceptors for content-length fixing and logging as defined in MeteoModule.
 */
@Singleton
class OkHttpClientWrapper @Inject constructor() : HttpClient {

    private var okHttpClient: OkHttpClient

    init {
        okHttpClient = createOkHttpClient()
    }

    private fun createOkHttpClient(): OkHttpClient {
        // Create selective logger
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        // Interceptor for fixing Content-Length issues
        val contentLengthFixInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (request.url.pathSegments.contains("latest")) {
                try {
                    val responseBody = response.body
                    if (responseBody != null) {
                        // Read all available data
                        val bodyBytes = responseBody.bytes()
                        val actualLength = bodyBytes.size
                        val declaredLength = response.header("Content-Length")?.toIntOrNull() ?: 0

                        Log.d("CONTENT_FIX", "URL: ${request.url}")
                        Log.d(
                            "CONTENT_FIX",
                            "Declared length: $declaredLength, actual: $actualLength"
                        )

                        // If there's a mismatch - fix it
                        if (declaredLength != actualLength) {
                            Log.w(
                                "CONTENT_FIX",
                                "Fixing Content-Length: $declaredLength -> $actualLength"
                            )
                        }

                        // Create new response with correct data
                        response.newBuilder()
                            .body(bodyBytes.toResponseBody(responseBody.contentType()))
                            .removeHeader("Content-Length")
                            .addHeader("Content-Length", actualLength.toString())
                            .removeHeader("Connection") // Remove conflicting headers
                            .addHeader("Connection", "keep-alive")
                            .build()
                    } else {
                        response
                    }
                } catch (e: Exception) {
                    Log.e("CONTENT_FIX", "Error fixing Content-Length: ${e.message}")
                    response
                }
            } else {
                response
            }
        }

        // Simple logger for debugging
        val customLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            if (request.url.pathSegments.contains("latest")) {
                Log.d("CUSTOM_HTTP", "URL: ${request.url}")
                Log.d("CUSTOM_HTTP", "Response code: ${response.code}")
                Log.d("CUSTOM_HTTP", "Content-Length: ${response.header("Content-Length")}")
                Log.d("CUSTOM_HTTP", "Content-Type: ${response.header("Content-Type")}")
                Log.d("CUSTOM_HTTP", "Connection: ${response.header("Connection")}")
            }

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(contentLengthFixInterceptor) // FIRST - fix data
            .addInterceptor(customLoggingInterceptor)    // SECOND - log fixed data
            .addInterceptor(logging)                     // THIRD - standard logging
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    override fun executeRequest(request: Request): Response {
        return okHttpClient.newCall(request).execute()
    }

    override fun executeRequestAsync(request: Request, callback: HttpCallback) {
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response)
            }
        })
    }

    override fun newRequestBuilder(): Request.Builder {
        return Request.Builder()
    }

    override fun setConnectionTimeout(timeoutSeconds: Long) {
        okHttpClient = okHttpClient.newBuilder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }

    override fun setReadTimeout(timeoutSeconds: Long) {
        okHttpClient = okHttpClient.newBuilder()
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
    }
}