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

## Log kegagalan CI

Jika build gagal/dibatalkan, workflow mengunggah artefak diagnosis (disimpan 30 hari):

`LogLynx-failure-run<NNNN>-a<attempt>-<fase>-<sha7>`

- `NNNN` = nomor run (naik otomatis, dipad 4 digit agar urut), `a<attempt>` naik tiap re-run, `<fase>` = task Gradle yang gagal (mis. `compileReleaseKotlin`) atau `keystore` / `signing-verify` / `release-publish` / `setup`.
- Isi: `diagnosis.md`, `errors.txt`, `gradle-build.log` (secret disamarkan), `environment.txt`, dan `reports/` / `r8/` / `kotlin-daemon/` bila ada.
- Ringkasan error + tautan artefak juga tampil di tab Summary run; error kompilasi Kotlin muncul sebagai anotasi pada baris kode.

Unduh run gagal terakhir dari Termux:

```bash
cd ~/projects/LogLynx && gh run download "$(gh run list --workflow build.yml --status failure --limit 1 --json databaseId --jq '.[0].databaseId')" --pattern 'LogLynx-failure-*' --dir ~/storage/downloads
```

Logika pengumpulan ada di `.github/scripts/collect-failure-logs.sh`.
