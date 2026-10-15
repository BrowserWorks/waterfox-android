/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import java.net.URI
import java.net.URISyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.appservices.fxaclient.FxaServer as NativeFxaServer
import org.mozilla.fenix.R
import org.mozilla.fenix.e2e.SystemInsetsPaddedFragment
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar

/** Settings for custom account and Sync servers. */
class SyncServersFragment : PreferenceFragmentCompat(), SystemInsetsPaddedFragment {
    private lateinit var accountServer: EditTextPreference
    private lateinit var tokenServer: EditTextPreference
    private lateinit var savePreference: Preference
    private var saving = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.sync_server_preferences, rootKey)
        val settings = requireComponents.settings
        accountServer = requirePreference(R.string.pref_key_override_fxa_server)
        tokenServer = requirePreference(R.string.pref_key_override_sync_tokenserver)
        savePreference = requirePreference(R.string.pref_key_save_sync_servers)
        accountServer.text = settings.overrideFxAServer
        tokenServer.text = settings.overrideSyncTokenServer
        savePreference.setOnPreferenceClickListener {
            saveAndRestart()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_sync_servers))
        updateMenu()
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (saving || hasAccount()) return
        if (
            !showCustomEditTextPreferenceDialog(
                preference = preference,
                onSuccess = {
                    (preference as EditTextPreference).let { it.text = it.text.orEmpty().trim() }
                    updateMenu()
                },
                errorMessage = {
                    if (isValidEndpoint(it.trim())) null else R.string.preferences_sync_servers_invalid_url
                },
            )
        ) {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun hasAccount(): Boolean =
        requireComponents.settings.signedInFxaAccount ||
            requireComponents.backgroundServices.accountManager.authenticatedAccount() != null

    private fun updateMenu() {
        val serverConfig = requireComponents.backgroundServices.serverConfig
        val currentAccountUrl = (serverConfig.server as? NativeFxaServer.Custom)?.url.orEmpty()
        val connected = hasAccount()
        val editable = !saving && !connected
        accountServer.isEnabled = editable
        tokenServer.isEnabled = editable
        listOf(accountServer, tokenServer).forEach {
            it.summary = it.text.orEmpty().ifEmpty { getString(R.string.preferences_sync_servers_default) }
        }
        requirePreference<Preference>(R.string.pref_key_sync_servers_info).summary =
            getString(
                if (connected) {
                    R.string.preferences_sync_servers_sign_out
                } else {
                    R.string.preferences_sync_servers_description
                }
            )
        val accountUrl = accountServer.text.orEmpty().trim()
        val tokenUrl = tokenServer.text.orEmpty().trim()
        savePreference.isEnabled =
            editable &&
                isValidEndpoint(accountUrl) &&
                isValidEndpoint(tokenUrl) &&
                (accountUrl != currentAccountUrl || tokenUrl != serverConfig.tokenServerUrlOverride.orEmpty())
    }

    private fun saveAndRestart() {
        if (saving || hasAccount()) {
            updateMenu()
            return
        }
        val accountUrl = accountServer.text.orEmpty().trim()
        val tokenUrl = tokenServer.text.orEmpty().trim()
        if (!isValidEndpoint(accountUrl) || !isValidEndpoint(tokenUrl)) return
        val context = requireContext().applicationContext
        val component = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
        if (component == null) {
            Toast.makeText(context, R.string.preferences_sync_servers_save_failed, Toast.LENGTH_LONG).show()
            return
        }
        val preferences = requireComponents.settings.preferences
        val accountKey = getString(R.string.pref_key_override_fxa_server)
        val tokenKey = getString(R.string.pref_key_override_sync_tokenserver)
        val restartIntent = Intent.makeRestartActivityTask(component)
        saving = true
        updateMenu()
        lifecycleScope.launch {
            withContext(NonCancellable) {
                val saved =
                    withContext(Dispatchers.IO) {
                        preferences.edit().putString(accountKey, accountUrl).putString(tokenKey, tokenUrl).commit()
                    }
                if (saved) {
                    restartFenix(context, restartIntent)
                } else {
                    saving = false
                    if (isAdded) updateMenu()
                    Toast.makeText(context, R.string.preferences_sync_servers_save_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun restartFenix(context: Context, intent: Intent) {
        context.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    private fun isValidEndpoint(value: String): Boolean {
        if (value.isEmpty()) return true
        return try {
            val uri = URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrEmpty() &&
                (uri.port == -1 || uri.port in 1..65535)
        } catch (_: URISyntaxException) {
            false
        }
    }
}
