plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.efa.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.efa.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 极致缩减包体积：只保留中文资源，移除三方依赖中自带的多国语言文件
        resourceConfigurations.add("zh")
    }

    buildTypes {
        release {
            // 启用代码压缩、混淆和 R8 优化
            isMinifyEnabled = true
            // 启用资源缩减（需与 isMinifyEnabled 一同开启）
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX & Compose 核心依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime-ktx)
    implementation(libs.androidx.activity-compose)
    
    // Compose BOM & UI
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.lifecycle.viewmodel)

    // Hilt 依赖注入
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation-compose)

    // WorkManager 后台任务
    implementation(libs.androidx.work-runtime-ktx)

    // 依赖的所有子模块
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:ai"))

    implementation(project(":feature:home"))
    implementation(project(":feature:focus"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:settings"))

    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso-core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test-junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test-manifest)
}
