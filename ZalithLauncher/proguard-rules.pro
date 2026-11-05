-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# SDL
-keep class org.libsdl.app.** { *; }

# Launcher
-keep class org.lwjgl.glfw.CallbackBridge {
    *;
}
-keep class com.oracle.dalvik.VMLauncher {
    *;
}

#
## Hilt
#-keep class dagger.hilt.** { *; }
#-keep class javax.inject.** { *; }
#-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
#-keepclasseswithmembers class * {
#    @dagger.hilt.* <methods>;
#}

# Prevent R8 from over-optimizing constructors (causes StackOverflow with Hilt + proguard-android-optimize.txt)
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.internal.GeneratedEntryPoint <init>(...);
}
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *


-keep class com.movtery.zalithlauncher.bridge.** { *; }
-keep class com.movtery.zalithlauncher.utils.device.VulkanChecker {
    *;
}
-keep class com.movtery.zalithlauncher.utils.device.VulkanCapabilities {
    *;
}
-keep interface com.movtery.zalithlauncher.utils.device.VulkanLogCallback {
    *;
}
-keep class com.movtery.zalithlauncher.game.input.CriticalNativeTest {
    *;
}

# Libraries
-keep class com.github.steveice10.opennbt.** { *; }

# SoraEditor language-textmate
-keep class org.jcodings.** { *; }

# PCL Pro Android - 保留核心类不被混淆
-keep class com.movtery.zalithlauncher.components.** { *; }
-keep class com.movtery.zalithlauncher.game.multirt.** { *; }
-keep class com.movtery.zalithlauncher.game.renderer.** { *; }
-keep class com.movtery.zalithlauncher.path.** { *; }
-keep class com.movtery.zalithlauncher.setting.** { *; }
-keep class com.movtery.zalithlauncher.utils.animation.** { *; }

# 保留枚举类（组件、JRE 等）
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 assets 相关
-keep class com.movtery.zalithlauncher.context.** { *; }

# Apache Commons Compress (tar.xz 解压)
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# 保留 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}