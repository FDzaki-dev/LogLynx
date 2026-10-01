# Changelog

## v4 — 2026-10-01
- Hemat baterai: sinkronisasi log ke layar berhenti total saat aplikasi di background (tanpa wake-up 200 ms dan salinan buffer 50.000 baris); otomatis lanjut saat aplikasi dibuka lagi.
- UI: toolbar kini bisa digulir dan tingginya dibatasi maksimal setengah area tersedia, sehingga tidak terpotong di landscape/keyboard terbuka dan daftar log tetap terlihat.
- Crash rilis: nama file & nomor baris dipertahankan di R8, sehingga stack trace di Documents/LogLynx bisa di-retrace dengan mapping.txt.

## v3 — 2026-10-01
- Distribusi: APK ter-sign kini dirilis otomatis lewat GitHub Releases (tag v1.0.<nomor build>) lengkap dengan file SHA-256. Versi naik tiap build sehingga APK baru selalu bisa meng-update yang lama.

## v2 — 2026-10-01
- UI: toolbar tidak lagi terpotong (level F + tombol Freeze/Clear sebelumnya keluar layar), kontras teks diperbaiki, baris log ringkas satu paragraf berwarna per level, tap = detail, tekan lama = salin, auto-scroll berhenti saat user menggeser list + tombol 'Ke bawah', penghitung baris, empty state.

## v1 — 2026-10-01
- Initial setup LogLynx sesuai Konfigurasi_Logcat_Reader_Totalitas.md: Compose UI, LogcatEngine (Shizuku + fallback lokal), filter level/regex, CrashLogger, CI GitHub Actions.
