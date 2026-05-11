package dev.kk2170.youtubeplayer

object YouTubeUrls {
    const val DEFAULT_VIDEO_URL: String = "https://www.youtube.com/watch?v=M7lc1UVf-VE"

    fun buildEmbedUrl(videoId: String): String = "https://www.youtube.com/embed/$videoId?rel=0"

    fun buildWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId&hl=ja"

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        if (VIDEO_ID_PATTERN.matches(trimmed)) {
            return trimmed
        }

        return URL_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(trimmed)?.groupValues?.getOrNull(1)
        }
    }

    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")
    private val URL_PATTERNS = listOf(
        Regex("(?:[?&]v=)([A-Za-z0-9_-]{11})"),
        Regex("(?:youtu\\.be/)([A-Za-z0-9_-]{11})"),
        Regex("(?:youtube(?:-nocookie)?\\.com/(?:embed|shorts|live)/)([A-Za-z0-9_-]{11})"),
    )
}
