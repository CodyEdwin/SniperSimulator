# Dagger Ridge does not currently enable shrinking.
# libGDX loads some classes reflectively; keep the application package if
# minification is enabled later.
-keep class com.snipersim.** { *; }
