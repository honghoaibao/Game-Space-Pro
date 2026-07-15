plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "com.gamespace"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gamespace"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // TỐI ƯU DUNG LƯỢNG: UI app 100% tiếng Việt (không có values-xx/ nào khác trong
        // res/ của chính app) — nhưng các thư viện AndroidX/Google (Compose, Room permission
        // rationale, v.v.) đóng gói sẵn string đã dịch cho hàng chục ngôn ngữ khác mà app
        // không dùng tới. resConfigs giữ lại bản dịch "vi" + bản mặc định (values/ không có
        // qualifier, thường là tiếng Anh — luôn được giữ làm fallback), loại các bản dịch
        // ngôn ngữ khác khỏi APK.
        resConfigs("vi")
    }

    // Trước đây KHÔNG khai báo signingConfigs.debug, nên mỗi máy (và mỗi lần chạy CI, vì runner
    // là máy ảo dùng 1 lần) tự sinh ra ~/.android/debug.keystore RIÊNG — hai APK debug build ở hai
    // nơi khác nhau có chữ ký khác nhau. Cài đè APK build mới lên APK debug đã cài trước đó (build
    // từ máy/run khác) bị Android chặn với lỗi "xung đột với gói hiện có" (INSTALL_FAILED_UPDATE_INCOMPATIBLE)
    // dù APK mới vẫn được ký hợp lệ — chỉ là ký bằng key khác. Cố định 1 keystore debug commit vào
    // repo (keystore/debug.keystore, không nhạy cảm — đây là quy ước chuẩn cho debug key dùng chung
    // trong team) để local và CI luôn ký ra cùng 1 chữ ký, update không còn xung đột.
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // TỐI ƯU DUNG LƯỢNG: shrinkResources chỉ có tác dụng khi đi kèm minifyEnabled (đã
            // bật sẵn) — R8 dò code không dùng tới resource nào (drawable/layout/string) rồi
            // loại khỏi APK release, không ảnh hưởng build debug.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // material-icons-extended ĐÃ GỠ (Phiên 11) — Google khuyến nghị không include trực tiếp
    // trong app production vì rất nặng (chứa toàn bộ ~2000+ icon dù app chỉ dùng ~40 cái).
    // material-icons-core (bộ icon cơ bản — chứa Icons/Icons.Filled/Icons.Outlined + vài trăm
    // icon thường dùng) đã tự có sẵn qua material3 ở trên, không cần khai báo thêm. ~40 icon
    // "extended" mà app thực sự dùng đã build tay từ path data GỐC (lấy từ
    // google/material-design-icons — cùng nguồn dữ liệu mà material-icons-extended dùng để
    // sinh code) — xem `ui/icons/filled/GsFilledIcons.kt` + `ui/icons/outlined/GsOutlinedIcons.kt`.
    // Cách dùng ở nơi gọi (`Icons.Filled.TenIcon`) không đổi, chỉ đổi import.
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore (Recovery / settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // WorkManager (dọn nền định kỳ, cảnh báo thermal)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    // Phiên 1 là scaffold ban đầu — không chặn CI vì lỗi style, sẽ siết lại khi code ổn định (xem TASK_BACKLOG.md).
    ignoreFailures.set(true)
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = true
}
