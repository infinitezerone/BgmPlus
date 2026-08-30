import com.infinitezerone.bgmplus.kmpAndroidLibrary

plugins {
    alias(libs.plugins.bgmplus.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kmpAndroidLibrary {
    namespace = "com.infinitezerone.bgmplus.core.model"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
    }
}
