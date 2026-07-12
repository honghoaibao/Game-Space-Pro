# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
