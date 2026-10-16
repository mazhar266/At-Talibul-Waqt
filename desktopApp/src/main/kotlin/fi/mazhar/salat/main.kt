package fi.mazhar.salat

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    // Window and taskbar icon; the installers get theirs from desktopApp/icons via build.gradle.kts.
    val icon = remember { BitmapPainter(useResource("talibulwaqt_icon.png", ::loadImageBitmap)) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "TalibulWaqt",
        icon = icon,
    ) {
        App()
    }
}
