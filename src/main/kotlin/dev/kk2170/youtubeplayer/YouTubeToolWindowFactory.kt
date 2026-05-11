package dev.kk2170.youtubeplayer

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class YouTubeToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = YouTubeToolWindowPanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
