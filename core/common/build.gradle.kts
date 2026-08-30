import com.infinitezerone.bgmplus.kmpAndroidLibrary

plugins {
    alias(libs.plugins.bgmplus.kmp.library)
}

kmpAndroidLibrary {
    namespace = "com.infinitezerone.bgmplus.core.common"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
    }
}
