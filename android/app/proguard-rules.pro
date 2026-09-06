# Keep JavaScript interfaces and WebView client callbacks intact.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.splitview.pad.** { *; }
