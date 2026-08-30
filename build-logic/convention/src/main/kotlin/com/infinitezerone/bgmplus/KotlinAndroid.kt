package com.infinitezerone.bgmplus

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureKotlinAndroid(
    extension: ApplicationExtension,
) {
    extension.apply {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
            isCoreLibraryDesugaringEnabled = true
        }
    }
}

internal fun Project.configureKotlinAndroid(
    extension: LibraryExtension,
) {
    extension.apply {
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
            isCoreLibraryDesugaringEnabled = true
        }
    }
}

/** 配合 isCoreLibraryDesugaringEnabled，统一从 version catalog (libs.versions.toml) 添加 desugar_jdk_libs */
internal fun Project.configureCoreLibraryDesugaring() {
    dependencies.add("coreLibraryDesugaring", libs.findLibrary("android-desugar-jdk-libs").get())
}
