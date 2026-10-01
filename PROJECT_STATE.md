# PROJECT_STATE — LogLynx

[BRANDING_NAME]: LogLynx
[TERMUX_ROOT]: LogLynx

## Identitas
- Aplikasi Android logcat reader (Compose, Shizuku). Sumber konfigurasi: Konfigurasi_Logcat_Reader_Totalitas.md
- Package/namespace: com.pro.logcatreader (sesuai lampiran). Label: LogLynx
- minSdk 26, compile/target 35, Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9 (CI via setup-gradle, tanpa wrapper)
- Release: R8 + shrinkResources ON; signing dari env KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD + release.keystore (GitHub Secrets, Box B)
- Versi: versionCode = GITHUB_RUN_NUMBER, versionName = 1.0.<run> di CI (lokal: 1 / 1.0.0-dev); basis "1.0" = appVersionBase di app/build.gradle.kts

## Struktur
- model/LogModel.kt (LogLevel, LogLine, LogcatParser)
- engine/LogcatEngine.kt (stream logcat -v threadtime, circular buffer 50000, sync UI 200ms)
- viewmodel/LogcatViewModel.kt (filter level + teks/regex, listener Shizuku)
- MainActivity.kt (UI Compose Darcula), LogLynxApp.kt + CrashLogger.kt (crash -> Documents/LogLynx via MediaStore)

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

[RESUME POINT]: Jalur unduhan GitHub Release (build.yml + versi dinamis di app/build.gradle.kts) -> kode selesai, BELUM dijalankan di GitHub; UI v2 (MainActivity.kt) juga belum diverifikasi di perangkat -> Push via Daily Update, cek tab Actions lalu Releases: tag v1.0.<run>, aset LogLynx-v*.apk + .sha256, notes terisi; install APK dari Releases dan cek UI v2. Jika job gagal mulai dari step 'Verifikasi tanda tangan & siapkan aset rilis' (path apksigner/aapt2 di $ANDROID_HOME/build-tools) atau 'Publikasi GitHub Release' (izin contents: write / setelan Actions permissions repo)
