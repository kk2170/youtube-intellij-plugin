package dev.kk2170.youtubeplayer

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class YouTubeTarget(
    val browserUrl: String,
    val videoId: String?,
    val listId: String?,
    val index: Int?,
    val startSeconds: Int?,
) {
    val isPlaylist: Boolean
        get() = listId != null
}

object YouTubeUrls {
    const val DEFAULT_VIDEO_URL: String = "https://www.youtube.com/watch?v=M7lc1UVf-VE"

    fun buildEmbedUrl(videoId: String): String = "https://www.youtube.com/embed/$videoId?rel=0"

    fun buildWatchUrl(videoId: String): String = buildBrowserUrl(
        videoId = videoId,
        listId = null,
        index = null,
        startSeconds = null,
    )

    fun buildBrowserUrl(
        videoId: String?,
        listId: String?,
        index: Int?,
        startSeconds: Int?,
    ): String {
        val params = linkedMapOf<String, String>()
        videoId?.let { params["v"] = it }
        listId?.let { params["list"] = it }
        index?.let { params["index"] = it.toString() }
        startSeconds?.let { params["t"] = it.toString() }
        params["hl"] = "ja"

        val baseUrl = if (videoId != null) WATCH_BASE_URL else PLAYLIST_BASE_URL
        return "$baseUrl?${params.toQueryString()}"
    }

    fun buildLocalPlayerUrl(baseUrl: String, target: YouTubeTarget): String {
        val params = linkedMapOf(
            "browserUrl" to target.browserUrl,
        )
        target.videoId?.let { params["videoId"] = it }
        target.listId?.let { params["listId"] = it }
        target.index?.let { params["index"] = it.toString() }
        target.startSeconds?.let { params["start"] = it.toString() }
        return "$baseUrl/player?${params.toQueryString()}"
    }

    fun parseTarget(input: String): YouTubeTarget? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        if (VIDEO_ID_PATTERN.matches(trimmed)) {
            return YouTubeTarget(
                browserUrl = buildWatchUrl(trimmed),
                videoId = trimmed,
                listId = null,
                index = null,
                startSeconds = null,
            )
        }

        val normalizedInput = normalizeInput(trimmed)
        val uri = runCatching { URI(normalizedInput) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val query = parseQuery(uri.rawQuery)

        val videoId = when {
            host == "youtu.be" -> uri.path.trim('/').takeIf(VIDEO_ID_PATTERN::matches)
            host.contains("youtube.com") || host.contains("youtube-nocookie.com") -> {
                val pathSegments = uri.path.split('/').filter(String::isNotBlank)
                when (pathSegments.firstOrNull()) {
                    "watch" -> query["v"]?.takeIf(VIDEO_ID_PATTERN::matches)
                    "embed", "shorts", "live" -> pathSegments.getOrNull(1)?.takeIf(VIDEO_ID_PATTERN::matches)
                    else -> query["v"]?.takeIf(VIDEO_ID_PATTERN::matches)
                }
            }
            else -> null
        }

        val listId = query["list"]?.takeIf(String::isNotBlank)
        val index = query["index"]?.toIntOrNull()
        val startSeconds = parseStartSeconds(query)

        if (videoId == null && listId == null) {
            return null
        }

        return YouTubeTarget(
            browserUrl = buildBrowserUrl(videoId, listId, index, startSeconds),
            videoId = videoId,
            listId = listId,
            index = index,
            startSeconds = startSeconds,
        )
    }

    fun extractVideoId(input: String): String? = parseTarget(input)?.videoId

    private fun normalizeInput(input: String): String {
        return when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.startsWith("www.") || input.startsWith("m.") || input.startsWith("youtube.com") || input.startsWith("youtu.be") -> "https://$input"
            else -> input
        }
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) {
            return emptyMap()
        }

        return rawQuery
            .split('&')
            .mapNotNull { token ->
                if (token.isBlank()) {
                    null
                } else {
                    val parts = token.split('=', limit = 2)
                    val key = decode(parts[0])
                    val value = decode(parts.getOrElse(1) { "" })
                    key to value
                }
            }
            .toMap()
    }

    private fun parseStartSeconds(query: Map<String, String>): Int? {
        return query["start"]?.toIntOrNull()
            ?: parseTimeToken(query["t"])
            ?: parseTimeToken(query["time_continue"])
    }

    private fun parseTimeToken(value: String?): Int? {
        if (value.isNullOrBlank()) {
            return null
        }

        value.toIntOrNull()?.let { return it }

        val match = TIME_TOKEN_PATTERN.matchEntire(value) ?: return null
        val hours = match.groups[1]?.value?.dropLast(1)?.toIntOrNull() ?: 0
        val minutes = match.groups[2]?.value?.dropLast(1)?.toIntOrNull() ?: 0
        val seconds = match.groups[3]?.value?.dropLast(1)?.toIntOrNull() ?: 0
        val totalSeconds = hours * 3600 + minutes * 60 + seconds
        return totalSeconds.takeIf { it > 0 }
    }

    private fun Map<String, String>.toQueryString(): String = entries.joinToString("&") { (key, value) ->
        "${encode(key)}=${encode(value)}"
    }

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

    private const val WATCH_BASE_URL = "https://www.youtube.com/watch"
    private const val PLAYLIST_BASE_URL = "https://www.youtube.com/playlist"
    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")
    private val TIME_TOKEN_PATTERN = Regex("^(?:(\\d+h))?(?:(\\d+m))?(?:(\\d+s))?$")
}
