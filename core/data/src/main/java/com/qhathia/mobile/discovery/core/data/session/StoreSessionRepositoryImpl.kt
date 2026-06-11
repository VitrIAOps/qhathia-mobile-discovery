package com.qhathia.mobile.discovery.core.data.session

import com.qhathia.mobile.discovery.core.domain.model.StoreSession
import com.qhathia.mobile.discovery.core.domain.repository.StoreSessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [StoreSessionRepository] backed by [StoreSessionLocalDataSource] (Preferences DataStore).
 * A future device-pairing / switch-store flow can write into the same data source.
 */
class StoreSessionRepositoryImpl @Inject constructor(
    private val localDataSource: StoreSessionLocalDataSource,
) : StoreSessionRepository {

    override fun observe(): Flow<StoreSession> = localDataSource.observe()

    override suspend fun updateStoreName(storeName: String?) =
        localDataSource.updateStoreName(storeName)

    override suspend fun deviceAccessToken(): String? = localDataSource.deviceAccessToken()

    override suspend fun saveDeviceSession(deviceAccessToken: String, deviceName: String?) =
        localDataSource.saveDeviceSession(deviceAccessToken, deviceName)

    override suspend fun clearDeviceSession() = localDataSource.clearDeviceSession()
}
