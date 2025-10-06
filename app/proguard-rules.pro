# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep ZXing classes
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Keep security classes
-keep class androidx.security.crypto.** { *; }

# Keep app classes
-keep class com.project.sportssync.** { *; }

# Remove debug info
-renamesourcefileattribute SourceFile