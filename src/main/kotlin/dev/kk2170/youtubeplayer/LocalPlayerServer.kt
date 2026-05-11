package dev.kk2170.youtubeplayer

import com.intellij.openapi.Disposable
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LocalPlayerServer : Disposable {
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val server: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)

    val baseUrl: String
        get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/player") { exchange -> handlePlayer(exchange) }
        server.executor = executor
        server.start()
    }

    fun playerUrlFor(target: YouTubeTarget): String = YouTubeUrls.buildLocalPlayerUrl(baseUrl, target)

    override fun dispose() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun handlePlayer(exchange: HttpExchange) {
        if (exchange.requestMethod != "GET") {
            exchange.sendResponseHeaders(405, -1)
            exchange.close()
            return
        }

        val params = parseQuery(exchange.requestURI.rawQuery)
        val html = buildPlayerHtml(
            browserUrl = params["browserUrl"],
            videoId = params["videoId"],
            listId = params["listId"],
            index = params["index"]?.toIntOrNull(),
            startSeconds = params["start"]?.toIntOrNull(),
        )

        val body = html.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.responseHeaders.add("Cache-Control", "no-store")
        exchange.sendResponseHeaders(200, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }

    private fun buildPlayerHtml(
        browserUrl: String?,
        videoId: String?,
        listId: String?,
        index: Int?,
        startSeconds: Int?,
    ): String {
        val safeBrowserUrl = browserUrl ?: "https://www.youtube.com/"
        val origin = baseUrl

        return """
            <!DOCTYPE html>
            <html lang="ja">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                html, body {
                  margin: 0;
                  width: 100%;
                  height: 100%;
                  overflow: hidden;
                  background: #000;
                }

                #player {
                  width: 100%;
                  height: 100%;
                }

                #fallback {
                  position: absolute;
                  inset: 0;
                  display: none;
                  align-items: center;
                  justify-content: center;
                  flex-direction: column;
                  gap: 12px;
                  color: #fff;
                  background: #111;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                }

                #fallback a {
                  color: #8ab4f8;
                }
              </style>
            </head>
            <body>
              <div id="player"></div>
              <div id="fallback">
                <div id="message">プレイヤーを読み込めませんでした。</div>
                <a href="${htmlEscape(safeBrowserUrl)}" target="_blank" rel="noopener noreferrer">ブラウザで開く</a>
              </div>
              <script>
                const target = {
                  browserUrl: ${jsonString(safeBrowserUrl)},
                  videoId: ${jsonString(videoId)},
                  listId: ${jsonString(listId)},
                  index: ${index?.toString() ?: "null"},
                  startSeconds: ${startSeconds?.toString() ?: "null"},
                  origin: ${jsonString(origin)}
                };

                const fallback = document.getElementById('fallback');
                const message = document.getElementById('message');

                function showFallback(text) {
                  message.textContent = text;
                  fallback.style.display = 'flex';
                }

                var tag = document.createElement('script');
                tag.src = 'https://www.youtube.com/iframe_api';
                document.head.appendChild(tag);

                let player;

                function onYouTubeIframeAPIReady() {
                  const playerVars = {
                    autoplay: 1,
                    controls: 1,
                    fs: 1,
                    hl: 'ja',
                    playsinline: 1,
                    rel: 0,
                    enablejsapi: 1,
                    origin: target.origin,
                    widget_referrer: target.origin
                  };

                  if (target.listId) {
                    playerVars.listType = 'playlist';
                    playerVars.list = target.listId;
                    if (target.index !== null) {
                      playerVars.index = target.index;
                    }
                  }

                  const options = {
                    width: '100%',
                    height: '100%',
                    playerVars,
                    events: {
                      onReady: onPlayerReady,
                      onError: onPlayerError
                    }
                  };

                  if (!target.listId && target.videoId) {
                    options.videoId = target.videoId;
                  }

                  player = new YT.Player('player', options);
                }

                function onPlayerReady(event) {
                  event.target.playVideo();

                  if (target.listId && target.videoId && target.index === null) {
                    syncPlaylistSelection(event.target);
                  } else if (target.startSeconds !== null) {
                    window.setTimeout(() => event.target.seekTo(target.startSeconds, true), 600);
                  }
                }

                function syncPlaylistSelection(activePlayer) {
                  let attempts = 0;
                  const timer = window.setInterval(() => {
                    attempts += 1;
                    const playlist = activePlayer.getPlaylist();
                    if (Array.isArray(playlist) && playlist.length > 0) {
                      const matchingIndex = playlist.indexOf(target.videoId);
                      if (matchingIndex >= 0) {
                        activePlayer.playVideoAt(matchingIndex);
                        if (target.startSeconds !== null) {
                          window.setTimeout(() => activePlayer.seekTo(target.startSeconds, true), 600);
                        }
                        window.clearInterval(timer);
                      } else if (attempts >= 20) {
                        window.clearInterval(timer);
                      }
                    } else if (attempts >= 20) {
                      window.clearInterval(timer);
                    }
                  }, 500);
                }

                function onPlayerError(event) {
                  const code = event.data;
                  if (code === 153) {
                    showFallback('YouTube が Referer / API client identification を要求しています。ブラウザで開いてください。');
                    return;
                  }

                  if (code === 101 || code === 150) {
                    showFallback('この動画は埋め込み再生を許可していません。ブラウザで開いてください。');
                    return;
                  }

                  showFallback('プレイヤーエラーが発生しました (code: ' + code + ')。ブラウザで開いてください。');
                }
              </script>
            </body>
            </html>
        """.trimIndent()
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

    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)

    private fun htmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun jsonString(value: String?): String {
        if (value == null) {
            return "null"
        }

        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }
}
