# Shizuku (newProcess dipanggil via reflection di LogcatEngine)
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**

# Stack trace crash rilis tetap bisa di-retrace (mapping.txt = artifact LogLynx-mapping di CI)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
