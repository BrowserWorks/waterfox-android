/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.search

import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import mozilla.components.service.location.LocationService
import java.util.Locale

/**
 * A [LocationService] that determines the user's region from the device itself instead of a network
 * geolocation service.
 *
 * Waterfox ships without a Mozilla Location Service token, so the upstream [LocationService.default]
 * always reports the placeholder "XX" region. That prevents the region-specific search defaults in
 * `list.json` (for example Qwant in GB) from ever being applied, leaving every user on the global
 * default. This implementation resolves a real region code, preferring the mobile network country,
 * then the SIM country, and finally the device locale.
 */
class DeviceRegionLocationService(
    private val context: Context,
) : LocationService {

    override suspend fun fetchRegion(readFromCache: Boolean): LocationService.Region? {
        val countryCode = deviceCountryCode() ?: return null
        return LocationService.Region(countryCode, countryCode)
    }

    override fun hasRegionCached(): Boolean = false

    /**
     * Returns the best available ISO 3166-1 alpha-2 country code for the device, upper-cased to
     * match the region keys used in the bundled search configuration, or null if none can be
     * determined.
     */
    private fun deviceCountryCode(): String? {
        val telephony = context.getSystemService<TelephonyManager>()
        val code = telephony?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: telephony?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country.takeIf { it.isNotBlank() }
        return code?.uppercase(Locale.ROOT)
    }
}
