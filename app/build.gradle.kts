plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment.ktx)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

            // Ktor engine for Supabase
            implementation(libs.ktor.client.okhttp)
            // SLF4J binding required for minified release builds (R8).
            implementation(libs.slf4j.nop)
        }
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.multiplatform.settings)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

            // Supabase (Auth + PostgREST)
            implementation(platform("io.github.jan-tennert.supabase:bom:${libs.versions.supabase.get()}"))
            implementation(libs.supabase.kt)
            implementation(libs.supabase.gotrue)
            implementation(libs.supabase.postgrest)
            implementation(libs.kotlinx.serialization.json)
            
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

            // Ktor engine for Supabase
            implementation(libs.ktor.client.java)
        }
    }
}

android {
    namespace = "com.lsj.notes"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lsj.notes"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

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
        isCoreLibraryDesugaringEnabled = true
    }
}

compose.desktop {
    application {
        mainClass = "com.lsj.notes.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )
            packageName = "Notes"
            packageVersion = "1.2.1"
            includeAllModules = true
            vendor = "Lovedeep0-bit"
            description = "A notes application"
            
            windows {
                shortcut = true
                dirChooser = true
                menuGroup = "Notes"
                perUserInstall = false
                upgradeUuid = "ced1ea4f-8dc8-430e-b7e3-e9fe5cb4b369"
                iconFile.set(project.file("src/desktopMain/resources/app-icon.ico"))
            }
            
            appResourcesRootDir.set(project.layout.projectDirectory.dir("src/desktopMain/resources"))
        }
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)

    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    // Provide versions for androidx.compose.* artifacts declared without versions in libs.versions.toml
    add("implementation", platform(libs.androidx.compose.bom))
    add("androidTestImplementation", platform(libs.androidx.compose.bom))
    add("debugImplementation", platform(libs.androidx.compose.bom))

    // Provide versions for Supabase modules
    add("implementation", platform(libs.supabase.bom))
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
