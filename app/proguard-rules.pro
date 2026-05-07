# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.AppGlideModule { *; }
