# Javalin Auto Restart Demo

Just run the following command to perform an automatic restart when you edit the sources
(same method as in the [micronaut project](https://micronaut.io/)).

```bash
./gradlew run -t
```

### Disclaimer

Whether this works as expected depends on the implementation of the gradle task and file system.
For example, running the source under /mnt/c(drvfs) from WSL2 did not generate the expected events.
Also, in the case of macOS(APFS), the generation of events are delayed by a few seconds.

See [build.gradle.kts](build.gradle.kts) and [plugin comments](src/main/kotlin/AutoShutdownPlugin.kt) for details.
