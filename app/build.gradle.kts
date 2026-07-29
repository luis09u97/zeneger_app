plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.seunome.zeneger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.seunome.zeneger"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")

    // Imagem circular
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Supabase
    implementation("io.github.jan-tennert.supabase:storage-kt:2.1.4")
    implementation("io.ktor:ktor-client-android:2.3.7")

// Glide para carregar imagens
    implementation("com.github.bumptech.glide:glide:4.16.0")

// Ucrop para recortar foto
    implementation("com.github.yalantis:ucrop:2.2.8")
    implementation("com.google.firebase:firebase-messaging")

    implementation("com.google.android.material:material:1.11.0")
}