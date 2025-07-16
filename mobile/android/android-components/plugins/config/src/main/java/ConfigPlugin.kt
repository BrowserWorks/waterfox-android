/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.extra
import org.gradle.process.ExecOutput
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class ConfigPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit
}

object Config {

    @JvmStatic
    private fun generateDebugVersionName(): String {
        val today = if (System.getenv("MOZ_BUILD_DATE") != null) {
            val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            format.parse(System.getenv("MOZ_BUILD_DATE"))
        } else {
            Date()
        }
        // Append the year (2 digits) and week in year (2 digits). This will make it easier to distinguish versions and
        // identify ancient versions when debugging issues. However this will still keep the same version number during
        // the week so that we do not end up with a lot of versions in tools like Sentry. As an extra this matches the
        // sections we use in the changelog (weeks).
        return SimpleDateFormat("1.0.yyww", Locale.US).format(today)
    }

    @JvmStatic
    fun releaseVersionName(project: Project): String? {
        // Note: release builds must have the `versionName` set. However, the gradle ecosystem makes this hard to
        // ergonomically validate (sometimes IDEs default to a release variant and mysteriously fail due to the
        // validation, sometimes devs just need a release build and specifying project properties is annoying in IDEs),
        // so instead we'll allow the `versionName` to silently default to an empty string.
        return if (project.hasProperty("versionName")) project.property("versionName").toString() else null
    }

    @JvmStatic
    fun nightlyVersionName(project: Project): String {
        // Nightly versions will use the version from "version.txt".
        return readVersionFromFile(project)
    }

    @JvmStatic
    fun readVersionFromFile(project: Project): String {
        var mozconfig = project.gradle.extensions.extraProperties.get("mozconfig") as Map<*, *>;
        var topsrcdir = mozconfig.get("topsrcdir") as String;
        var versionPath = Paths.get(topsrcdir, "mobile/android/version.txt");
        return project.file(versionPath).useLines { it.firstOrNull() ?: "" }
    }

    @JvmStatic
    fun majorVersion(project: Project): String {
        return readVersionFromFile(project).split(".")[0]
    }

    /**
     * Return the version code that consists of the current year and minutes since
     * the start of the year. E.g. 2024-09-13 8:49 -> 2024369049
     */
    @JvmStatic
    fun generateFennecVersionCode(abi: String): Int {
        val now = if (System.getenv("MOZ_BUILD_DATE") != null) {
            val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
            val buildDate = format.parse(System.getenv("MOZ_BUILD_DATE"))
            OffsetDateTime.ofInstant(buildDate.toInstant(), ZoneOffset.UTC)
        } else {
            OffsetDateTime.now(ZoneOffset.UTC)
        }
        val year = Year.of(now.year)
        val startOfYear = OffsetDateTime.of(year.atDay(1), LocalTime.MIN, ZoneOffset.UTC)
        val minutes = Duration.between(startOfYear, now).toMinutes()
        return (year.value * 1000000 + minutes).toInt()
    }


}
