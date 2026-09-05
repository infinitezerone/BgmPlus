package com.infinitezerone.minibgm

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureKotlinMultiplatform() {
    extensions.configure<KotlinMultiplatformExtension> {
        // toolchain 25：编译器与字节码目标均为 25（class file v69，AGP 9.3 D8/R8 支持）。
        // java.* API 表面由 minSdk 31 + lint NewApi + coreLibraryDesugaring 兜底
        //（2026-08 决定，见 KotlinAndroid.kt 同版注释）
        jvmToolchain(25)

        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
        // jvm() // 桌面端目标（暂未启用，后续需要时开启）

        applyDefaultHierarchyTemplate()
    }

    // Android target 由 com.android.kotlin.multiplatform.library 注册，无法经
    // androidTarget{} 配置 jvmTarget；在编译任务层统一固定为 25，
    // 避免随 toolchain/运行 JDK 漂移导致字节码目标不一致。
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }
}
