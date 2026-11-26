# ДЗ №9 — корутины: отмена установки и последовательная/параллельная загрузка

Проект: `C:\Kotlin\StartApp` (Android, Compose, MVVM + Hilt, Retrofit).
Сборка и проверка: `./gradlew :app:assembleDebug`, установка на эмулятор
`Medium_Phone_API_35`, проверка по дереву доступности (`uiautomator dump`) и скриншотам.

---

## Задание 1. Отмена установки приложения

### Что сделано

`presentation/detail/AppDetailViewModel.kt`

- Состояние установки:

```kotlin
sealed interface InstallState {
    data object NotInstalled : InstallState
    data class Installing(val progress: Int) : InstallState // 0..100
    data object Installed : InstallState
}
```

- В `AppDetailUiState.Content` добавлено поле `installState: InstallState`
  (по умолчанию `NotInstalled`).
- Прогресс установки — это состояние UI, а не данные каталога, поэтому он живёт
  в отдельном `MutableStateFlow<InstallState>` и попадает в `Content` через
  `combine(repository.apps, _installState)`. Так обновление прогресса
  не перезапускает загрузку каталога и наоборот.
- Приватное поле `installJob: Job?` и функция `onInstallClick()`:

```kotlin
fun onInstallClick() {
    val current = _installState.value
    if (current is InstallState.Installing) {
        installJob?.cancel()                    // отмена установки
        installJob = null
        _installState.value = InstallState.NotInstalled
        _events.trySend(AppDetailEvent.InstallCancelled)
        return
    }

    installJob = viewModelScope.launch {        // запуск установки
        _installState.value = InstallState.Installing(0)
        try {
            emulateInstall()
            _installState.value = InstallState.Installed
            _events.send(AppDetailEvent.InstallFinished)
        } finally {
            installJob = null
        }
    }
    _events.trySend(AppDetailEvent.InstallStarted)
}
```

- Эмуляция с точкой отмены:

```kotlin
private suspend fun emulateInstall() {
    for (progress in 0..100 step PROGRESS_STEP) {   // PROGRESS_STEP = 5
        delay(PROGRESS_DELAY_MS)                   // 150 мс — точка отмены
        _installState.update { InstallState.Installing(progress) }
    }
}
```

`delay` — приостанавливающая функция, поэтому `installJob.cancel()`
прерывает корутину с `CancellationException` до `InstallState.Installed`,
`finally` освобождает ссылку на джобу, а UI-состояние сбрасывается в
`NotInstalled` в обработчике нажатия.

`presentation/detail/AppDetailScreen.kt`

- Новый композабл `InstallButton(installState, onInstallClick)`:
  - `NotInstalled` → кнопка «Установить» с иконкой загрузки;
  - `Installing(progress)` → кнопка «Отменить» (цвета `error`) +
    `LinearProgressIndicator` + подпись «Установка… N%»;
  - `Installed` → неактивная кнопка «Установлено».
- `AppDetailScreen` получает колбэк `onInstallClick` из `AppDetailRoute`,
  который вызывает `viewModel::onInstallClick` (заглушка
  `showUnderDevelopmentMessage()` больше не используется).

Разовые события расширены: `InstallStarted`, `InstallFinished`, `InstallCancelled` —
`AppDetailRoute` показывает по ним снекбар («Загрузка «X» началась»,
««X» установлено», «Установка «X» отменена»).

### Проверка на устройстве

| Действие | Что показал экран |
|---|---|
| открыли карточку QuickNote | `Установить` |
| нажали «Установить» | `Отменить` и `Установка… 55%`, снекбар «Загрузка «QuickNote» началась» |
| дождались конца (≈3,2 с) | `Установлено`, снекбар ««QuickNote» установлено» |
| вернулись в список, снова открыли карточку, нажали «Установить», через ~0,7 с нажали «Отменить» | `Установить`, снекбар «Установка «QuickNote» отменена»; ещё через 4 с — по-прежнему `Установить` (джоба не «доехала» до конца) |

---

## Задание 2. Загрузка рейтингов в каталоге

### Что сделано

- `data/source/network/RatingApi.kt` — аналог `AppApi` для рейтингов:

```kotlin
interface RatingApi {
    suspend fun getRating(id: String): Float
}

class EmulatedRatingApi @Inject constructor() : RatingApi {
    override suspend fun getRating(id: String): Float {
        delay(NETWORK_DELAY_MS)            // 1500 мс — эмуляция сети
        return (30..50).random() / 10f     // 3.0..5.0
    }
}
```

- `domain/repository/RatingRepository.kt` — порт домена,
  `data/EmulatedRatingRepository.kt` — реализация поверх `RatingApi`,
  биндинги в `di/AppModule.kt`.
