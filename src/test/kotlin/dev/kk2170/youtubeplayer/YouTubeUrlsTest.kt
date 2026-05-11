package dev.kk2170.youtubeplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeUrlsTest {
    @Test
    fun `extract raw video id`() {
        assertEquals("M7lc1UVf-VE", YouTubeUrls.extractVideoId("M7lc1UVf-VE"))
    }

    @Test
    fun `extract from standard watch url`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://www.youtube.com/watch?v=M7lc1UVf-VE"),
        )
    }

    @Test
    fun `extract from mobile watch url with extra params`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://m.youtube.com/watch?feature=share&v=M7lc1UVf-VE&t=42"),
        )
    }

    @Test
    fun `extract from short url`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://youtu.be/M7lc1UVf-VE?si=demo"),
        )
    }

    @Test
    fun `extract from embed nocookie url`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://www.youtube-nocookie.com/embed/M7lc1UVf-VE"),
        )
    }

    @Test
    fun `extract from shorts url`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://www.youtube.com/shorts/M7lc1UVf-VE"),
        )
    }

    @Test
    fun `extract from live url`() {
        assertEquals(
            "M7lc1UVf-VE",
            YouTubeUrls.extractVideoId("https://www.youtube.com/live/M7lc1UVf-VE?feature=share"),
        )
    }

    @Test
    fun `reject invalid input`() {
        assertNull(YouTubeUrls.extractVideoId("https://example.com/video"))
        assertNull(YouTubeUrls.extractVideoId("not a youtube id"))
        assertNull(YouTubeUrls.extractVideoId(""))
    }
}
