package com.aaspas.customer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.aaspas.customer.core.location.AndroidGeocoderLocationSearchService
import com.aaspas.customer.core.location.LocationSearchService
import com.aaspas.customer.core.location.SelectedLocationStore
import com.aaspas.customer.core.maps.GoogleMapsInitializer
import com.aaspas.customer.core.network.NetworkModule
import com.aaspas.customer.core.notifications.RealtimeNotificationCoordinator
import com.aaspas.customer.core.notifications.RealtimeNotificationStore
import com.aaspas.customer.core.startup.StartupTracer
import com.aaspas.customer.data.remote.AdminApi
import com.aaspas.customer.data.remote.AuthApi
import com.aaspas.customer.data.remote.DetailsApi
import com.aaspas.customer.data.remote.DiscoveryApi
import com.aaspas.customer.data.remote.FavoritesApi
import com.aaspas.customer.data.remote.HomeApi
import com.aaspas.customer.data.remote.NotificationApi
import com.aaspas.customer.data.remote.ShopOwnerApi
import com.aaspas.customer.data.repository.AdminRepositoryImpl
import com.aaspas.customer.data.repository.AuthRepositoryImpl
import com.aaspas.customer.data.repository.CategoryOffersRepositoryImpl
import com.aaspas.customer.data.repository.FavoritesRepositoryImpl
import com.aaspas.customer.data.repository.HomeRepositoryImpl
import com.aaspas.customer.data.repository.NotificationRepositoryImpl
import com.aaspas.customer.data.repository.OfferDetailsRepositoryImpl
import com.aaspas.customer.data.repository.SearchRepositoryImpl
import com.aaspas.customer.data.repository.ShopDetailsRepositoryImpl
import com.aaspas.customer.data.repository.ShopOwnerRepositoryImpl
import com.aaspas.customer.domain.repository.AdminRepository
import com.aaspas.customer.domain.repository.AuthRepository
import com.aaspas.customer.domain.repository.CategoryOffersRepository
import com.aaspas.customer.domain.repository.FavoritesRepository
import com.aaspas.customer.domain.repository.HomeRepository
import com.aaspas.customer.domain.repository.NotificationRepository
import com.aaspas.customer.domain.repository.OfferDetailsRepository
import com.aaspas.customer.domain.repository.SearchRepository
import com.aaspas.customer.domain.repository.ShopDetailsRepository
import com.aaspas.customer.domain.repository.ShopOwnerRepository
import org.osmdroid.config.Configuration
import java.util.concurrent.Executors

class AasPasApplication : Application(), ImageLoaderFactory {

    private val backgroundInitExecutor = Executors.newSingleThreadExecutor()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        StartupTracer.mark("application_onCreate_start")
        NetworkModule.init(this)
        Configuration.getInstance().userAgentValue = packageName
        backgroundInitExecutor.execute {
            Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
            GoogleMapsInitializer.initializeIfConfigured(this)
            StartupTracer.mark("application_background_init_done")
        }
        StartupTracer.mark("application_onCreate_end")
    }

    val selectedLocationStore: SelectedLocationStore by lazy {
        SelectedLocationStore(this)
    }

    val locationSearchService: LocationSearchService by lazy {
        AndroidGeocoderLocationSearchService(this)
    }

    val homeApi: HomeApi by lazy { NetworkModule.publicApi() }

    val detailsApi: DetailsApi by lazy { NetworkModule.publicApi() }

    val discoveryApi: DiscoveryApi by lazy { NetworkModule.publicApi() }

    val authApi: AuthApi by lazy { NetworkModule.api() }

    val favoritesApi: FavoritesApi by lazy { NetworkModule.api() }

    val shopOwnerApi: ShopOwnerApi by lazy { NetworkModule.api() }

    val notificationApi: NotificationApi by lazy { NetworkModule.api() }

    val adminApi: AdminApi by lazy { NetworkModule.api() }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, NetworkModule.session())
    }

    val favoritesRepository: FavoritesRepository by lazy {
        FavoritesRepositoryImpl(favoritesApi)
    }

    val homeRepository: HomeRepository by lazy {
        HomeRepositoryImpl(homeApi)
    }

    val offerDetailsRepository: OfferDetailsRepository by lazy {
        OfferDetailsRepositoryImpl(detailsApi)
    }

    val shopDetailsRepository: ShopDetailsRepository by lazy {
        ShopDetailsRepositoryImpl(detailsApi)
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepositoryImpl(discoveryApi)
    }

    val categoryOffersRepository: CategoryOffersRepository by lazy {
        CategoryOffersRepositoryImpl(discoveryApi)
    }

    val shopOwnerRepository: ShopOwnerRepository by lazy {
        ShopOwnerRepositoryImpl(shopOwnerApi)
    }

    val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(notificationApi)
    }

    val adminRepository: AdminRepository by lazy {
        AdminRepositoryImpl(adminApi)
    }

    val realtimeNotificationStore: RealtimeNotificationStore by lazy {
        RealtimeNotificationStore()
    }

    val realtimeNotificationCoordinator: RealtimeNotificationCoordinator by lazy {
        RealtimeNotificationCoordinator(
            sessionManager = NetworkModule.session(),
            notificationRepository = notificationRepository,
            store = realtimeNotificationStore,
        )
    }
}