- `domain/usecase/LoadRatingsUseCase.kt` — два способа и замер времени:

```kotlin
suspend fun loadSequentially(ids: List<String>): RatingsLoadResult {
    val ratings = LinkedHashMap<String, Float>(ids.size)
    val elapsedMs = measureTimeMillis {
        for (id in ids) ratings[id] = ratingRepository.getRating(id)
    }
    return RatingsLoadResult(ratings, elapsedMs, ids.size)
}

suspend fun loadInParallel(ids: List<String>): RatingsLoadResult {
    lateinit var ratings: Map<String, Float>
    val elapsedMs = measureTimeMillis {
        ratings = coroutineScope {
            ids.map { id -> async { id to ratingRepository.getRating(id) } }.awaitAll().toMap()
        }
    }
    return RatingsLoadResult(ratings, elapsedMs, ids.size)
}
```

- `presentation/list/AppListViewModel.kt` — в `init` ждёт каталог
  (`repository.apps.first { it.isNotEmpty() }`), затем замеряет оба способа
  по очереди на **одних и тех же 10 приложениях**
  (`RATINGS_SAMPLE_SIZE = 10`), после чего параллельно догружает рейтинги
  остальных приложений каталога (41 шт.) — уже только для UI, не для замера.
  Кнопка «Замерить заново» повторяет замер, предыдущая джоба отменяется.
  Тайминги дополнительно пишутся в logcat (`AppListViewModel`).
- `presentation/list/AppListScreen.kt` — блок «Рейтинги приложений» со строками
  «Последовательно — 15.0 с · 10 запросов», «Параллельно — 1.5 с · 10 запросов»,
  «Параллельно быстрее в 10.0 раза» и кнопкой перезапуска;
  в каждой карточке рядом с названием — звезда и число (`★ 4.9`).

### Результаты замеров (эмулятор, каталог из 41 приложения, замер на 10)

| Способ | Прогон 1 | Прогон 2 («Замерить заново») |
|---|---|---|
| Последовательно (`for` + suspend-вызов) | **15 035 мс** | **15 023 мс** |
| Параллельно (`async` + `awaitAll`) | **1 512 мс** | **1 503 мс** |
| Отношение | ≈ 9,9× | ≈ 10,0× |

Эмуляция задержки сети — 1 500 мс на запрос, запросов 10.

### Выводы

1. **Последовательный вариант ≈ N × задержка.** 10 запросов × 1,5 с = 15 000 мс;
   накладные расходы на переключение корутин — всего 23–35 мс на весь пакет
   (≈3 мс на запрос). Время растёт линейно от количества приложений:
   для всего каталога (41 шт.) это было бы ≈61 с.
2. **Параллельный вариант ≈ одна задержка.** 1 512 мс ≈ 1 500 мс + ~12 мс
   накладных расходов: все 10 `async` «висят» на `delay` одновременно,
   потому что `delay` — приостановка корутины, а не блокировка потока.
   Поток освобождается, и планировщик успевает запустить остальные корутины.
   Отсюда ускорение примерно в 10 раз — почти в число запросов.
3. **Structured concurrency.** `coroutineScope` внутри `loadInParallel`
   гарантирует, что все `async` принадлежат одному родителю: если один запрос
   падает с исключением, остальные отменяются, а исключение пробрасывается
   наружу (`awaitAll` / родительский scope). Если отменить весь замер
   (например, ViewModel очищается или нажата «Замерить заново»), дочерние
   корутины отменяются автоматически — «висящих» запросов не остаётся.
   Поэтому в ViewModel замер лежит в `viewModelScope`, а ссылка на джобу
   (`ratingsJob`) нужна, чтобы перезапуск отменял предыдущий замер.
4. **Параллельность без ограничений — не всегда хорошо.** Для 10 запросов
   это идеально, но для сотен одновременных запросов легко упереться в лимиты
   сети/сервера и в память. При реальной нагрузке распараллеливание
   ограничивают (`Semaphore`, `flatMapMerge(concurrency = N)`, пул диспетчеров),
   а последовательный вариант остаётся нужен, когда запросы зависят друг от друга
   (например, рейтинг нужен только после получения id из ответа сервера).
5. **Практический итог для экрана:** последовательная загрузка 10 рейтингов
   (15 с) — заметный для пользователя «залипший» экран, параллельная (1,5 с) —
   приемлемая. Поэтому в реальном UI рейтинги грузятся параллельно, а
   последовательный способ оставлен как учебный замер для сравнения.

---

## Попутные правки, без которых проект не собирался/не запускался

Эти изменения не относятся к теме ДЗ, но были нужны, чтобы приложение
собралось и заработало на устройстве (в исходном состоянии `assembleDebug`
падал, а приложение крэшилось при старте):

