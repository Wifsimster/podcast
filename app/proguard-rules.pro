# Keep Media3 session classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Hilt / Dagger generated code is handled by their consumer rules.
# Room generated code is handled by its consumer rules.

# Keep model classes (reflection-free, but keep names for clarity)
-keep class com.carne.podcast.data.model.** { *; }
