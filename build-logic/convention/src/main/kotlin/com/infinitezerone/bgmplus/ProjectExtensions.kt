package com.infinitezerone.bgmplus

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.androidLibrary(configure: LibraryExtension.() -> Unit) {
    extensions.configure<LibraryExtension>(configure)
}

/** AGP KMP 库模块的 Android 配置入口（namespace 等），经 finalizeDsl 延迟到 variant 创建前执行 */
fun Project.kmpAndroidLibrary(configure: KotlinMultiplatformAndroidLibraryExtension.() -> Unit) {
    extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
        finalizeDsl(configure)
    }
}

fun Project.androidApplication(configure: ApplicationExtension.() -> Unit) {
    extensions.configure<ApplicationExtension>(configure)
}
