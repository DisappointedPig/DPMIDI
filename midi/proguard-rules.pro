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


-keepattributes **

-dontskipnonpubliclibraryclassmembers

# EventBus 3.0
-keepclassmembers class ** {
    public void on*Event*(**);
    public void onMIDINameChange(**);
}

# EventBus 3.0 annotation
-keepclassmembers class * {
    @org.greenrobot.event.Subscribe <methods>;
}
-keep enum org.greenrobot.event.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.event.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}

#-keepclassmembers class ** {
#    public void MIDI*Event(**);
#}

#-keep public class com.disappointedpig.midi.MIDISession { *; }

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
#-dontwarn our.company.project.R*
#-injars bin/project.jar
#-verbose
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-printmapping mapping.txt
-verbose
-dontoptimize
-dontpreverify
-dontshrink
-dontskipnonpubliclibraryclassmembers
-dontusemixedcaseclassnames
-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep public class * {
    public protected *;
}

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# https://android-arsenal.com/details/1/3482
-keep class net.rehacktive.waspdb.** { *; }
-keep class com.esotericsoftware.kryo.** { *; }
-keep class com.disappointedpig.midi.MIDIEventBusIndex

# EventBus 3.0 --------------------------
-keepclassmembers class ** {
    public void on*Event*(**);
    public void onMIDINameChange(**);
}

# EventBus 3.0 annotation
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Only required if you use AsyncExecutor
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
# --------------------------
