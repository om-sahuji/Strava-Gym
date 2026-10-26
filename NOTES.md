# Runs feature — implementation notes

## Phase 0 — Repo findings (read-only)

- **Language / toolkit**: 100 % Kotlin, single `:app` module. UI is **Jetpack Compose** with
  **Material 2** (`androidx.compose.material:1.4.3`) + Material 3 dependency available but the app
  uses Material 2 (`Scaffold`, `TopBar`, `ListItem`, `Card`, `SwipeToDismiss`, `TopAppBar`).
- **Build**: AGP 8.1.0, Gradle 8.2.1, Kotlin 1.9.0, KSP (Room), JDK 17, compileSdk 33,
  buildTools 34.0.0, minSdk 21, targetSdk 33.
- **DB**: Room 2.5.2, single `AppDatabase` (`workout_routines_database`), **version 43**, schema
  export enabled into `app/schemas/…AppDatabase/`. Explicit `Migration` objects are registered in
  `di/Modules.kt` (`addMigrations(MIGRATION_36_37, … 42_43)`), no destructive migration fallback.
  Entities: `Exercise`, `Routine`, `Workout` (+ `WorkoutSet`, `WorkoutSetGroup`), all in
  `data/domain`, all in `data/dao` with `@Dao` interfaces, repositories in `data/Repository.kt`.
- **DI**: Koin (`di/Modules.kt`): `single` DB, `factory` DAOs/repositories, `viewModel` builders.
  ViewModels are obtained in Compose with `org.koin.androidx.compose.getViewModel`.
- **Navigation**: Accompanist animated `NavHost` + `ModalBottomSheetLayout`, routes are
  `enum class Screen` names (`ui/NavGraph.kt`). Bottom navigation has 3 tabs
  (Routines / Exercises / Insights) defined in `ui/main/BottomBar.kt`; a bottom bar is only shown
  on those tab routes.
- **Data shape**: a gym workout is a `Workout(routineId, startTime, endTime)` with
  `WorkoutSetGroup`s and `WorkoutSet`s (reps/weight/time/distance). Settings live in DataStore
  (`data/Preferences.kt`, `AppPrefs` sealed class).
- **Existing charting**: hand-drawn Compose `Canvas` (`ui/components/SimpleLineChart.kt`), no chart
  library.

## Decisions

- **Where the Runs UI fits**: a **4th bottom-navigation tab "Runs"** (`BottomNavItem.Runs`,
  `Icons.Default.DirectionsRun` from the already-included material-icons-extended), plus a
  `runDetail/{stravaId}` route. Nothing in the existing tabs changes; Strava setup is a new entry
  in the existing Settings screen (`Screen.stravaSettings`).
- **HTTP / JSON**: no new dependency. `HttpURLConnection` + `kotlinx.serialization` (already used by
  the Room `Converters`), so the JSON mapping is unit-testable on the JVM.
- **Secret storage**: no new dependency; a small `StravaCredentials` store encrypts the values with
  an AES/GCM key from the Android Keystore (API 23+). See limitations below.
- **DB change**: additive table `run_table`, DB version 43 → 44 with `MIGRATION_43_44`
  (`CREATE TABLE`), no existing table touched.

## Build environment (this sandbox)

No JDK/Android SDK pre-installed and no root. Toolchain installed under `$HOME/tools`:
Temurin JDK 17 + Android `cmdline-tools` with `platforms;android-33` and `build-tools;34.0.0`.
`local.properties` (git-ignored) points at that SDK. Build command:

```bash
export JAVA_HOME=$HOME/tools/jdk-17.0.21+9
export ANDROID_SDK_ROOT=$HOME/tools/android-sdk
./gradlew assembleDebug -Dorg.gradle.jvmargs="-Xmx1400m -XX:MaxMetaspaceSize=512m"
```
