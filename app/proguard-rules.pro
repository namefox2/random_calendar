# R8 최적화 비활성화 (코드 최적화로 인한 런타임 오류 방지)
-dontoptimize

# 앱 코드 전체 보존 (이름 변경/제거 방지)
-keep class com.letsgo.randomcalendar.** { *; }

# 어노테이션 및 시그니처 보존 (Kotlin reflection, Room 동작에 필요)
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** { volatile <fields>; }

# Room - 엔티티 및 DAO
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }
# Room KSP 생성 클래스 (_Impl) 보존
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }

# ViewBinding
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(android.view.View);
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.AppGlideModule { *; }
