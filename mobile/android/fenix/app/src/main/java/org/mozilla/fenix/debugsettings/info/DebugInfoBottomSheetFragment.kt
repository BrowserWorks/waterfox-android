/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.debugsettings.info

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.pm.PackageInfoCompat
import androidx.fragment.compose.content
import com.google.android.material.R as materialR
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.utils.ext.packageManagerCompatHelper
import org.mozilla.fenix.debugsettings.info.ui.DebugInfoBottomSheet
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.FirefoxTheme

/** A [BottomSheetDialogFragment] displaying the [DebugInfoBottomSheet]. */
class DebugInfoBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View?>(materialR.id.design_bottom_sheet)
                bottomSheet?.setBackgroundResource(android.R.color.transparent)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = content {
        FirefoxTheme {
            val context = LocalContext.current
            val resources = LocalResources.current
            val provider =
                remember(context, resources) {
                    DebugInfoProvider.create(
                        settings = context.components.settings,
                        versionName = context.getVersionName(),
                        deviceLocale = context.getDeviceLocaleTag(),
                        secretSettingsKeys = getSecretSettingsPreferenceKeys(resources),
                    )
                }
            val sections by
                produceState(initialValue = emptyList(), provider) {
                    value = provider.getDebugInfo()
                }

            DebugInfoBottomSheet(
                sections = sections,
                onDismissRequest = ::dismiss,
            )
        }
    }
}

private fun Context.getVersionName(): String {
    return try {
        val packageInfo =
            packageManagerCompatHelper.getPackageInfoCompat(
                packageName,
                0,
            )
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString()
        String.format(
            "%s (Build #%s)",
            packageInfo.versionName,
            versionCode,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }
}

private fun Context.getDeviceLocaleTag(): String =
    LocaleManager.getCurrentLocale(this)?.toLanguageTag() ?: LocaleManager.getSystemDefault().toLanguageTag()
