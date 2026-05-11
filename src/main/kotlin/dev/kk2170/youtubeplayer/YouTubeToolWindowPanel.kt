package dev.kk2170.youtubeplayer

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class YouTubeToolWindowPanel : JPanel(BorderLayout()), Disposable {
    private val videoField = JBTextField(YouTubeUrls.DEFAULT_VIDEO_URL, 40)
    private val statusLabel = JBLabel("YouTube URL または動画 ID を入力して Load を押してください。")
    private var browser: JBCefBrowser? = null

    init {
        border = JBUI.Borders.empty(8)

        if (!JBCefApp.isSupported()) {
            add(createUnsupportedPanel(), BorderLayout.CENTER)
        } else {
            val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                add(JLabel("YouTube URL / 動画 ID:"))
                add(videoField)

                val loadButton = JButton("読み込み")
                loadButton.addActionListener { loadVideo() }
                add(loadButton)

                val openButton = JButton("ブラウザで開く")
                openButton.addActionListener {
                    val videoId = YouTubeUrls.extractVideoId(videoField.text)
                    if (videoId == null) {
                        statusLabel.text = "有効な YouTube 動画 ID が検出されませんでした。"
                        return@addActionListener
                    }

                    BrowserUtil.browse(YouTubeUrls.buildWatchUrl(videoId))
                }
                add(openButton)
            }

            add(toolbar, BorderLayout.NORTH)

            browser = JBCefBrowser().also {
                Disposer.register(this, it)
                add(it.component, BorderLayout.CENTER)
            }

            add(statusLabel, BorderLayout.SOUTH)

            videoField.addActionListener { loadVideo() }
            loadVideo(YouTubeUrls.DEFAULT_VIDEO_URL)
        }
    }

    override fun dispose() = Unit

    private fun createUnsupportedPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(16)
            add(
                JBLabel(
                    "この IDE ランタイムでは JCEF が利用できません。JetBrains Runtime を使用して埋め込み再生を有効にしてください。",
                    SwingConstants.CENTER,
                ).apply {
                    foreground = JBColor.RED
                },
                BorderLayout.CENTER,
            )
        }
    }

    private fun loadVideo(input: String = videoField.text) {
        val videoId = YouTubeUrls.extractVideoId(input)
        if (videoId == null) {
            statusLabel.text = "有効な YouTube 動画 ID が検出されませんでした。"
            return
        }

        // Load the full watch page to ensure playback works.
        // Direct embed URLs often fail with Error 153 in JCEF due to missing Referer headers.
        browser?.loadURL(YouTubeUrls.buildWatchUrl(videoId))
        
        videoField.text = YouTubeUrls.buildWatchUrl(videoId)
        statusLabel.text = "動画を読み込みました: $videoId"
    }
}
