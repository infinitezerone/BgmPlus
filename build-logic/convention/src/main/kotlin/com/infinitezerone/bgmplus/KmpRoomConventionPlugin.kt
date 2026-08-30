package com.infinitezerone.bgmplus

import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class KmpRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("androidx.room3")
            apply("com.google.devtools.ksp")
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        val roomCompiler = libs.findLibrary("androidx-room-compiler").get()

        dependencies {
            add("kspAndroid", roomCompiler)
            // add("kspJvm", roomCompiler) // 桌面端暂未启用
        }
    }
}
