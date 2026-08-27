package de.nichu42.boxviewer.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://api.opensensemap.org/"

    private val moshi = Moshi.Builder()
        .add(Measurement::class.java, MeasurementAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Shared OkHttpClient for all network calls (including Photon/Nominatim geocoding).
    // Reusing avoids per-geocode ConnectionPool/Dispatcher thread spikes in cached state.
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiLoggingInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            // In release, BODY buffers entire JSON into heap (50 KB per call) for logcat.
            // Gate behind BuildConfig.DEBUG so background widget refreshes don't spike Anon RSS.
            level = if (de.nichu42.boxviewer.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "BoxViewer/${de.nichu42.boxviewer.BuildConfig.VERSION_NAME} (contact: nichu42@42bit.email)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val api: OpenSenseMapApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenSenseMapApi::class.java)
    }
}
