# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in proguard-android-optimize.txt

# Keep application entry points / Android components
-keep class com.strobingn.bowtune.BowTuneApp { *; }
-keep class com.strobingn.bowtune.MainActivity { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class com.strobingn.bowtune.data.** { *; }
-dontwarn androidx.room.paging.**

# ML Kit pose (base)
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_pose_blaze.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Kotlin / coroutines
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose / Material icons reflection helpers are covered by consumer rules;
# keep Parcelable/Serializable data models used across process boundaries.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
