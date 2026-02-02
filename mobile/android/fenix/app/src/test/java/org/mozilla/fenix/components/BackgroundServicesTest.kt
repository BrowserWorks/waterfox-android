/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.support.base.observer.ObserverRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackgroundServicesTest {

    @MockK
    private lateinit var settings: Settings

    private lateinit var registry: ObserverRegistry<AccountObserver>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { settings.signedInFxaAccount = any() } just Runs

        registry = ObserverRegistry<AccountObserver>().apply {
            register(TelemetryAccountObserver(settings))
        }
    }

    @Test
    fun `account observer updates signed-in state on sign in`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.Signin) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state on sign up`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.Signup) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state on pairing`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.Pairing) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state on recovery`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.Recovered) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state for external auth without action`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.OtherExternal(null)) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state for external auth with action`() {
        registry.notifyObservers { onAuthenticated(mockk<OAuthAccount>(), AuthType.OtherExternal("someAction")) }

        verify { settings.signedInFxaAccount = true }
        confirmVerified(settings)
    }

    @Test
    fun `account observer updates signed-in state on logout`() {
        registry.notifyObservers { onLoggedOut() }

        verify { settings.signedInFxaAccount = false }
        confirmVerified(settings)
    }
}
