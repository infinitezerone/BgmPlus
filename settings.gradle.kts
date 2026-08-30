pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.settings") version "9.3.2"
}

android {
    compileSdk = 37
    // Android 12 (API 31)：libcore ≈ Java 11，配合 coreLibraryDesugaring 覆盖剩余缺口
    minSdk = 31
    targetSdk = 37
}

rootProject.name = "BgmPlus"

include(":app")
include(":core:model")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":feature:user")
include(":feature:schedule")
