package com.example.kiparys

import android.app.Application
import android.os.Build.VERSION.SDK_INT
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.kiparys.Constants.SERVICES_CONNECT_TIMEOUT
import com.example.kiparys.Constants.SERVICES_READ_TIMEOUT
import com.example.kiparys.Constants.SERVICES_WRITE_TIMEOUT
import com.example.kiparys.data.repository.DataManagementRepository
import com.example.kiparys.data.repository.DataStoreRepository
import com.example.kiparys.data.repository.MessagingRepository
import com.example.kiparys.service.DataManagementService
import com.example.kiparys.service.MessagingService
import com.example.kiparys.util.NotificationChannelsManager
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class KiparysApplication : Application(), ImageLoaderFactory {

    lateinit var dataStoreRepository: DataStoreRepository
        private set

    lateinit var messagingRepository: MessagingRepository
        private set

    lateinit var dataManagementRepository: DataManagementRepository
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this)
        super.onCreate()

        FirebaseApp.initializeApp(this)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        val notificationChannelsManager = NotificationChannelsManager(this)
        notificationChannelsManager.createNotificationChannels()

        dataStoreRepository = DataStoreRepository.getInstance(this)

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(SERVICES_CONNECT_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(SERVICES_READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(SERVICES_WRITE_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            .build()

        val retrofitMessagingService = Retrofit.Builder()
            .baseUrl(BuildConfig.MESSAGING_SERVICE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val messagingService = retrofitMessagingService.create(MessagingService::class.java)
        messagingRepository = MessagingRepository(messagingService)

        val retrofitDataManagementService = Retrofit.Builder()
            .baseUrl(BuildConfig.DATA_MANAGEMENT_SERVICE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val dataManagementService =
            retrofitDataManagementService.create(DataManagementService::class.java)
        dataManagementRepository = DataManagementRepository(dataManagementService)
    }


    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }

}
