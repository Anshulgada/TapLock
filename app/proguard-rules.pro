# Keep all app classes — small app, avoids R8 breaking accessibility service binding.
-keep class com.taplock.app.** { *; }

# Accessibility services must keep their lifecycle methods intact.
-keep class * extends android.accessibilityservice.AccessibilityService {
    public <init>();
    public void onServiceConnected();
    public void onDestroy();
    public boolean performGlobalAction(int);
}

# Keep Kotlin companion objects (holds the running service instance).
-keepclassmembers class com.taplock.app.** {
    public static ** Companion;
}
-keep class com.taplock.app.**$Companion { *; }
