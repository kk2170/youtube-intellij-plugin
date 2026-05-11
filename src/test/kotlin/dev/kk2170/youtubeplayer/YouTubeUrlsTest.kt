package dev.kk2170.youtubeplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun `parse playlist watch url preserves playlist fields`() {
        val target = assertNotNull(
            YouTubeUrls.parseTarget("https://www.youtube.com/watch?v=M7lc1UVf-VE&list=PL1234567890ABCDE&index=4&t=95"),
        )

        assertEquals("M7lc1UVf-VE", target.videoId)
        assertEquals("PL1234567890ABCDE", target.listId)
        assertEquals(4, target.index)
        assertEquals(95, target.startSeconds)
        assertEquals(
            "https://www.youtube.com/watch?v=M7lc1UVf-VE&list=PL1234567890ABCDE&index=4&t=95&hl=ja",
            target.browserUrl,
        )
    }

    @Test
    fun `parse mix url keeps mix list`() {
        val target = assertNotNull(
            YouTubeUrls.parseTarget("https://www.youtube.com/watch?v=I1t9fTZEO9E&list=RDI1t9fTZEO9E&start_radio=1"),
        )

        assertEquals("I1t9fTZEO9E", target.videoId)
        assertEquals("RDI1t9fTZEO9E", target.listId)
        assertEquals(null, target.index)
        assertEquals(
            "https://www.youtube.com/watch?v=I1t9fTZEO9E&list=RDI1t9fTZEO9E&hl=ja",
            target.browserUrl,
        )
    }

    @Test
    fun `parse playlist url without video id`() {
        val target = assertNotNull(
            YouTubeUrls.parseTarget("https://www.youtube.com/playlist?list=PL1234567890ABCDE"),
        )

        assertEquals(null, target.videoId)
        assertEquals("PL1234567890ABCDE", target.listId)
        assertEquals(
            "https://www.youtube.com/playlist?list=PL1234567890ABCDE&hl=ja",
            target.browserUrl,
        )
    }

    @Test
    fun `parse time token with minutes and seconds`() {
        val target = assertNotNull(
            YouTubeUrls.parseTarget("https://youtu.be/M7lc1UVf-VE?t=1m35s"),
        )

        assertEquals(95, target.startSeconds)
        assertEquals(
            "https://www.youtube.com/watch?v=M7lc1UVf-VE&t=95&hl=ja",
            target.browserUrl,
        )
    }

    @Test
    fun `reject invalid input`() {
        assertNull(YouTubeUrls.extractVideoId("https://example.com/video"))
        assertNull(YouTubeUrls.extractVideoId("not a youtube id"))
        assertNull(YouTubeUrls.extractVideoId(""))
        assertNull(YouTubeUrls.parseTarget("https://example.com/video"))
    }
}
