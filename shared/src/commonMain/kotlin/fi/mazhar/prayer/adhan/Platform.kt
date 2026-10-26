package fi.mazhar.prayer.adhan

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform