import com.infinitezerone.bgmplus.androidLibrary

plugins {
    alias(libs.plugins.bgmplus.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

androidLibrary {
    namespace = "com.infinitezerone.bgmplus.feature.search"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
