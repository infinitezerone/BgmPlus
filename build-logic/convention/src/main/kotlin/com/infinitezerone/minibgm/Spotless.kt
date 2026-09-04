package com.infinitezerone.minibgm

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotless() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint(libs.findVersion("ktlint").get().requiredVersion)
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.kts")
            targetExclude("**/build/**")
            ktlint(libs.findVersion("ktlint").get().requiredVersion)
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
