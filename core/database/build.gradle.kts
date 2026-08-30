import com.infinitezerone.bgmplus.kmpAndroidLibrary

plugins {
    alias(libs.plugins.bgmplus.kmp.library)
    alias(libs.plugins.bgmplus.kmp.room)
}

kmpAndroidLibrary {
    namespace = "com.infinitezerone.bgmplus.core.database"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(project(":core:common"))
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
    }
}
