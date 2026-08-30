import com.infinitezerone.bgmplus.androidLibrary

plugins {
    alias(libs.plugins.bgmplus.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

androidLibrary {
    namespace = "com.infinitezerone.bgmplus.feature.subject"
}
