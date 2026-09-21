# Okean Visuals — GitHub build

Этот вариант не использует `gradlew.bat`, поэтому Windows Smart App Control не должен блокировать локальный launcher.

Самый простой способ получить JAR:
1. Загрузить содержимое проекта в репозиторий GitHub `dimaborodaenko2015ujl-netizen/okeanvisuals`.
2. Открыть вкладку Actions.
3. Запустить workflow `Build Okean Visuals`.
4. После завершения открыть запуск workflow и скачать artifact `OkeanVisuals-1.21.4`.

Сборка выполняется на GitHub Actions с Java 21 и Gradle.
