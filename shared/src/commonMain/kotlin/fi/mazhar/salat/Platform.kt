package fi.mazhar.salat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform