package fi.mazhar.salat.platform

import android.location.LocationManager
import android.os.Build
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidLocationServiceTest {

    private val allEnabled: (String) -> Boolean = { true }

    @Test
    fun finePermissionTriesEveryEnabledProviderBestFirst() {
        assertEquals(
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            AndroidLocationService.usableProviders(fineGranted = true, sdkInt = Build.VERSION_CODES.N, enabled = allEnabled),
        )
    }

    @Test
    fun approximateOnlyBeforeAndroid12SkipsProvidersThatRequireFineLocation() {
        assertEquals(
            listOf(LocationManager.NETWORK_PROVIDER),
            AndroidLocationService.usableProviders(fineGranted = false, sdkInt = Build.VERSION_CODES.R, enabled = allEnabled),
        )
    }

    @Test
    fun approximateOnlyOnAndroid12UsesEveryProvider() {
        assertEquals(
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER),
            AndroidLocationService.usableProviders(fineGranted = false, sdkInt = Build.VERSION_CODES.S, enabled = allEnabled),
        )
    }

    @Test
    fun disabledProvidersAreDropped() {
        val enabled: (String) -> Boolean = { it == LocationManager.NETWORK_PROVIDER }
        assertEquals(
            listOf(LocationManager.NETWORK_PROVIDER),
            AndroidLocationService.usableProviders(fineGranted = true, sdkInt = Build.VERSION_CODES.TIRAMISU, enabled = enabled),
        )
    }

    @Test
    fun nothingUsableWhenEveryProviderIsOff() {
        assertEquals(
            emptyList(),
            AndroidLocationService.usableProviders(fineGranted = true, sdkInt = Build.VERSION_CODES.S, enabled = { false }),
        )
    }
}
