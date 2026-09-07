-keep class ru.gdesanek.** { *; }
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
}
-dontwarn kotlin.**
-dontwarn org.jetbrains.**
