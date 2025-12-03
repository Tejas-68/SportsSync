import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.project.sportssync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sportssync.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Firebase BOM - Use single version
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")  // For profile pictures

    // Apache POI for Excel export
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    
    // QR Code library
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // CardView for better UI
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Glide for image loading (if needed)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Applandeo Material Calendar View
    implementation("com.applandeo:material-calendar-view:1.9.0")
}