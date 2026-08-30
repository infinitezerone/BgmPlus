plugins {
    `kotlin-dsl`
}

group = "com.infinitezerone.bgmplus.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "bgmplus.kmp.library"
            implementationClass = "com.infinitezerone.bgmplus.KmpLibraryConventionPlugin"
        }
        register("kmpRoom") {
            id = "bgmplus.kmp.room"
            implementationClass = "com.infinitezerone.bgmplus.KmpRoomConventionPlugin"
        }
        register("androidApplication") {
            id = "bgmplus.android.application"
            implementationClass = "com.infinitezerone.bgmplus.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "bgmplus.android.application.compose"
            implementationClass = "com.infinitezerone.bgmplus.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "bgmplus.android.library"
            implementationClass = "com.infinitezerone.bgmplus.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "bgmplus.android.library.compose"
            implementationClass = "com.infinitezerone.bgmplus.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "bgmplus.android.feature"
            implementationClass = "com.infinitezerone.bgmplus.AndroidFeatureConventionPlugin"
        }
    }
}
