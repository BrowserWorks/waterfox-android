# Waterfox for Android

The Waterfox for Android browser is based on Fenix.

## License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/

## Building

### For Local Development (Feature Development)

For feature development, you can build a local development version quickly:

1. **Download Clang** (similar to CI):
   ```shell
   mkdir -p $HOME/.mozbuild
   curl -L https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/gecko.cache.level-3.toolchains.v3.linux64-clang-20.latest/artifacts/public/build/clang.tar.zst -o clang.tar.zst
   tar -xvf clang.tar.zst -C $HOME/.mozbuild
   ```

2. **Build GeckoView**:
   ```shell
   ./mach build
   ```

3. **Open in Android Studio**:
   - Open Android Studio
   - Select the `fenix` project as the module to debug/release
   - You can now debug and develop features directly in Android Studio

### For Production Builds

To create a production-equivalent build locally (similar to what CI does):

1. **Set up environment variables**:
   ```shell
   export GRADLE_MAVEN_REPOSITORIES="https://maven.google.com/,https://repo.maven.apache.org/maven2/,https://plugins.gradle.org/m2/,https://maven.mozilla.org/maven2/"
   export MOZ_BUILD_DATE="$(date +'%Y%m%d%H0000')"
   ```

2. **Download and set up Clang**:
   ```shell
   mkdir -p $HOME/.mozbuild
   curl -L https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/gecko.cache.level-3.toolchains.v3.linux64-clang-20.latest/artifacts/public/build/clang.tar.zst -o clang.tar.zst
   tar -xvf clang.tar.zst -C $HOME/.mozbuild
   ```

3. **Build GeckoView for multiple architectures**:

   For ARM (armeabi-v7a):
   ```shell
   export MOZCONFIG=".mozconfig-arm-linux-androideabi"
   rustup target add thumbv7neon-linux-androideabi
   ./mach build
   ./mach package
   ./mach gradle :geckoview:assemble
   ```

   For ARM64 (arm64-v8a):
   ```shell
   export MOZCONFIG=".mozconfig-aarch64-linux-android"
   rustup target add aarch64-linux-android
   ./mach build
   ./mach package
   ./mach gradle :geckoview:assemble
   ```

   For x86_64:
   ```shell
   export MOZCONFIG=".mozconfig-x86_64-linux-android"
   rustup target add x86_64-linux-android
   ./mach build
   ./mach package
   ./mach gradle :geckoview:assemble
   ```

4. **Build Fat AAR**:
   ```shell
   export MOZ_ANDROID_FAT_AAR_ARMEABI_V7A="$(pwd)/obj-arm-linux-androideabi/gradle/build/mobile/android/geckoview/outputs/aar/geckoview-release.aar"
   export MOZ_ANDROID_FAT_AAR_ARM64_V8A="$(pwd)/obj-aarch64-linux-android/gradle/build/mobile/android/geckoview/outputs/aar/geckoview-release.aar"
   export MOZ_ANDROID_FAT_AAR_X86_64="$(pwd)/obj-x86_64-linux-android/gradle/build/mobile/android/geckoview/outputs/aar/geckoview-release.aar"
   ./mach build
   ```

5. **Build Release APKs**:
   ```shell
   ./gradlew assemblefenixRelease
   ```

6. **Output Locations**:
   After building, your files will be located at:

   **APK files**:
   ```
   objdir/gradle/build/mobile/android/fenix/app/outputs/apk/fenix/release/
   ```
