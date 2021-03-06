# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jay/Library/Developer/Android/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#
#-keepattributes **
#
#-dontskipnonpubliclibraryclassmembers
#
## EventBus 3.0
#-keepclassmembers class ** {
#    public void on*Event*(**);
#    public void onMIDINameChange(**);
#}
#
## EventBus 3.0 annotation
#-keepclassmembers class * {
#    @org.greenrobot.event.Subscribe <methods>;
#}
#-keep enum org.greenrobot.event.ThreadMode { *; }
#
## Only required if you use AsyncExecutor
#-keepclassmembers class * extends org.greenrobot.event.util.ThrowableFailureEvent {
#    <init>(java.lang.Throwable);
#}

# https://android-arsenal.com/details/1/3482
-keep class net.rehacktive.waspdb.** { *; }
-keep class com.esotericsoftware.kryo.** { *; }
