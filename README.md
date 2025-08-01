# Waterfox for Android

The Waterfox for Android browser is based on Fenix.

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

## Building

### For Local Development (Feature Development)

For feature development, you can build a local development version quickly:

1. **Build GeckoView**:
   ```shell
   ./mach build
   ```

2. **Open in Android Studio**:
   - Open Android Studio
   - Select the `fenix` project as the module to debug/release
   - Select `waterfoxDebug` as the build variant
   - You can now debug and develop features directly in Android Studio

### For Production Builds

To create a production-equivalent build locally (similar to what CI does):

1. **Initialise submodules and set up environment variables**:
   ```shell
   git submodule update --init --recursive
   export GRADLE_MAVEN_REPOSITORIES="https://maven.google.com/,https://repo.maven.apache.org/maven2/,https://plugins.gradle.org/m2/,https://maven.mozilla.org/maven2/"
   export MOZ_BUILD_DATE="$(date +'%Y%m%d%H0000')"
   ```

2. **Build GeckoView for multiple architectures**:

   For ARM (armeabi-v7a):
   ```shell
   export MOZCONFIG=".mozconfig-arm-linux-androideabi"
   rustup target add thumbv7neon-linux-androideabi
   ./mach build
   export GRADLE_INVOKED_WITHIN_MACH_BUILD=1
   ./mach gradle :geckoview:assembleRelease
   ./mach gradle :geckoview:publishReleasePublicationToMavenRepository
   python3 scripts/create-maven-zip.py --objdir "obj-arm-linux-androideabi" --include-snapshots --require-aar
   ```

   For ARM64 (arm64-v8a):
   ```shell
   export MOZCONFIG=".mozconfig-aarch64-linux-android"
   rustup target add aarch64-linux-android
   ./mach build
   export GRADLE_INVOKED_WITHIN_MACH_BUILD=1
   ./mach gradle :geckoview:assembleRelease
   ./mach gradle :geckoview:publishReleasePublicationToMavenRepository
   python3 scripts/create-maven-zip.py --objdir "obj-aarch64-linux-android" --include-snapshots --require-aar
   ```

   For x86_64:
   ```shell
   export MOZCONFIG=".mozconfig-x86_64-linux-android"
   rustup target add x86_64-linux-android
   ./mach build
   export GRADLE_INVOKED_WITHIN_MACH_BUILD=1
   ./mach gradle :geckoview:assembleRelease
   ./mach gradle :geckoview:publishReleasePublicationToMavenRepository
   python3 scripts/create-maven-zip.py --objdir "obj-x86_64-linux-android" --include-snapshots --require-aar
   ```

3. **Build Fat AAR**:
   ```shell
   export MOZ_ANDROID_FAT_AAR_ARMEABI_V7A="$(pwd)/obj-arm-linux-androideabi/gradle/target.maven.zip"
   export MOZ_ANDROID_FAT_AAR_ARM64_V8A="$(pwd)/obj-aarch64-linux-android/gradle/target.maven.zip"
   export MOZ_ANDROID_FAT_AAR_X86_64="$(pwd)/obj-x86_64-linux-android/gradle/target.maven.zip"
   ./mach build
   ```

4. **Build Release APKs**:
   ```shell
   ./gradlew assembleWaterfoxRelease
   ```

5. **Output Locations**:
   After building, your files will be located at:

   **APK files**:
   ```
   objdir/gradle/build/mobile/android/fenix/app/outputs/apk/waterfox/release/
   ```
