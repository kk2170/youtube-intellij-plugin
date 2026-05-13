package dev.kk2170.youtubeplayer

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isExecutable

data class BrowserLaunchResult(
    val success: Boolean,
    val message: String,
)

object BrowserAppLauncher {
    fun launchAppMode(url: String): BrowserLaunchResult {
        val candidates = candidateExecutables()
        if (candidates.isEmpty()) {
            return BrowserLaunchResult(
                success = false,
                message = "appモード対応ブラウザが見つかりませんでした。Chrome / Edge をインストールしてください。",
            )
        }

        val errors = mutableListOf<String>()
        for (candidate in candidates) {
            try {
                ProcessBuilder(
                    candidate.path.toString(),
                    "--new-window",
                    "--app=$url",
                ).start()

                return BrowserLaunchResult(
                    success = true,
                    message = "${candidate.displayName} を app モードで起動しました。",
                )
            } catch (error: Exception) {
                errors += "${candidate.displayName}: ${error.message ?: error.javaClass.simpleName}"
            }
        }

        return BrowserLaunchResult(
            success = false,
            message = buildString {
                append("appモード起動に失敗しました。")
                if (errors.isNotEmpty()) {
                    append(' ')
                    append(errors.joinToString(" / "))
                }
            },
        )
    }

    private fun candidateExecutables(): List<BrowserExecutable> {
        val configured = System.getenv("YOUTUBE_PLAYER_BROWSER")
            ?.takeIf(String::isNotBlank)
            ?.let(::pathIfExecutable)
            ?.let { listOf(BrowserExecutable("Configured Browser", it)) }
            .orEmpty()

        val osName = System.getProperty("os.name").lowercase()
        val defaults = when {
            osName.contains("win") -> windowsCandidates()
            osName.contains("mac") -> macCandidates()
            else -> linuxCandidates()
        }

        return (configured + defaults)
            .distinctBy { it.path.toAbsolutePath().normalize().toString() }
    }

    private fun windowsCandidates(): List<BrowserExecutable> {
        val names = listOf(
            "msedge.exe" to "Microsoft Edge",
            "chrome.exe" to "Google Chrome",
            "chromium.exe" to "Chromium",
        )

        val commonRoots = listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            System.getenv("LocalAppData"),
        )

        val absoluteCandidates = buildList {
            for ((exe, displayName) in names) {
                val relativePaths = when (exe) {
                    "msedge.exe" -> listOf(
                        "Microsoft/Edge/Application/$exe",
                    )
                    "chrome.exe" -> listOf(
                        "Google/Chrome/Application/$exe",
                    )
                    else -> listOf(
                        "Chromium/Application/$exe",
                    )
                }

                for (root in commonRoots) {
                    for (relativePath in relativePaths) {
                        pathIfExecutable(Paths.get(root, relativePath))?.let {
                            add(BrowserExecutable(displayName, it))
                        }
                    }
                }
            }
        }

        return absoluteCandidates + pathCandidates(names)
    }

    private fun macCandidates(): List<BrowserExecutable> = listOfNotNull(
        pathIfExecutable(Paths.get("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"))
            ?.let { BrowserExecutable("Microsoft Edge", it) },
        pathIfExecutable(Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"))
            ?.let { BrowserExecutable("Google Chrome", it) },
        pathIfExecutable(Paths.get("/Applications/Chromium.app/Contents/MacOS/Chromium"))
            ?.let { BrowserExecutable("Chromium", it) },
    )

    private fun linuxCandidates(): List<BrowserExecutable> = pathCandidates(
        listOf(
            "microsoft-edge" to "Microsoft Edge",
            "microsoft-edge-stable" to "Microsoft Edge",
            "google-chrome" to "Google Chrome",
            "google-chrome-stable" to "Google Chrome",
            "chromium" to "Chromium",
            "chromium-browser" to "Chromium",
        ),
    )

    private fun pathCandidates(names: List<Pair<String, String>>): List<BrowserExecutable> {
        return buildList {
            for ((exe, displayName) in names) {
                pathIfExecutableOnPath(exe)?.let { add(BrowserExecutable(displayName, it)) }
            }
        }
    }

    private fun pathIfExecutableOnPath(fileName: String): Path? {
        val pathEnv = System.getenv("PATH") ?: return null
        return pathEnv
            .split(File.pathSeparator)
            .firstNotNullOfOrNull { entry ->
                pathIfExecutable(Paths.get(entry, fileName))
            }
    }

    private fun pathIfExecutable(rawPath: String): Path? = pathIfExecutable(Paths.get(rawPath))

    private fun pathIfExecutable(path: Path): Path? {
        return path
            .takeIf { Files.exists(it) }
            ?.takeIf { Files.isRegularFile(it) }
            ?.takeIf { it.isExecutable() || path.toString().endsWith(".exe", ignoreCase = true) }
    }
}

private data class BrowserExecutable(
    val displayName: String,
    val path: Path,
)
