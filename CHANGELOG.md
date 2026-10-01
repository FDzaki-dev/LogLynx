# Changelog

## v3 — 2026-10-01
- Distribusi: APK ter-sign kini dirilis otomatis lewat GitHub Releases (tag v1.0.<nomor build>) lengkap dengan file SHA-256. Versi naik tiap build sehingga APK baru selalu bisa meng-update yang lama.

## v2 — 2026-10-01
- UI: toolbar tidak lagi terpotong (level F + tombol Freeze/Clear sebelumnya keluar layar), kontras teks diperbaiki, baris log ringkas satu paragraf berwarna per level, tap = detail, tekan lama = salin, auto-scroll berhenti saat user menggeser list + tombol 'Ke bawah', penghitung baris, empty state.

## v1 — 2026-10-01
- Initial setup LogLynx sesuai Konfigurasi_Logcat_Reader_Totalitas.md: Compose UI, LogcatEngine (Shizuku + fallback lokal), filter level/regex, CrashLogger, CI GitHub Actions.
