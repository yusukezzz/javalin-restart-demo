# Javalin Auto Restart Demo

Just run the following command to perform an automatic restart when you edit the sources
(same method as in the [micronaut project](https://micronaut.io/)).

```bash
./gradlew run -t
```

### Disclaimer

Whether this works as expected depends on the gradle task and the filesystem implementation.
For example, when executing a source under `/mnt/c` from WSL2, the expected event did not occur.

See build.gradle.kts and plugin comments for details.