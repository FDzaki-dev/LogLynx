# PROJECT_STATE — LogLynx

[BRANDING_NAME]: LogLynx
[TERMUX_ROOT]: LogLynx

## Identitas
- Aplikasi Android logcat reader (Compose, Shizuku). Sumber konfigurasi: Konfigurasi_Logcat_Reader_Totalitas.md
- Referensi pembanding: LogcatReader-2.6.1.zip (proyek upstream; acuan konfigurasi build/fitur, BUKAN untuk disalin utuh: upstream memakai foreground service yang dilarang guard)
- Package/namespace: com.pro.logcatreader (sesuai lampiran). Label: LogLynx
- minSdk 26, compile/target 35, Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9 (CI via setup-gradle, tanpa wrapper)
- Release: R8 + shrinkResources ON; signing dari env KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD + release.keystore (GitHub Secrets, Box B)
- Versi: versionCode = GITHUB_RUN_NUMBER, versionName = 1.0.<run> di CI (lokal: 1 / 1.0.0-dev); basis "1.0" = appVersionBase di app/build.gradle.kts

## Struktur
- .github/scripts/collect-failure-logs.sh (pengumpul diagnosis CI gagal; dipanggil build.yml)
- model/LogModel.kt (LogLevel, LogLine, LogcatParser)
- engine/LogcatEngine.kt (stream logcat -v threadtime, circular buffer 50000, sync UI 200ms; sync ditangguhkan total saat UI background via uiActive/setUiActive)
- viewmodel/LogcatViewModel.kt (filter level + teks/regex, listener Shizuku, onUiStart/onUiStop)
- MainActivity.kt (Scaffold + TopAppBar/SearchTopBar, LevelFilterRow, FAB gulir, LogActionsSheet; onStart/onStop -> ViewModel.onUiStart/onUiStop via by viewModels())
- ui/theme/Theme.kt (LogLynxTheme: skema teal ala LogcatReader, gelap = netral Darcula; badgeColor per level), ui/LogRow.kt (LogListEntry/LogItemRow, logScrollbar), LogLynxApp.kt + CrashLogger.kt (crash -> Documents/LogLynx via MediaStore)

## CI / Release (build.yml)
- Trigger: push ke main (paths-ignore *.md, .gitignore) + workflow_dispatch; permissions contents: write; concurrency tanpa cancel
- Alur: keystore dari Secrets -> gradle assembleRelease -> apksigner verify -> baca versionName via aapt2 -> aset LogLynx-v<versi>.apk + .sha256 -> gh release create v<versi> (notes = entri teratas CHANGELOG + SHA-256)
- Tanpa Secrets: build tetap jalan, rilis dilewati (APK unsigned tidak bisa di-install). Re-run run yang sama: aset ditimpa, bukan release ganda
- Artifact tambahan: LogLynx-release (APK, 14 hari), LogLynx-mapping (mapping R8, 30 hari)
- Jalur artefak kegagalan (v6): step build memakai `id: build` + `tee build-logs/gradle-assembleRelease.log` (pipefail); step publish `id: publish`. Saat `failure() || cancelled()`: `bash .github/scripts/collect-failure-logs.sh` (continue-on-error) -> upload-artifact@v4 bernama `LogLynx-failure-run<NNNN>-a<attempt>-<fase>-<sha7>` (retention 30 hari) -> tautan di GITHUB_STEP_SUMMARY. Fase = task Gradle gagal / keystore / signing-verify / release-publish / setup. Secret (KEYSTORE_*/KEY_*) + pola token disamarkan via python3; tanpa python3 artefak dilewati (fail-safe). Error `e: file://...` jadi anotasi `::error` (maks 10).
- Unduh di HP: tab Releases, atau Termux: gh release download --pattern 'LogLynx-*.apk' (lihat README)

## Deviasi dari lampiran (disengaja)
- Manifest: tanpa atribut package (pakai namespace Gradle); ShizukuProvider exported=true + permission INTERACT_ACROSS_USERS_FULL (wajib agar binder Shizuku masuk)
- Dependency tambahan agar compile: activity-compose 1.9.3, lifecycle-runtime-compose 2.8.7
- Engine: Shizuku.newProcess via reflection (deprecated/private di 13.1.x); counter ukuran buffer (size deque O(n)); emit hanya jika ada data baru; proses logcat di-destroy saat cancel; buffer dibersihkan tiap start
- ViewModel: regex dikompilasi sekali per emisi; filter di Dispatchers.Default; request izin Shizuku + restart stream saat izin/binder siap
- UI (v2, MainActivity.kt): toolbar 3 baris tanpa tinggi fixed (bobot rata, tidak terpotong), warna konten eksplisit (kontras), BasicTextField kompak + tombol hapus,
  chip level (terpilih = warna level), baris log 1 paragraf: jam | badge level | tag: pesan, tint merah/oranye untuk E/F/W, tap = detail (tanggal/PID/TID), tekan lama = salin,
  auto-scroll berhenti saat user drag + tombol 'Ke bawah', penghitung baris, empty state; tetap: collectAsStateWithLifecycle, rememberSaveable, WindowInsets (systemBars + ime), auto-scroll keyed id baris terakhir + scrollToItem
