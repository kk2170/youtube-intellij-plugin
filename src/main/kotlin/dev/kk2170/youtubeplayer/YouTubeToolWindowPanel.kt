package dev.kk2170.youtubeplayer

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

class YouTubeToolWindowPanel : JPanel(BorderLayout()), Disposable {
    private val searchField = JBTextField()
    private val statusLabel = JBLabel()
    private val summaryArea = JTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        background = JBUI.CurrentTheme.ToolWindow.background()
        foreground = JBUI.CurrentTheme.Label.foreground()
        border = JBUI.Borders.empty(16)
        text = "YouTube は app モードの外部ブラウザで開きます。\n\n" +
            "・埋め込み禁止動画でも再生しやすい\n" +
            "・Mix / Playlist URL をそのまま保持\n" +
            "・Edge / Chrome / Chromium を優先使用\n\n" +
            "『アプリで開く』は --app= 付きで起動、\n『ブラウザで開く』は通常タブで起動します。"
    }

    init {
        searchField.text = YouTubeUrls.DEFAULT_VIDEO_URL

        add(createToolbar(), BorderLayout.NORTH)
        add(summaryArea, BorderLayout.CENTER)

        val statusPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(statusLabel, BorderLayout.SOUTH)
        }
        add(statusPanel, BorderLayout.SOUTH)

        searchField.addActionListener { launchAppMode() }
        statusLabel.text = "待機中"
    }

    override fun dispose() = Unit

    private fun createToolbar(): JPanel {
        val appButton = JButton("アプリで開く", AllIcons.Actions.Execute).apply {
            addActionListener { launchAppMode() }
        }

        val browserButton = JButton("ブラウザで開く", AllIcons.Ide.External_link_arrow).apply {
            addActionListener {
                val target = YouTubeUrls.parseTarget(searchField.text)
                if (target == null) {
                    statusLabel.text = "有効な YouTube URL / 動画 ID / Playlist URL を入力してください。"
                    return@addActionListener
                }
                BrowserUtil.browse(target.browserUrl)
                statusLabel.text = "通常ブラウザで開きました。"
                updateSummary(target)
            }
        }

        return JPanel(BorderLayout(8, 0)).apply {
            border = JBUI.Borders.empty(4, 8)
            add(searchField, BorderLayout.CENTER)

            val buttonPanel = JPanel(BorderLayout(4, 0)).apply {
                add(appButton, BorderLayout.WEST)
                add(browserButton, BorderLayout.EAST)
            }
            add(buttonPanel, BorderLayout.EAST)
        }
    }

    private fun launchAppMode(input: String = searchField.text) {
        val target = YouTubeUrls.parseTarget(input)
        if (target == null) {
            statusLabel.text = "有効な YouTube URL / 動画 ID / Playlist URL を入力してください。"
            return
        }

        searchField.text = target.browserUrl
        val result = BrowserAppLauncher.launchAppMode(target.browserUrl)
        statusLabel.text = result.message
        updateSummary(target)
    }

    private fun updateSummary(target: YouTubeTarget) {
        summaryArea.text = buildString {
            appendLine("YouTube は app モードの外部ブラウザで開きます。")
            appendLine()
            appendLine("対象URL:")
            appendLine(target.browserUrl)
            appendLine()
            appendLine("判定:")
            appendLine("・videoId = ${target.videoId ?: "-"}")
            appendLine("・listId = ${target.listId ?: "-"}")
            appendLine("・index = ${target.index ?: "-"}")
            appendLine("・start = ${target.startSeconds ?: "-"}")
            appendLine()
            appendLine("Mix / Playlist は URL を保持したまま起動します。")
        }
    }
}
