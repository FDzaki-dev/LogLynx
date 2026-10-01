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
- MainActivity.kt (UI Compose Darcula; onStart/onStop -> ViewModel.onUiStart/onUiStop via by viewModels()), LogLynxApp.kt + CrashLogger.kt (crash -> Documents/LogLynx via MediaStore)

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
- v4 (guard): toolbar MainActivity = Modifier.layout (maks 50% tinggi tersedia) + verticalScroll; R8 keep SourceFile/LineNumberTable + renamesourcefileattribute (proguard-rules.pro); .gitignore + /.kotlin/
- Ditunda sengaja (v4): upgrade konfigurasi terkopel ala LogcatReader-2.6.1 (AGP/Gradle/Kotlin/SDK 36) -> berisiko tanpa build lokal, lihat RESUME POINT

[RESUME POINT]: Guard baterai (LogcatEngine.startStreaming loop uiActive + MainActivity.onStart/onStop) + toolbar scroll-cap (MainActivity.LogcatScreen, Modifier.layout/verticalScroll) + R8 keep line numbers (proguard-rules.pro) -> kode selesai, hanya review statik; BELUM di-build (sandbox tanpa jaringan/SDK), BELUM diverifikasi di perangkat; hasil uji perangkat untuk UI v2/rilis v3 belum dikonfirmasi user -> Push via Daily Update, pastikan Actions hijau lalu Releases (tag v1.0.<run>); uji di perangkat: (1) pindah ke app lain ~30 dtk lalu kembali = log langsung terisi & bertambah, (2) landscape + keyboard saat mengetik pencarian = toolbar tidak terpotong & daftar log tampak, (3) paksa crash di build rilis = baris di Documents/LogLynx memuat nomor baris. Jika compile gagal mulai dari MainActivity.kt (import layout/verticalScroll/rememberScrollState/viewModels), lalu LogcatEngine.startStreaming (uiActive.first). Setelah hijau: upgrade konfigurasi ala LogcatReader-2.6.1 dalam SATU batch terpisah tanpa fitur lain: AGP 9.1.0 + gradle-version 9.3.1 di build.yml + Kotlin 2.3.20 (plugin compose ikut) + compileSdk/targetSdk 36 + activity-compose 1.13.0, lifecycle 2.10.0, coroutines 1.10.2, Compose BOM 2026.03.01; kotlinOptions -> kotlin { compilerOptions { jvmTarget } }; gradle.properties android.builtInKotlin=false & android.newDsl=false (meniru upstream). Verifikasi tiap versi di build.yml run sebelum lanjut.
