# Keep Media3 session classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Hilt / Dagger generated code is handled by their consumer rules.
# Room generated code is handled by its consumer rules.

# Strip verbose/debug/info logging from release builds so feed URLs, media-
# browser caller identity and browse activity never reach logcat. Genuine
# warnings/errors (Log.w / Log.e) are kept.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
