# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# Firebase
# ========================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firestore
-keep class com.google.firebase.firestore.** { *; }
-keepclassmembers class com.google.firebase.firestore.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.iid.** { *; }

# Keep all model classes for Firestore
-keep class com.project.sportssync.BorrowRequest { *; }
-keep class com.project.sportssync.BorrowRequest$BorrowedEquipment { *; }
-keep class com.project.sportssync.RequestModel { *; }
-keep class com.project.sportssync.SportModel { *; }
-keep class com.project.sportssync.SportModel$EquipmentItem { *; }
-keep class com.project.sportssync.NotificationModel { *; }

# Keep all fields in model classes
-keepclassmembers class com.project.sportssync.** {
    <fields>;
}

# ========================================
# Glide
# ========================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ========================================
# Apache POI (Excel export)
# ========================================
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.schemas.**
-dontwarn javax.xml.stream.**

# Ignore Java AWT classes (desktop-only, not available on Android)
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn com.graphbuilder.**


# Keep POI classes
-keep class org.apache.poi.ss.usermodel.** { *; }
-keep class org.apache.poi.xssf.usermodel.** { *; }

# ========================================
# ZXing (QR Code)
# ========================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ========================================
# AndroidX
# ========================================
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# RecyclerView
-keep class androidx.recyclerview.widget.** { *; }

# CardView
-keep class androidx.cardview.widget.** { *; }

# SwipeRefreshLayout
-keep class androidx.swiperefreshlayout.widget.** { *; }

# ========================================
# Material Components
# ========================================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# ========================================
# Kotlin (if used by dependencies)
# ========================================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# ========================================
# General Android
# ========================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable implementations
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# App-specific classes
# ========================================
# Keep all activities
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Activity

# Keep all adapters
-keep class * extends androidx.recyclerview.widget.RecyclerView$Adapter {
    <init>(...);
    public <methods>;
}

# Keep SessionManager
-keep class com.project.sportssync.SessionManager { *; }

# Keep ValidationUtils
-keep class com.project.sportssync.ValidationUtils { *; }

# Keep NotificationHelper
-keep class com.project.sportssync.NotificationHelper { *; }

# Keep ImageUtils
-keep class com.project.sportssync.ImageUtils { *; }

# Keep MyFirebaseMessagingService
-keep class com.project.sportssync.MyFirebaseMessagingService { *; }

# ========================================
# Debugging
# ========================================
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}