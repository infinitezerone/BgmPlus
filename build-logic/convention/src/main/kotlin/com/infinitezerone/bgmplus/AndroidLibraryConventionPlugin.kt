package com.infinitezerone.bgmplus

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // AGP 9 内置 Kotlin：不再应用 org.jetbrains.kotlin.android，
        // jvmTarget 默认取 compileOptions.targetCompatibility
        with(pluginManager) {
            apply("com.android.library")
        }

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
        }
        configureCoreLibraryDesugaring()
        configureSpotless()
    }
}
