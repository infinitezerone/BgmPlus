import com.infinitezerone.bgmplus.androidLibrary

plugins {
    alias(libs.plugins.bgmplus.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

androidLibrary {
    namespace = "com.infinitezerone.bgmplus.feature.user"
}

dependencies {
    implementation(project(":core:datastore"))
    // UserViewModel 经 Custom Tabs 打开授权页
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)
}
