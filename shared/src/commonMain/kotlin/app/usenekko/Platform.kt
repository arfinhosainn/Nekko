package app.usenekko

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform