import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "fi.mazhar.prayer.adhan.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "fi.mazhar.prayer.adhan"
            packageVersion = "1.0.0"
            // Generated from icons/ by tools/make_app_icons.py.
            val iconDir = project.file("icons")
            macOS { iconFile.set(iconDir.resolve("talibulwaqt.icns")) }
            windows { iconFile.set(iconDir.resolve("talibulwaqt.ico")) }
            linux { iconFile.set(iconDir.resolve("talibulwaqt.png")) }
        }
    }
}