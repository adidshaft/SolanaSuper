
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
}

import java.util.Properties

android {
    namespace = "com.solanasuper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.solanasuper"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        externalNativeBuild {
            cmake {
                arguments("-DANDROID_LD=lld", "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384")
            }
        }

        // Secure QuickNode & IPFS Configuration
        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProperties.load(it) }
        }

        val quickNodeRpc = localProperties.getProperty("QUICKNODE_SOLANA_RPC") ?: "https://api.devnet.solana.com"
        val quickNodeIpfs = localProperties.getProperty("QUICKNODE_IPFS_URL") ?: "https://ipfs.io"

        buildConfigField("String", "QUICKNODE_SOLANA_RPC", "\"$quickNodeRpc\"")
        buildConfigField("String", "QUICKNODE_IPFS_URL", "\"$quickNodeIpfs\"")
    }

    ndkVersion = "28.0.12433566"

    // Define NDK/External Native Build (Moved out of defaultConfig)
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.systemProperty("java.library.path", file("src/main/cpp/rust/target/debug").absolutePath)
        }
    }
}

// Dynamic NDK Linker Resolution for Rust
afterEvaluate {
    // Access Android extension safely
    val androidExt = extensions.getByName("android") as com.android.build.gradle.internal.dsl.BaseAppModuleExtension
    val ndkDir = androidExt.ndkDirectory
    
    val osName = System.getProperty("os.name").lowercase()
    val hostOs = when {
        osName.contains("mac") -> "darwin-x86_64"
        osName.contains("windows") -> "windows-x86_64"
        else -> "linux-x86_64"
    }
    
    val toolchainDir = file("$ndkDir/toolchains/llvm/prebuilt/$hostOs/bin")
    
    // API Level 26 matches minSdk
    val aarch64Linker = toolchainDir.resolve("aarch64-linux-android26-clang").absolutePath
    val armv7Linker = toolchainDir.resolve("armv7a-linux-androideabi26-clang").absolutePath
    val x86Linker = toolchainDir.resolve("i686-linux-android26-clang").absolutePath
    val x86_64Linker = toolchainDir.resolve("x86_64-linux-android26-clang").absolutePath
    
    androidExt.defaultConfig.externalNativeBuild.cmake.arguments.apply {
        add("-DRUST_LINKER_AARCH64=$aarch64Linker")
        add("-DRUST_LINKER_ARMV7=$armv7Linker")
        add("-DRUST_LINKER_I686=$x86Linker")
        add("-DRUST_LINKER_X86_64=$x86_64Linker")
    }
}

// Custom Task to build Rust
tasks.register<Exec>("cargoBuild") {
    // In a real scenario, this would loop over ABIs.
    // simplified: just build valid target if possible or skip if no cargo
    commandLine("echo", "Skipping actual cargo build in simulated environment.")
    // Real command: commandLine "cargo", "build", "--target", "aarch64-linux-android", "--release"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // ... rest (dependencies block starts later?)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Room & SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    
    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    // Nearby - Fixed accessor
    implementation(libs.play.services.nearby)

    // CameraX & MLKit (QR Scanning)
    val camerax_version = "1.3.0-alpha04" // Use stable or alpha as needed
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Protobuf
    implementation(libs.protobuf.java)

    // Solana (Manual)
    implementation(libs.eddsa)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    
    // Testing
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.mockito.android)
}

// Protobuf
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
