# Advanced settings

Advanced Settings is always available in Waterfox. No debug-menu unlock is required, and developer debug options are not included on this screen.

1. Tap the three-dot menu in the toolbar.
2. Tap Settings.
3. Scroll to the Advanced category and select Advanced Settings.

Some options remain restricted to debug or Nightly builds.

# Overwrite secret settings from `local.properties`

When building locally you can define a specific combination of secret settings you want to enable or disable, without toggling them manually in the app UI every time. Debug builds can force secret settings on startup from the `local.properties` file.

This only takes effect in debug builds. Other build types ignore these entries. Only boolean (on/off) secret settings are supported. Each value must be `true` or `false`.

Add one line per setting, using the `secretSettings.` prefix followed by the setting's [preference key](https://searchfox.org/firefox-main/source/mobile/android/fenix/app/src/main/res/values/preference_keys.xml):

```properties
secretSettings.pref_key_use_scroll_data_for_dynamic_toolbar=true
```

On every rebuild of the app these values are re-applied, so any change you make manually to these particular settings in the app is reset the next time it starts.
