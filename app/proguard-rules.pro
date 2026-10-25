# Keep WebView JS interface bridge members (ScrambleBridge, exposed to JS as
# AndRubikNative): they are only called by name from JavaScript.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
