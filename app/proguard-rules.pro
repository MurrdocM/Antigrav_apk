-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class android.webkit.** { *; }
-keep class androidx.webkit.** { *; }
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }
-keep class androidx.datastore.** { *; }
-dontwarn com.google.errorprone.annotations.**
