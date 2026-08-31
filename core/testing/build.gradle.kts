import com.infinitezerone.bgmplus.kmpAndroidLibrary

plugins {
    alias(libs.plugins.bgmplus.kmp.library)
}

kmpAndroidLibrary {
    namespace = "com.infinitezerone.bgmplus.core.testing"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            api(project(":core:common"))
            api(project(":core:data"))
            api(project(":core:datastore"))
            api(libs.androidx.datastore.core)
            api(libs.kotlinx.coroutines.test)
            api(libs.kotlin.test)
            api(libs.junit)
        }
        matching { it.name == "androidHostTest" }.configureEach {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.junit)
            }
        }
    }
}
