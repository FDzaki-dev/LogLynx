# Changelog

## v6 — 2026-10-01
- Perbaikan build: tombol gulir atas/bawah (FAB) dipindah ke composable tersendiri sehingga kompilasi `MainActivity.kt` tidak lagi gagal (AnimatedVisibility di dalam Column/Box). Tampilan dan perilaku tidak berubah.
- CI: jika build gagal, log diagnosis otomatis diunggah sebagai artefak bernama `LogLynx-failure-run<nomor>-a<attempt>-<fase>-<sha7>` (nomor naik otomatis, tidak pernah bentrok saat re-run), berisi ringkasan, daftar error, log Gradle penuh (secret disamarkan), dan info lingkungan. Error kompilasi juga muncul sebagai anotasi di halaman run.

## v5 — 2026-10-01
- Tampilan baru terinspirasi LogcatReader: app bar teal berisi judul + jumlah baris, tombol Cari, Jeda/Lanjut auto-scroll, dan menu (tampilan ringkas, hapus log). Mengikuti tema terang/gelap sistem.
- Baris log: badge prioritas berwarna di kiri, tag, pesan, lalu tanggal/jam/PID/TID. Mode ringkas satu baris (tap = buka detail). Tint merah/oranye untuk E/F/W tetap ada.
- Pencarian: search bar menggantikan app bar, kecocokan disorot kuning, tombol Regex (teks merah jika regex tidak valid). Level minimum jadi baris chip yang bisa digulir.
- Tekan lama baris = menu bawah: salin baris, salin pesan, filter tag ini. Scrollbar tipis dan tombol gulir atas/bawah.
- Dihapus: toolbar 3 baris lama (digantikan app bar + chip level).

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
