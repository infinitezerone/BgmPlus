import com.infinitezerone.bgmplus.androidLibrary

plugins {
    alias(libs.plugins.bgmplus.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

androidLibrary {
    namespace = "com.infinitezerone.bgmplus.feature.schedule"
}

dependencies {
    // 播放平台链接经 Custom Tabs 打开
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
