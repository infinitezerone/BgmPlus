import com.infinitezerone.bgmplus.androidLibrary

plugins {
    alias(libs.plugins.bgmplus.android.library.compose)
}

androidLibrary {
    namespace = "com.infinitezerone.bgmplus.core.designsystem"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
}
