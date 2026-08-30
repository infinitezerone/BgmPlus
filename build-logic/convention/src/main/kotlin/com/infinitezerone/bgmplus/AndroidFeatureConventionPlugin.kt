package com.infinitezerone.bgmplus

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("bgmplus.android.library")
            apply("bgmplus.android.library.compose")
        }

        dependencies {
            add("implementation", project(":core:model"))
            add("implementation", project(":core:common"))
            add("implementation", project(":core:data"))
            add("implementation", project(":core:designsystem"))

            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("koin-androidx-compose").get())

            add("testImplementation", project(":core:testing"))
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}