1. `app/build.gradle.kts`: `kotlin(kotlin.kapt)` → `alias(libs.plugins.android.legacy.kapt)`
   (`com.android.legacy-kapt`, версия из `agp`). В AGP 9 включён built-in Kotlin,
   и `org.jetbrains.kotlin.kapt` с ним несовместим — AGP сам предлагает
   `com.android.legacy-kapt`.
2. `gradle/libs.versions.toml`: Hilt 2.56.2 → **2.60.1** (2.56.2 падал на AGP 9:
   `Android BaseExtension not found`).
3. `data/mapper/AppMapper.kt`: `toDomain` / `toDomainList` были member-extension’ами
   внутри `object AppMapper`, а импортировались как топ-левел
   (`import ru.mobile.startapp.data.mapper.toDomain`) — сделаны топ-левел функциями.
4. `data/NetworkAppRepository.kt`: добавлен импорт `CatalogAppDto`;
   `collect` из `init` вынесен в `CoroutineScope(SupervisorJob() + Dispatchers.IO)`
   (collect — suspend-функция, из `init` напрямую её звать нельзя);
   в `catch` добавлен лог причины.
5. `MainActivity.kt`: убран несуществующий параметр `StartAppNavHost(viewModel = …)`
   (остался от ручного DI до Hilt).
6. `AppListRoute` / `AppDetailRoute`: `viewModel()` → `hiltViewModel()`
   (иначе `FATAL: NoSuchMethodException AppListViewModel.<init>`).
7. `AndroidManifest.xml`: `android:usesCleartextTraffic="true"` — каталог отдаётся
   по `http://185.103.109.134/`, без этого запрос блокировался и список вечно
   висел в состоянии загрузки.

## Как воспроизвести

```bash
cd C:/Kotlin/StartApp
export JAVA_HOME='C:\Users\skute\.jdks\corretto-21.0.4'
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mobile.startapp/.MainActivity
adb logcat -s AppListViewModel    # тайминги обоих способов
```

---

# ДЗ №11 — Wishlist: отметка «в вишлисте» в Room и иконка в тулбаре

Задание сделано после `git revert` предыдущей реализации «Понравившихся»:
каталог снова читается из Room, но пользовательская отметка называется
`isInWishlist` и переключается иконкой в тулбаре карточки.

## Что сделано

### 1. Поле в Entity БД и доменной модели

`data/source/local/db/AppEntity.kt` — таблица `apps`:

```kotlin
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val developer: String,
    val version: String,
    val size: String,
    val fullDescription: String,
    val isInWishlist: Boolean = false,
    val position: Int = 0
)
```

`domain/App.kt` — добавлено `val isInWishlist: Boolean = false`. Поле хранится
только в БД: сеть его не присылает, поэтому обновление каталога не должно
его затирать.

### 2. Мапперы

`data/mapper/AppMapper.kt`:

- `CatalogAppDto.toEntity(position)` — сеть → Entity, `isInWishlist = false`,
  `position` из `mapIndexed`;
- `AppEntity.toDomain()` — Entity → Domain, переносит `isInWishlist`;
- `App.toEntity(position)` — Domain → Entity;
- `List<AppEntity>.toDomainApps()` — список Entity → список Domain
  (имя отличается от `List<AppDto>.toDomainList()`, иначе после стирания
  дженериков совпадает JVM-сигнатура).

### 3. DAO: обновление статуса

`data/source/local/db/AppDao.kt`:

```kotlin
@Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
fun getAppDetails(id: String): Flow<AppEntity?>

@Query("UPDATE apps SET isInWishlist = :isInWishlist WHERE id = :id")
suspend fun updateWishlistStatus(id: String, isInWishlist: Boolean)
```

`upsertCatalog` (`@Transaction`) вставляет новые строки (`OnConflictStrategy.IGNORE`)
и обновляет только контентные поля — `isInWishlist` не входит в UPDATE,
поэтому отметка переживает перезагрузку каталога.

### 4. Репозиторий: подписки и переключение

`domain/repository/AppRepository.kt`:

```kotlin
val apps: Flow<List<App>>
fun observeApp(appId: String): Flow<App?>
suspend fun toggleWishlist(appId: String)
```

`data/NetworkAppRepository.kt`: Room — единственный источник правды.

```kotlin
override val apps: Flow<List<App>> =
    appDetailsDao.observeApps().map { it.toDomainApps() }

override fun observeApp(appId: String): Flow<App?> =
    appDetailsDao.getAppDetails(appId).map { it?.toDomain() }

override suspend fun toggleWishlist(appId: String) {
    val currentEntity = appDetailsDao.getAppDetails(appId).first()
    currentEntity?.let { appDetailsDao.updateWishlistStatus(appId, !it.isInWishlist) }
}
```

