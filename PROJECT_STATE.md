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
- Dependency tambahan UI v3: androidx.compose.material:material-icons-core (BOM)
- Ditunda sengaja (v4): upgrade konfigurasi terkopel ala LogcatReader-2.6.1 (AGP/Gradle/Kotlin/SDK 36) -> berisiko tanpa build lokal, lihat RESUME POINT

[RESUME POINT]: UI v3 ala LogcatReader (MainActivity.kt LogcatScreen/LogTopBar/SearchTopBar/LevelFilterRow/LogActionsSheet, ui/LogRow.kt LogItemRow/logScrollbar, ui/theme/Theme.kt) + guard baterai v4 (LogcatEngine.startStreaming uiActive) -> kode selesai, hanya review statik; BELUM di-build (sandbox tanpa jaringan/SDK) dan BELUM diverifikasi di perangkat; hasil uji UI v2/rilis v3 belum dikonfirmasi user -> Push via Daily Update, pastikan Actions hijau lalu Releases (tag v1.0.<run>); uji di perangkat: (1) terang & gelap: app bar teal, badge V/D/I/W/E/F, status bar tidak menimpa judul, (2) Cari: keyboard terbuka = daftar log tetap terlihat, kecocokan kuning, Regex salah = teks merah muda, tutup/back = filter hilang, (3) tekan lama baris = sheet salin/filter tag, (4) menu > Tampilan ringkas: tap baris = expand, (5) geser list = tombol Jeda jadi Play + FAB atas/bawah muncul, (6) rotasi: filter/mode ringkas/jeda bertahan, (7) background ~30 dtk lalu kembali = log terisi. Jika compile gagal mulai dari MainActivity.kt (ikon material-icons-core, ListItem/FilterChip/ModalBottomSheet, ImageVector.path PauseIcon), lalu ui/LogRow.kt (logScrollbar, layoutInfo). Jika ikon tidak ditemukan: ganti dengan teks/vektor sendiri, jangan tambah material-icons-extended. Setelah hijau, opsional (satu batch tiap item, jangan dicampur): font Roboto Mono bundel (res/font), toggle sembunyikan kolom meta, atau upgrade konfigurasi build ala LogcatReader (AGP 9.1.0 + gradle-version 9.3.1 di build.yml + Kotlin 2.3.20 + SDK 36 + activity-compose 1.13.0/lifecycle 2.10.0/coroutines 1.10.2/BOM 2026.03.01; kotlinOptions -> kotlin { compilerOptions { jvmTarget } }; gradle.properties android.builtInKotlin=false & android.newDsl=false), verifikasi tiap versi di build.yml run.
