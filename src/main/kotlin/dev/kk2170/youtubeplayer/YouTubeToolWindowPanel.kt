package dev.kk2170.youtubeplayer

import com.intellij.icons.AllIcons
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
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class YouTubeToolWindowPanel : JPanel(BorderLayout()), Disposable {
    private val searchField = JBTextField()
    private val statusLabel = JBLabel()
    private var browser: JBCefBrowser? = null

    init {
        if (!JBCefApp.isSupported()) {
            add(createUnsupportedPanel(), BorderLayout.CENTER)
        } else {
            val toolbar = createToolbar()
            add(toolbar, BorderLayout.NORTH)

            browser = JBCefBrowser().also {
                Disposer.register(this, it)
                // ブラウザの境界線を消し、IDEの背景色に合わせる
                it.component.border = BorderFactory.createEmptyBorder()
                add(it.component, BorderLayout.CENTER)
            }

            val statusPanel = JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(4, 8)
                add(statusLabel, BorderLayout.SOUTH)
            }
            add(statusPanel, BorderLayout.SOUTH)

            searchField.addActionListener { loadVideo() }

            loadVideo(YouTubeUrls.DEFAULT_VIDEO_URL)
        }
    }

    private fun createToolbar(): JPanel {
        val loadButton = JButton("読み込み", AllIcons.Actions.Execute).apply {
            addActionListener { loadVideo() }
        }

        val openButton = JButton("ブラウザで開く", AllIcons.Ide.External_link_arrow).apply {
            addActionListener {
                val videoId = YouTubeUrls.extractVideoId(searchField.text)
                if (videoId == null) {
                    statusLabel.text = "有効な YouTube 動画 ID が検出されませんでした。"
                    return@addActionListener
                }
                BrowserUtil.browse(YouTubeUrls.buildWatchUrl(videoId))
            }
        }

        return JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(4, 8)
            add(searchField, BorderLayout.CENTER)
            
            val buttonPanel = JPanel(BorderLayout(4, 0)).apply {
                add(loadButton, BorderLayout.WEST)
                add(openButton, BorderLayout.EAST)
            }
            add(buttonPanel, BorderLayout.EAST)
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

    private fun loadVideo(input: String = searchField.text) {
        val videoId = YouTubeUrls.extractVideoId(input)
        if (videoId == null) {
            statusLabel.text = "有効な YouTube 動画 ID が検出されませんでした。"
            return
        }

        statusLabel.text = "読み込み中..."
        
        // 埋め込みプレイヤー（iframe）では Error 153 が発生しやすいため、
        // 通常の YouTube 視聴ページ（watch）を直接表示して再生を安定させます。
        browser?.loadURL(YouTubeUrls.buildWatchUrl(videoId))

        searchField.text = YouTubeUrls.buildWatchUrl(videoId)
        statusLabel.text = "再生中: $videoId"
    }
}
