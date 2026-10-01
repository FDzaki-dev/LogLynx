# PROJECT_STATE — LogLynx

[BRANDING_NAME]: LogLynx
[TERMUX_ROOT]: LogLynx

## Identitas
- Aplikasi Android logcat reader (Compose, Shizuku). Sumber konfigurasi: Konfigurasi_Logcat_Reader_Totalitas.md
- Package/namespace: com.pro.logcatreader (sesuai lampiran). Label: LogLynx
- minSdk 26, compile/target 35, Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9 (CI via setup-gradle, tanpa wrapper)
- Release: R8 + shrinkResources ON; signing dari env KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD + release.keystore (GitHub Secrets, Box B)

## Struktur
- model/LogModel.kt (LogLevel, LogLine, LogcatParser)
- engine/LogcatEngine.kt (stream logcat -v threadtime, circular buffer 50000, sync UI 200ms)
- viewmodel/LogcatViewModel.kt (filter level + teks/regex, listener Shizuku)
- MainActivity.kt (UI Compose Darcula), LogLynxApp.kt + CrashLogger.kt (crash -> Documents/LogLynx via MediaStore)

## Deviasi dari lampiran (disengaja)
- Manifest: tanpa atribut package (pakai namespace Gradle); ShizukuProvider exported=true + permission INTERACT_ACROSS_USERS_FULL (wajib agar binder Shizuku masuk)
- Dependency tambahan agar compile: activity-compose 1.9.3, lifecycle-runtime-compose 2.8.7
- Engine: Shizuku.newProcess via reflection (deprecated/private di 13.1.x); counter ukuran buffer (size deque O(n)); emit hanya jika ada data baru; proses logcat di-destroy saat cancel; buffer dibersihkan tiap start
- ViewModel: regex dikompilasi sekali per emisi; filter di Dispatchers.Default; request izin Shizuku + restart stream saat izin/binder siap
- UI: collectAsStateWithLifecycle, rememberSaveable, WindowInsets (systemBars + ime), auto-scroll keyed id baris terakhir + scrollToItem

[RESUME POINT]: Initial setup LogLynx v1 -> ZIP tersusun sesuai konfigurasi lampiran, BELUM pernah di-build/di-run -> Jalankan Box A + Box B, cek build GitHub Actions pertama; jika gagal, perbaiki hanya file/baris yang disebut error compile (kandidat: LogcatEngine.newShizukuProcess/openProcess, LogcatViewModel binderListener & permissionListener)
