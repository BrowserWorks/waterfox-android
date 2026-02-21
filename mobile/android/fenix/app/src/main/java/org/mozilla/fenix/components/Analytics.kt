/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import mozilla.components.lib.crash.CrashReporter
import mozilla.components.lib.crash.runtimetagproviders.BuildRuntimeTagProvider
import mozilla.components.lib.crash.runtimetagproviders.EnvironmentRuntimeProvider
import mozilla.components.lib.crash.runtimetagproviders.ExperimentDataRuntimeTagProvider
import mozilla.components.lib.crash.runtimetagproviders.VersionInfoProvider
import mozilla.components.support.utils.Browsers
import mozilla.components.support.utils.ext.packageManagerCompatHelper
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.metrics.DefaultMetricsStorage
import org.mozilla.fenix.components.metrics.FirstSessionMetricsService
import org.mozilla.fenix.components.metrics.GleanMetricsService
import org.mozilla.fenix.components.metrics.GleanProfileIdPreferenceStore
import org.mozilla.fenix.components.metrics.GleanUsageReportingMetricsService
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.components.metrics.MetricsStorage
import org.mozilla.fenix.crashes.CrashFactCollector
import org.mozilla.fenix.crashes.NimbusExperimentDataProvider
import org.mozilla.fenix.crashes.ReleaseRuntimeTagProvider
import org.mozilla.fenix.perf.lazyMonitored
import org.mozilla.fenix.utils.Settings

/** Component group for all functionality related to analytics e.g. crash reporting and telemetry. */
class Analytics(
    private val context: Context,
    private val settings: Settings,
    private val nimbusComponents: NimbusComponents,
) {
    val crashReporter: CrashReporter by lazyMonitored {
        val intent =
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val crashReportingIntentFlags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0 // No flags. Default behavior.
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                crashReportingIntentFlags,
            )

        CrashReporter(
            context = context,
            services = emptyList(),
            telemetryServices = emptyList(),
            shouldPrompt = CrashReporter.Prompt.NEVER,
            enabled = true,
            nonFatalCrashIntent = pendingIntent,
            useLegacyReporting = false,
            runtimeTagProviders =
                listOf(
                    ReleaseRuntimeTagProvider(),
                    BuildRuntimeTagProvider(context.versionInfoProvider),
                    EnvironmentRuntimeProvider(),
                    ExperimentDataRuntimeTagProvider(
                        NimbusExperimentDataProvider(getEnrollments = nimbusComponents::getEnrollmentsForCrashReporter)
                    ),
                ),
        )
    }

    val crashFactCollector: CrashFactCollector by lazyMonitored {
        CrashFactCollector(crashReporter)
    }

    val metricsStorage: MetricsStorage by lazyMonitored {
        DefaultMetricsStorage(
            context = context,
            settings = settings,
            checkDefaultBrowser = { Browsers.isDefaultBrowser(context) },
        )
    }

    val metrics: MetricController by lazyMonitored {
        MetricController.create(
            listOf(
                GleanMetricsService(context),
                FirstSessionMetricsService(context),
                GleanUsageReportingMetricsService(gleanProfileIdStore = GleanProfileIdPreferenceStore(context)),
            ),
            isDataTelemetryEnabled = { settings.isTelemetryEnabled },
            isMarketingDataTelemetryEnabled = {
                settings.isMarketingTelemetryEnabled && settings.hasMadeMarketingTelemetrySelection
            },
            isUsageTelemetryEnabled = { settings.isDailyUsagePingEnabled },
            settings,
        )
    }
}

private val Context.versionInfoProvider: VersionInfoProvider
    get() {
        val packageInfo =
            applicationContext.packageManagerCompatHelper.getPackageInfoCompat(
                applicationContext.packageName,
                0,
            )
        return VersionInfoProvider.fromPackageInfo(packageInfo)
    }
