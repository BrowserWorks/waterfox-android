/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.distributions

import kotlinx.coroutines.runBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.ext.packageManagerWrapper

import org.junit.After
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import kotlin.collections.listOf

@RunWith(RobolectricTestRunner::class)
class DistributionIdManagerTest {


    private var providerValue: String? = null
    private var legacyProviderValue: String? = null
    private var storedId: String? = null
    private var savedId: String = ""

    private val testDistributionProviderChecker = object : DistributionProviderChecker {
        override suspend fun queryProvider(): String? = providerValue
    }

    private val testBrowserStoreProvider = object : DistributionBrowserStoreProvider {
        override fun getDistributionId(): String? = storedId

        override fun updateDistributionId(id: String) {
            storedId = id
        }
    }

    private val testDistributionSettings = object : DistributionSettings {
        override fun getDistributionId(): String = savedId

        override fun saveDistributionId(id: String) {
            savedId = id
        }
    }

    @After
    fun tearDown() {
        providerValue = null
        legacyProviderValue = null
        storedId = null
        savedId = ""
        ShadowBuild.reset()
    }

    @Test
    fun `WHEN a device is made by vivo AND the vivo distribution file is found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                appPreinstalledOnVivoDevice = { true },
            )

            // Mock Build.MANUFACTURER to simulate a Vivo device
            ShadowBuild.setManufacturer("vivo")

            val distributionId = subject.getDistributionId()

            assertEquals("vivo-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.VIVO_001, distribution)
        }

    @Test
    fun `WHEN a device is not made by vivo AND the vivo distribution file is found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                appPreinstalledOnVivoDevice = { true },
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN a device is made by vivo AND the vivo distribution file is not found THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                appPreinstalledOnVivoDevice = { false },
            )

            // Mock Build.MANUFACTURER to simulate a Vivo device
            ShadowBuild.setManufacturer("vivo")

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the device is not vivo AND the channel is not mozilla online THEN the proper id is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is digital_tubrine AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtTelefonicaInstalled = { true },
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_001, distribution)
        }

    @Test
    fun `WHEN the provider is not digital_tubrine AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtTelefonicaInstalled = { true },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is not digital_tubrine AND the DT app is not installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtTelefonicaInstalled = { false },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is null AND the DT app is installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtTelefonicaInstalled = { true },
            )

            providerValue = null
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is null AND the DT app is not installed THEN the proper ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtTelefonicaInstalled = { false },
            )

            providerValue = null
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is aura THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )

            providerValue = "aura"
            val distributionId = subject.getDistributionId()

            assertEquals("aura-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.AURA_001, distribution)
        }

    @Test
    fun `WHEN the provider is DT AND a DT USA package is installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtUsaInstalled = { true },
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-002", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_002, distribution)
        }

    @Test
    fun `WHEN the provider is not DT AND a DT USA package is installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
                isDtUsaInstalled = { true },
            )

            providerValue = "some_provider"
            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }

    @Test
    fun `WHEN the provider is DT and telefonica and USA packages are not installed THEN the proper distribution ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )

            providerValue = "digital_turbine"
            val distributionId = subject.getDistributionId()

            assertEquals("dt-003", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DT_003, distribution)
        }

    @Test
    fun `WHEN there is a saved ID THEN the saved ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )

            testDistributionSettings.saveDistributionId("vivo-001")

            val distributionId = subject.getDistributionId()

            assertEquals("vivo-001", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.VIVO_001, distribution)
        }

    @Test
    fun `WHEN there is not a saved ID THEN a non blank ID is returned`() =
        runBlocking {
            val subject = DistributionIdManager(
                packageManager = testContext.packageManagerWrapper,
                testBrowserStoreProvider,
                distributionProviderChecker = testDistributionProviderChecker,
                distributionSettings = testDistributionSettings,
            )

            val distributionId = subject.getDistributionId()

            assertEquals("Mozilla", distributionId)

            val distribution = subject.getDistribution()

            assertEquals(DistributionIdManager.Distribution.DEFAULT, distribution)
        }
}
