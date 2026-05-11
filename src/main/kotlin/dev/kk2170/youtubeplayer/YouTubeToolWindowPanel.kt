package dev.kk2170.youtubeplayer

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Toolkit
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class YouTubeToolWindowPanel(
    private val toolWindow: ToolWindow
) : JPanel(BorderLayout()), Disposable {
    private val searchField = JBTextField()
    private val statusLabel = JBLabel()
    private var browser: JBCefBrowser? = null
    private var isMaximized = false
    private var playerServer: LocalPlayerServer? = null

    init {
        searchField.text = YouTubeUrls.DEFAULT_VIDEO_URL

        if (!JBCefApp.isSupported()) {
            add(createUnsupportedPanel(), BorderLayout.CENTER)
        } else {
            playerServer = LocalPlayerServer().also { Disposer.register(this, it) }

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
                val target = YouTubeUrls.parseTarget(searchField.text)
                if (target == null) {
                    statusLabel.text = "有効な YouTube URL / 動画 ID / Playlist URL を入力してください。"
                    return@addActionListener
                }
                BrowserUtil.browse(target.browserUrl)
            }
        }

        // 最大化/元に戻すボタン
        val maximizeButton = JButton("最大化").apply {
            addActionListener { toggleMaximize() }
        }

        return JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(4, 8)
            add(searchField, BorderLayout.CENTER)
            
            val buttonPanel = JPanel(BorderLayout(4, 0)).apply {
                add(loadButton, BorderLayout.WEST)
                add(maximizeButton, BorderLayout.CENTER)
                add(openButton, BorderLayout.EAST)
            }
            add(buttonPanel, BorderLayout.EAST)
        }
    }

    private fun toggleMaximize() {
        if (isMaximized) {
            // 元に戻す (Docked)
            toolWindow.setType(ToolWindowType.DOCKED, null)
            isMaximized = false
        } else {
            // 最大化 (Floating + 画面サイズ)
            toolWindow.setType(ToolWindowType.FLOATING, null)
            
            // フレームを取得してサイズ変更
            val frame = toolWindow.component.parent
            if (frame != null) {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                frame.setSize(screenSize)
                frame.setLocation(0, 0)
            }
            isMaximized = true
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
        val target = YouTubeUrls.parseTarget(input)
        if (target == null) {
            statusLabel.text = "有効な YouTube URL / 動画 ID / Playlist URL を入力してください。"
            return
        }

        statusLabel.text = "読み込み中..."

        val playerUrl = playerServer?.playerUrlFor(target) ?: target.browserUrl
        browser?.loadURL(playerUrl)

        searchField.text = target.browserUrl
        statusLabel.text = when {
            target.listId != null -> "再生中: Playlist ${target.listId}"
            target.videoId != null -> "再生中: ${target.videoId}"
            else -> "再生中"
        }
    }
}
