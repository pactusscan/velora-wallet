# Trust Wallet Core JNI
-keep class wallet.core.jni.** { *; }
-keep class wallet.core.java.** { *; }

# Retrofit / Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.andrutstudio.velora.data.rpc.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.InstallIn class *

# Glance AppWidget
-keep class androidx.glance.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keepclassmembers class * extends androidx.glance.appwidget.GlanceAppWidget {
    public <init>();
}

# WebView JavaScript bridge — keep @JavascriptInterface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Compose — keep generated code intact
-keep class androidx.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Kotlin coroutines / serialization
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore Preferences (used by Glance widget state)
-keep class androidx.datastore.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Prevent stripping of enum entries used in domain model
-keepclassmembers enum com.andrutstudio.velora.domain.model.** { *; }

# Source file / line number info for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