- v4 (guard): R8 keep SourceFile/LineNumberTable + renamesourcefileattribute (proguard-rules.pro); .gitignore + /.kotlin/. Toolbar scroll-cap v4 DIGANTIKAN UI v3 (tidak ada lagi toolbar tinggi)
- UI v3 (v5 batch, meniru LogcatReader-2.6.1 ui/): tiru = app bar primaryContainer + judul/subjudul, search bar pengganti app bar + Regex, badge prioritas kiri + tag/pesan/meta, mode ringkas (tag 20% | pesan 80%, tap expand), sorot hasil cari, bottom sheet aksi baris, scrollbar auto-hide, FAB gulir atas/bawah, palet teal + shapes 4/8/12 + warna prioritas identik.
  Modifikasi LogLynx: gelap = netral Darcula (bukan hitam kehijauan), tint E/F/W dipertahankan, level minimum = baris chip gulir horizontal, 'Jeda' = jeda auto-scroll (bukan jeda tangkap log), tap baris hanya expand di mode ringkas, filter tag ini lewat search query, tanpa dynamic color/Settings/Filters/Saved logs/foreground service (di luar scope + dilarang guard), font Monospace sistem (Roboto Mono tidak dibundel), insets via Scaffold contentWindowInsets=safeDrawing.
- v6 (fix compile): `ColumnScope.AnimatedVisibility` tidak bisa dipanggil implisit di dalam Box yang ada di Column (DslMarker). FAB gulir dipindah ke `private fun ScrollFabs(...)` top-level di MainActivity.kt; dipanggil dari Box di LogcatScreen. Perilaku/tampilan identik.
- Dependency tambahan UI v3: androidx.compose.material:material-icons-core (BOM)
- Ditunda sengaja (v4): upgrade konfigurasi terkopel ala LogcatReader-2.6.1 (AGP/Gradle/Kotlin/SDK 36) -> berisiko tanpa build lokal, lihat RESUME POINT

[RESUME POINT]: Fix compile `MainActivity.kt` (LogcatScreen -> ScrollFabs, sebelumnya error baris 247/255 AnimatedVisibility) + jalur artefak log kegagalan (build.yml step id build/publish/faillog/upload-faillog + .github/scripts/collect-failure-logs.sh) -> skrip diuji lokal terhadap log CI asli (run 99831735530: fase compileReleaseKotlin, anotasi + redaksi secret OK), YAML tervalidasi; Kotlin BELUM di-build (sandbox tanpa jaringan/SDK) dan belum diuji di perangkat; UI v3 belum dikonfirmasi user -> Push via Daily Update, tunggu Actions: (a) hijau -> cek Releases tag v1.0.<run> lalu uji UI v3 di perangkat: terang & gelap (app bar teal, badge V/D/I/W/E/F), Cari (keyboard terbuka = daftar tetap terlihat, kecocokan kuning, Regex salah = teks merah muda, back = filter hilang), tekan lama baris = sheet salin/filter tag, menu > Tampilan ringkas, geser list = Jeda jadi Play + FAB atas/bawah muncul (ScrollFabs), rotasi mempertahankan filter/ringkas/jeda, background ~30 dtk lalu kembali = log terisi; (b) merah -> unduh artefak `LogLynx-failure-run*` (gh run download, lihat README), baca `errors.txt`; jika masih compile, mulai dari MainActivity.kt (ikon material-icons-core, ListItem/FilterChip/ModalBottomSheet, ImageVector.path PauseIcon) lalu ui/LogRow.kt (logScrollbar, layoutInfo); jangan tambah material-icons-extended. Jika artefak kegagalan sendiri bermasalah: isolasi di step `faillog` (collect-failure-logs.sh) atau `upload-faillog`, jangan sentuh step build. Setelah hijau, opsional (satu batch tiap item): font Roboto Mono bundel (res/font), toggle sembunyikan kolom meta, atau upgrade konfigurasi build ala LogcatReader (AGP 9.1.0 + gradle-version 9.3.1 di build.yml + Kotlin 2.3.20 + SDK 36 + activity-compose 1.13.0/lifecycle 2.10.0/coroutines 1.10.2/BOM 2026.03.01; kotlinOptions -> kotlin { compilerOptions { jvmTarget } }; gradle.properties android.builtInKotlin=false & android.newDsl=false), verifikasi tiap versi di build.yml run.
