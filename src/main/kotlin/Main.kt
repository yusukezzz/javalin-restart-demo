import io.javalin.Javalin
import java.nio.file.Path

fun main() {
    System.setProperty("org.slf4j.simpleLogger.log.FileWatcher", "DEBUG")
    val app = Javalin.create {
        // Should only be registered for local development
        it.registerPlugin(AutoShutdownPlugin(listOf(Path.of("src", "main"))))
    }
    app.get("/") {
        it.result("Hello world!")
    }
    app.start(9000)
}