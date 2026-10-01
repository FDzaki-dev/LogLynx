# LogLynx

Aplikasi Android logcat reader: real-time, filter level + teks/regex, auto-scroll lock, buffer sirkular 50.000 baris.

- Log sistem penuh: jalankan Shizuku lalu beri izin ke LogLynx. Tanpa Shizuku, hanya log aplikasi sendiri.
- Crash log tersimpan di Documents/LogLynx.
- Build & rilis: GitHub Actions (`.github/workflows/build.yml`), APK di-sign dari GitHub Secrets.

Detail teknis & status: lihat `PROJECT_STATE.md`.

## Unduh

APK resmi hanya dari halaman [Releases](../../releases/latest) (aset `LogLynx-v<versi>.apk` + `.sha256`).

Dari Termux (di folder proyek):

```bash
cd ~/projects/LogLynx && gh release download --pattern 'LogLynx-*.apk' --dir ~/storage/downloads --clobber
```

Verifikasi (opsional): `sha256sum LogLynx-v<versi>.apk` harus sama dengan nilai SHA-256 di catatan rilis. Butuh Android 8.0+.

## Alur rilis

Push ke `main` (selain perubahan `*.md`) memicu build. Jika GitHub Secrets signing ada, APK diverifikasi (`apksigner`), lalu dipublikasikan sebagai Release `v1.0.<nomor build>`. Tanpa Secrets, build tetap jalan tetapi tidak merilis APK unsigned. Catatan rilis diambil dari entri teratas `CHANGELOG.md`.
