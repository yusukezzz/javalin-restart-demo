/*
 * Copyright 2022 yusukezzz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

/**
 * Automatically stops Javalin when a file is modified.
 * Available for ./gradlew run -t(--continuous) task.
 *
 * Usage:
 * ```
 * Javalin.create {
 *     if (isDev) {
 *         it.registerPlugin(AutoShutdownPlugin(listOf(Path.of("src", "main"))))
 *     }
 * }
 * ```
 *
 * IMPORTANT:
 * [paths] must be recognized by gradle task.
 * In other words, they must be treated as input files in the task executed under run.
 * By default, only files under src/main/java(kotlin) and
 * src/main/resources (depends on compileJava and compileKotlin tasks).
 * If you want to add src/main/foo, you must include a task that uses this directory as input in the run dependency.
 * Otherwise, when Javalin stops, gradle will not execute the next task.
 *
 * In addition, [java.nio.file.WatchService] behavior depends on the Filesystem.
 * Below are the patterns I have confirmed(04/07/2022).
 * If you are using macOS,
 * [directory-watcher](https://github.com/gmethvin/directory-watcher)
 * may be more efficient than the JVM default.
 *
 * 1. OK
 *     - Platform: Windows11
 *     - Filesystem: NTFS
 *     - JDK: Intellij IDE builtin(11.0.13)
 *     - Runner: Intellij IDE Run configuration
 * 1. OK
 *     - Platform: Linux(Ubuntu on WSL2)
 *     - Filesystem: ext4
 *     - JDK: OpenJDK(11.0.14.1+1-0ubuntu1~20.04)
 *     - Runner: gradle wrapper on Ubuntu terminal(zsh)
 * 1. NG(no events happened)
 *     - Platform: Linux(Ubuntu on WSL2)
 *     - Filesystem: drvfs (under /mnt/c)
 *     - JDK: OpenJDK(11.0.14.1+1-0ubuntu1~20.04)
 *     - Runner: gradle wrapper on Ubuntu terminal(zsh)
 */
class AutoShutdownPlugin(private val paths: List<Path>): Plugin {
    override fun apply(app: Javalin) {
        FileWatcher(paths) {
            app.stop()
            exitProcess(0)
        }.start()
    }
}

// from https://github.com/micronaut-projects/micronaut-core/blob/3.4.x/context/src/main/java/io/micronaut/scheduling/io/watch/DefaultWatchThread.java
// Not cleaning up this thread as it is just for local development.
private class FileWatcher(
    private val paths: List<Path>,
    private val watchService: WatchService = FileSystems.getDefault().newWatchService(),
    private val fileChangedHandler: () -> Unit
): Thread("filewatcher-thread") {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val watchKeys = ConcurrentLinkedDeque<WatchKey>()

    override fun run() {
        paths.forEach {
            addWatchDirectory(it)
        }
        logger.debug("Start watching...")
        while (true) {
            watch()
        }
    }

    private fun watch() {
        watchService.poll(300, TimeUnit.MILLISECONDS)?.let { key ->
            if (watchKeys.contains(key)) {
                key.pollEvents().forEach { event ->
                    if (event.context() is Path) {
                        fileChangedHandler()
                    }
                }
            }
            key.reset()
        }
    }

    private fun registerPath(path: Path): WatchKey =
        path.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY)

    private fun isValidDirectoryToMonitor(file: File): Boolean =
        (file.isDirectory && !file.isHidden && !file.name.startsWith("."))

    private fun addWatchDirectory(path: Path) {
        Files.walkFileTree(path, object: SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(p: Path, attr: BasicFileAttributes): FileVisitResult {
                if (!isValidDirectoryToMonitor(p.toFile())) {
                    return FileVisitResult.SKIP_SUBTREE
                }
                logger.debug("Add watch path: $p")
                watchKeys.add(registerPath(p))
                return FileVisitResult.CONTINUE
            }
        })
    }
}
