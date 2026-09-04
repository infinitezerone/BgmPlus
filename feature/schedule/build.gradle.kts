import com.infinitezerone.minibgm.androidLibrary

plugins {
    alias(libs.plugins.minibgm.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

androidLibrary {
    namespace = "com.infinitezerone.minibgm.feature.schedule"
}

dependencies {
    // 播放平台链接经 Custom Tabs 打开
    implementation(libs.androidx.browser)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
