/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.settings

import androidx.core.content.edit
import mozilla.components.support.ktx.android.content.PreferencesHolder
import mozilla.components.support.ktx.android.content.booleanPreference
import org.mozilla.fenix.FeatureFlags
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private class DummyProperty : ReadWriteProperty<PreferencesHolder, Boolean> {
    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>) = false
    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: Boolean) = Unit
}

/**
 * Property delegate for getting and setting a boolean shared preference gated by a feature flag.
 *
 * @param key The key for the shared preference.
 * @param defaultValue The default value to return when the preference is unset.
 * @param featureFlag If `true`, the shared preference value is returned; if `false`, this always
 * returns `false`, regardless of the stored value.
 *
 * Note: If you intend to always pass `true` for [featureFlag], consider using [booleanPreference]
 * directly instead, as the feature flag provides no additional behavior in that case.
 *
 * For example, this is **not** recommended:
 * ```
 * val isMyFeatureEnabled by featureFlagBooleanPreference(
 *     …
 *     featureFlag = true,
 *     …
 * )
 * ```
 *
 * [featureFlag] may be controlled through various mechanisms - for example, via an
 * alternative feature-flag system like [FeatureFlags], or internal conditionals.
 *
 * For example, recommended use:
 * ```
 * val isMyFeatureEnabled by featureFlagBooleanPreference(
 *     …
 *     featureFlag = FeatureFlags.ONBOARDING_FEATURE_ENABLED,
 *     …
 * )
 * ```
 */
@Deprecated(
    "Use booleanPreference instead and control the feature flag through Nimbus.",
    ReplaceWith(
        "booleanPreference(key, defaultValue)",
        "mozilla.components.support.ktx.android.content.booleanPreference",
    ),
)
fun featureFlagBooleanPreference(key: String, defaultValue: Boolean, featureFlag: Boolean) =
    if (featureFlag) {
        booleanPreference(key, defaultValue)
    } else {
        DummyProperty()
    }

private class LazyBooleanPreference(val key: String, val defaultValue: () -> Boolean) :
    ReadWriteProperty<PreferencesHolder, Boolean> {

    override fun getValue(thisRef: PreferencesHolder, property: KProperty<*>): Boolean =
        thisRef.preferences.getBoolean(key, defaultValue())

    override fun setValue(thisRef: PreferencesHolder, property: KProperty<*>, value: Boolean) =
        thisRef.preferences.edit { putBoolean(key, value) }
}

/**
 * Property delegate for lazily getting and setting a boolean shared preference gated by a feature flag.
 *
 * @param key The key for the shared preference.
 * @param featureFlag If `true`, the shared preference value is returned; if `false`, this always
 * returns `false`, regardless of the stored value.
 *
 * Note: If you intend to always pass `true` for [featureFlag], consider using [booleanPreference]
 * directly instead, as the feature flag provides no additional behavior in that case.
 *
 * For example, this is **not** recommended:
 * ```
 * val isMyFeatureEnabled by lazyFeatureFlagBooleanPreference(
 *     …
 *     featureFlag = true,
 *     …
 * )
 * ```
 *
 * [featureFlag] may be controlled through various mechanisms - for example, via an
 * alternative feature-flag system like [FeatureFlags], or internal conditionals.
 *
 * For example, recommended use:
 * ```
 * val isMyFeatureEnabled by lazyFeatureFlagBooleanPreference(
 *     …
 *     featureFlag = FeatureFlags.ONBOARDING_FEATURE_ENABLED,
 *     …
 * )
 * ```
 *
 * @param defaultValue The default value to return when the preference is unset.
 */
@Deprecated(
    "Use booleanPreference with a lambda default instead.",
    ReplaceWith(
        "booleanPreference(key, defaultValue)",
        "mozilla.components.support.ktx.android.content.booleanPreference",
    ),
)
fun lazyFeatureFlagBooleanPreference(key: String, featureFlag: Boolean, defaultValue: () -> Boolean) =
    if (featureFlag) {
        LazyBooleanPreference(key, defaultValue)
    } else {
        DummyProperty()
    }

fun hardcodedFeatureFlagBooleanPreference(key: String, featureFlag: Boolean, defaultValue: () -> Boolean) =
    if (featureFlag) {
        LazyBooleanPreference(key, defaultValue)
    } else {
        DummyProperty()
    }
