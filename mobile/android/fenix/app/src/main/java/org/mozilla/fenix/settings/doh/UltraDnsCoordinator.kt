/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.doh

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mozilla.components.concept.engine.Engine
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.Settings

/** Selected DNS settings and the VPN policy applied to the running engine. */
data class UltraDnsState(
    val selectedMode: Engine.DohSettingsMode,
    val pauseOnVpn: Boolean,
    val vpnActive: Boolean,
) {
    val isPaused: Boolean
        get() = selectedMode == Engine.DohSettingsMode.ULTRA && pauseOnVpn && vpnActive

    val effectiveMode: Engine.DohSettingsMode
        get() = if (isPaused) Engine.DohSettingsMode.OFF else selectedMode
}

/** Process-lifetime policy owned by Core; never persists a temporary VPN pause as a DNS selection. */
class UltraDnsCoordinator(
    context: Context,
    private val settings: Settings,
) {
    private val connectivityManager = requireNotNull(
        context.applicationContext.getSystemService(ConnectivityManager::class.java),
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val preferenceKeys = setOf(
        context.getString(R.string.pref_key_doh_settings_mode),
        context.getString(R.string.pref_key_doh_use_ohttp),
        context.getString(R.string.pref_key_waterfox_dns_pause_on_vpn),
    )
    private var engine: Engine? = null
    private val mutableState = MutableStateFlow(readState())

    val state: StateFlow<UltraDnsState> = mutableState.asStateFlow()

    private val refresh = Runnable { applySelectedMode() }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in preferenceKeys) {
            scheduleRefresh()
        }
    }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = scheduleRefresh()

        override fun onLost(network: Network) = scheduleRefresh()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) = scheduleRefresh()
    }

    /** Snapshot used for both TRR mode and OHTTP before constructing GeckoEngine. */
    @MainThread
    fun initialMode(): Engine.DohSettingsMode {
        checkMainThread()
        return readState().also { mutableState.value = it }.effectiveMode
    }

    /** Attach once to the process engine and observe the app's default network, including VPNs. */
    @MainThread
    fun start(engine: Engine) {
        checkMainThread()
        if (this.engine != null) {
            check(this.engine === engine)
            return
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback, mainHandler)
        settings.preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        this.engine = engine
        applySelectedMode()
    }

    /** Apply the latest selection through the VPN policy, without changing stored DNS settings. */
    @MainThread
    fun applySelectedMode() {
        checkMainThread()
        val currentState = readState()
        engine?.settings?.let { engineSettings ->
            if (engineSettings.dohSettingsMode != currentState.effectiveMode) {
                engineSettings.dohSettingsMode = currentState.effectiveMode
            }
        }
        mutableState.value = currentState
    }

    /** Persist the user's pause preference and apply it immediately. */
    @MainThread
    fun setPauseOnVpn(enabled: Boolean) {
        checkMainThread()
        settings.pauseUltraDnsOnVpn = enabled
        applySelectedMode()
    }

    private fun scheduleRefresh() {
        // Defer split mode/OHTTP preference writes and re-query instead of replaying stale network events.
        mainHandler.removeCallbacks(refresh)
        mainHandler.post(refresh)
    }

    private fun readState(): UltraDnsState {
        val network = connectivityManager.activeNetwork
        val vpnActive = network?.let {
            connectivityManager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } == true
        return UltraDnsState(
            selectedMode = settings.getDohSettingsMode(),
            pauseOnVpn = settings.pauseUltraDnsOnVpn,
            vpnActive = vpnActive,
        )
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Ultra DNS policy must run on the main thread" }
    }
}