Загрузка каталога из сети пишет ответ в Room; HTTP-ошибка, пустой ответ и
сетевая ошибка оставляют прежние строки — отметки не теряются.
Блок «Вам может понравиться» вынесен в расширение `List<App>.similarApps(appId)`
(`domain/SimilarApps.kt`), потому что каталог теперь Flow, а не синхронный `StateFlow`.

### 5. ViewModel: новое поле и подписка

`presentation/detail/AppDetailViewModel.kt`:

```kotlin
data class Content(
    val app: App,
    val similarApps: List<App>,
    val installState: InstallState = InstallState.NotInstalled,
    val isInWishlist: Boolean = false
) : AppDetailUiState

val uiState = combine(
    repository.observeApp(appId),
    repository.apps,
    _installState
) { app, apps, installState ->
    when {
        app != null -> AppDetailUiState.Content(
            app = app,
            similarApps = apps.similarApps(app.id),
            installState = installState,
            isInWishlist = app.isInWishlist
        )
        apps.isEmpty() -> AppDetailUiState.Loading
        else -> AppDetailUiState.NotFound
    }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDetailUiState.Loading)

fun toggleWishlist() {
    viewModelScope.launch { repository.toggleWishlist(appId) }
}
```

Разовый запрос в состояние больше не пишет: после `toggleWishlist` Room
инвалидирует `SELECT` и присылает новую строку в `observeApp`, `combine`
пересобирает `Content`, иконка перерисовывается сама. Пустой каталог при
холодном старте даёт `Loading`, а не «Приложение не найдено».

### 6. Иконка в тулбаре

`presentation/detail/AppDetailScreen.kt`: `DetailToolBar` получил
`isInWishlist` и колбэк `onWishlistClick`; в `actions` — `IconButton`:

- не добавлено → `Icons.Default.StarBorder`, contentDescription «Добавить в вишлист»;
- добавлено → `Icons.Default.Star`, contentDescription «Убрать из вишлиста».

Клик из `AppDetailRoute` уходит в `viewModel::toggleWishlist`. Иконка скрыта,
пока карточка в `Loading`/`NotFound`. Новые строки — `wishlist_add`/`wishlist_remove`
в `strings.xml`.

## Проверка

| Проверка | Результат |
|---|---|
| `:app:testDebugUnitTest` | 69 тестов, 0 падений |
| `:app:connectedDebugAndroidTest` (Medium_Phone_API_35) | 33 теста, 0 падений |
| Карточка QuickNote → иконка «Добавить в вишлист» | звезда стала закрашенной, contentDescription «Убрать из вишлиста» |
| Назад → снова открыть QuickNote | состояние сохранено |
| `force-stop` + повторный запуск → QuickNote | по-прежнему «Убрать из вишлиста» — отметка пришла из Room |
| `run-as ru.mobile.startapp` + sqlite по `databases/startapp.db` (с `-wal`/`-shm`) | колонка `isInWishlist INTEGER NOT NULL`; `select id,name,isInWishlist from apps where isInWishlist=1` вернул QuickNote; всего 41 строка |

## Как воспроизвести

```bash
cd C:/Kotlin/StartApp
export JAVA_HOME='C:\Users\skute\.jdks\corretto-21.0.4'
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mobile.startapp/.MainActivity
# открыть карточку → нажать звезду в тулбаре → назад → открыть снова
```

## Нюансы

1. Room 2.8.5 подключён через **KSP** (`2.2.10-2.0.2`); Hilt остался на
   `com.android.legacy-kapt` — обе кодогенерации в одном модуле работают.
2. В `gradle.properties` нужен `android.disallowKotlinSourceSets=false`:
   KSP регистрирует исходники через `kotlin.sourceSets`, AGP 9 с built-in Kotlin
   это запрещает без флага.
3. `@Upsert` не используется: он перезаписывает все колонки, и сетевой
   `isInWishlist = false` стёр бы отметку. Вставка IGNORE + UPDATE контента
   сохраняет пользовательское значение.
4. `deleteMissing` вызывается только для непустого ответа сервера:
   в SQLite `NOT IN ()` истинно для всех строк и удалил бы весь кеш.
5. `AppDatabase` — `version = 2` и `fallbackToDestructiveMigration(true)`:
   предыдущая (отменённая) ветка успела создать version-1 БД с колонкой
   `isFavorite`, и без пересоздания таблицы Room упал бы на проверке схемы.
   Проверено установкой поверх: `PRAGMA user_version = 2`, каталог
   перезагрузился, вишлист работает.
