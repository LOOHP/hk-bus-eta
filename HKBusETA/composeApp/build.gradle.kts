/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.a
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.googleDevToolsKsp)
    alias(libs.plugins.compose.compiler)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    // Optionally specify a different file name containing your secrets.
    // The plugin defaults to "local.properties"
    propertiesFileName = "secrets.properties"

    // A properties file containing default secret values. This file can be
    // checked in version control.
    defaultPropertiesFileName = "local.defaults.properties"

    // Configure which keys should be ignored by the plugin by providing regular expressions.
    // "sdk.dir" is ignored by default.
    ignoreList.add("keyToIgnore") // Ignore the key "keyToIgnore"
    ignoreList.add("sdk.*")       // Ignore all keys matching the regexp "sdk.*"
}

kotlin {
    androidTarget {
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
                freeCompilerArgs.addAll(
                    listOf(
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" + project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics")
                )
                freeCompilerArgs.addAll(
                    listOf(
                        "-P",
                        "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination="  + project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics")
                )
            }
        }
    }
    
    jvm("desktop") {
        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val wasmJsMain by getting

        androidMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.play.services.location)
            implementation(libs.play.services.wearable)
            implementation(libs.maps.compose)
            api(libs.compose.webview.multiplatform)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.concurrent.futures.ktx)
            implementation(libs.androidx.browser)
            implementation(libs.android.pdf.viewer)
            implementation(libs.kotlinx.coroutines.guava)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
        }
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtimeSaveable)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.uuid)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.encoding)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.stately.concurrency)
            implementation(libs.stately.concurrent.collections)
            implementation(libs.colormath)
            implementation(compose.components.resources)
            implementation(libs.reorderable)
            implementation(libs.xmlCore)
            implementation(libs.serialization.xml)
            implementation(libs.material3.window.size.clazz.multiplatform)
            implementation(libs.material.kolor)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        iosMain.dependencies {
            implementation(projects.shared)
            implementation(libs.cupertino)
            implementation(libs.cupertino.native)
            api(libs.compose.multiplatform.lifecycle.tracker)
        }
        desktopMain.dependencies {
            implementation(projects.shared)
            implementation(compose.desktop.currentOs)
            implementation(libs.commons.lang3)
            implementation(libs.kotlinx.coroutines.swing)
            api(libs.compose.webview.multiplatform)
        }
        wasmJsMain.dependencies {
            implementation(projects.shared)
        }
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
    }
}

android {
    namespace = "com.loohp.hkbuseta"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.loohp.hkbuseta"
        minSdk = 26
        targetSdk = 34
        versionCode = 929
        versionName = "2.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "com.loohp.hkbuseta.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HK Bus ETA"
            packageVersion = "2.4.3"

            macOS {
                iconFile.set(project.file("icon.icns"))
            }
            windows {
                perUserInstall = true
                upgradeUuid = "20ce294a-f7d4-484a-87c0-d26d8950ab5e"
                iconFile.set(project.file("icon.ico"))
            }
            linux {
                packageName = "hk-bus-eta"
                appRelease = packageVersion
                iconFile.set(project.file("icon.png"))
            }
        }

        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
            version.set("7.5.0")
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}