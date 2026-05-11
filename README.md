# YouTube IntelliJ Plugin

Minimal IntelliJ Platform plugin scaffold that adds a `YouTube` tool window backed by JCEF.

Current scaffold targets the latest stable OSS/tooling available at implementation time:

- IntelliJ Platform Gradle Plugin `2.16.0`
- Kotlin Gradle Plugin `2.3.21`
- IntelliJ IDEA Community `2025.3`
- Gradle Wrapper `9.5.0`

## What it does

- opens a `YouTube` tool window inside IntelliJ IDEA
- embeds a YouTube player with `JBCefBrowser`
- lets you paste a YouTube URL or a raw 11-character video ID and load it

## Project structure

- `build.gradle.kts` / `settings.gradle.kts`: Gradle-based IntelliJ Platform plugin setup
- `src/main/resources/META-INF/plugin.xml`: plugin registration
- `src/main/kotlin/dev/kk2170/youtubeplayer`: tool window implementation

## Run locally

1. Open this folder in IntelliJ IDEA.
2. Import the Gradle project.
3. Run the Gradle `runIde` task.
4. In the sandbox IDE, open the `YouTube` tool window from the right side.

## Verify locally

- `./gradlew test`
- `./gradlew build`
- `./gradlew runIde`

## Notes

- The plugin uses the IDE's embedded browser (JCEF) to open the standard YouTube watch page.
- This approach ensures playback works reliably without being blocked by embedding restrictions (Error 153).
- Automated tests cover raw IDs plus `watch`, `youtu.be`, `shorts`, `live`, and `youtube-nocookie` URL formats.
