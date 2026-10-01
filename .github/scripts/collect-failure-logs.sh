#!/usr/bin/env bash
# collect-failure-logs.sh — kumpulkan diagnosis build yang gagal ke satu folder siap-unggah.
#
# Dipanggil dari build.yml (step "Kumpulkan log kegagalan") hanya saat job gagal/dibatalkan.
# Best-effort: tanpa `set -e`, satu langkah gagal tidak boleh membatalkan seluruh pengumpulan.
#
# Output (GITHUB_OUTPUT):
#   name  = nama artefak dinamis  <APP>-failure-run<NNNN>-a<attempt>-<fase>-<sha7>
#   dir   = folder yang diunggah (isi artefak, tanpa folder pembungkus)
#   phase = fase/task yang gagal
# Nama unik per (run, attempt, fase): re-run job yang sama tidak bentrok di upload-artifact@v4,
# dan urut alfabet = urut kronologis (nomor run dipad 4 digit).
set -uo pipefail

APP="${APP_NAME:-LogLynx}"
GRADLE_LOG="${GRADLE_LOG:-build-logs/gradle-assembleRelease.log}"
WORKSPACE="${GITHUB_WORKSPACE:-$PWD}"
TMP_ROOT="${RUNNER_TEMP:-/tmp}"
OUT_FILE="${GITHUB_OUTPUT:-/dev/null}"
SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-/dev/null}"

slug() { printf '%s' "$1" | tr -c 'A-Za-z0-9._-' '-' | sed -E 's/-+/-/g; s/^-+|-+$//g' | cut -c1-40; }
strip_ts() { sed -E 's/^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9:.]+Z //'; }

# python3 dibutuhkan untuk redaksi secret. Tanpa itu, JANGAN unggah log mentah.
if ! command -v python3 >/dev/null 2>&1; then
  echo "::warning::python3 tidak tersedia — artefak log kegagalan dilewati (mencegah kebocoran secret)."
  exit 0
fi

# ---------- Fase yang gagal ----------
phase="setup"
for pair in "keystore:${OUTCOME_KEYSTORE:-}" "build:${OUTCOME_BUILD:-}" \
            "signing-verify:${OUTCOME_ASSETS:-}" "release-publish:${OUTCOME_PUBLISH:-}"; do
  case "${pair#*:}" in
    failure|cancelled) phase="${pair%%:*}"; break ;;
  esac
done

failed_task=""
if [ -s "$GRADLE_LOG" ]; then
  failed_task="$(strip_ts < "$GRADLE_LOG" \
    | sed -nE "s/.*Execution failed for task '([^']+)'.*/\1/p" | head -n 1)"
  failed_task="${failed_task##*:}"
fi
if [ "$phase" = "build" ] && [ -n "$failed_task" ]; then
  phase="$failed_task"            # mis. compileReleaseKotlin, minifyReleaseWithR8
fi
phase="$(slug "$phase")"
[ -n "$phase" ] || phase="unknown"

# ---------- Nama dinamis ----------
run_no="${RUN_NUMBER:-0}"; [[ "$run_no" =~ ^[0-9]+$ ]] || run_no=0
attempt="${RUN_ATTEMPT:-1}"; [[ "$attempt" =~ ^[0-9]+$ ]] || attempt=1
sha7="$(printf '%s' "${COMMIT_SHA:-0000000}" | cut -c1-7)"
NAME="${APP}-failure-run$(printf '%04d' "$run_no")-a${attempt}-${phase}-${sha7}"
OUT="${TMP_ROOT}/ci-failure/${NAME}"
rm -rf "$OUT"; mkdir -p "$OUT"

# ---------- Bahan mentah ----------
if [ -s "$GRADLE_LOG" ]; then
  cp "$GRADLE_LOG" "$OUT/gradle-build.log"
fi

ERRS="$OUT/errors.txt"
{
  if [ -s "$GRADLE_LOG" ]; then
    echo "## Error kompilasi Kotlin/Java"
    strip_ts < "$GRADLE_LOG" | grep -E '^(e|error): ' || echo "(tidak ada)"
    echo
    echo "## Gradle: What went wrong"
    strip_ts < "$GRADLE_LOG" | grep -E -A4 '^\* What went wrong:' | head -n 40 || echo "(tidak ada)"
    echo
    echo "## R8 / AAPT / lainnya"
    strip_ts < "$GRADLE_LOG" | grep -E '(^|[[:space:]])(R8:|AAPT:|ERROR:|Missing class)' | head -n 40 || true
  else
    echo "Log Gradle tidak ada (kegagalan terjadi sebelum/di luar langkah build, fase: ${phase})."
  fi
} > "$ERRS" 2>/dev/null

# Laporan Gradle (HTML/XML), batas 5 MB per file
if [ -d app/build/reports ]; then
  mkdir -p "$OUT/reports"
  (cd app/build/reports && find . -type f -size -5M -print0 | xargs -0 -r -I{} cp --parents {} "$OUT/reports/") 2>/dev/null
fi
# Diagnosis R8 (bukan mapping.txt: itu sudah artefak terpisah)
for f in missing_rules.txt configuration.txt; do
  [ -f "app/build/outputs/mapping/release/$f" ] && { mkdir -p "$OUT/r8"; cp "app/build/outputs/mapping/release/$f" "$OUT/r8/"; }
done
# Log crash daemon Kotlin / JVM
if [ -d .kotlin/errors ]; then mkdir -p "$OUT/kotlin-daemon"; cp -r .kotlin/errors/. "$OUT/kotlin-daemon/" 2>/dev/null; fi
find . -maxdepth 3 -name 'hs_err_pid*.log' -size -5M -exec cp {} "$OUT/" \; 2>/dev/null

# Lingkungan (whitelist — TIDAK pernah dump `env`, agar secret tidak ikut)
{
  echo "collected_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "runner_os=${RUNNER_OS:-?} arch=${RUNNER_ARCH:-?} image=${ImageOS:-?}/${ImageVersion:-?}"
  echo "uname=$(uname -srm)"
  echo "--- java"; java -version 2>&1 | head -n 3
  echo "--- gradle"; timeout 90 gradle --version 2>&1 | sed -n '1,14p'
  echo "--- disk"; df -h "$WORKSPACE" 2>/dev/null | tail -n 2
  echo "--- memory"; free -m 2>/dev/null | head -n 2
} > "$OUT/environment.txt" 2>&1

# ---------- diagnosis.md ----------
RUN_URL="${SERVER_URL:-https://github.com}/${REPOSITORY:-}/actions/runs/${RUN_ID:-0}"
{
  echo "# ${APP} — laporan kegagalan CI"
  echo
  echo "| Field | Nilai |"
  echo "|---|---|"
  echo "| Artefak | \`${NAME}\` |"
  echo "| Fase gagal | \`${phase}\` |"
  echo "| Run | [#${run_no} (attempt ${attempt})](${RUN_URL}) |"
  echo "| Commit | \`${COMMIT_SHA:-?}\` |"
  echo "| Branch / event | ${REF_NAME:-?} / ${EVENT_NAME:-?} |"
  echo "| Pemicu | ${ACTOR:-?} |"
  echo "| Dikumpulkan (UTC) | $(date -u +%Y-%m-%dT%H:%M:%SZ) |"
  echo
  echo "## Ringkasan error"
  echo
  echo '```text'
  head -n 40 "$ERRS"
  echo '```'
  echo
  echo "## Isi artefak"
  echo
  echo "- \`diagnosis.md\` — ringkasan ini"
  echo "- \`errors.txt\` — error hasil ekstraksi dari log Gradle"
  echo "- \`gradle-build.log\` — output penuh \`gradle assembleRelease --stacktrace\` (secret sudah disamarkan)"
  echo "- \`environment.txt\` — versi Java/Gradle, OS, disk, memori"
  echo "- \`reports/\`, \`r8/\`, \`kotlin-daemon/\` — hanya jika ada"
  echo
  echo "## Unduh (Termux)"
  echo
  echo '```bash'
  echo "gh run download ${RUN_ID:-<run-id>} -n '${NAME}' --dir ~/storage/downloads"
  echo '```'
} > "$OUT/diagnosis.md"

# ---------- Redaksi secret (nilai secret + pola token umum) ----------
REDACT_DIR="$OUT" python3 - <<'PY'
import os, re, pathlib
root = pathlib.Path(os.environ["REDACT_DIR"])
keys = ("KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD", "KEYSTORE_BASE64")
vals = sorted({os.environ.get(k, "") for k in keys if len(os.environ.get(k, "")) >= 4},
              key=len, reverse=True)
token_re = re.compile(r"(gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})")
for p in root.rglob("*"):
    if not p.is_file():
        continue
    data = p.read_bytes()
    if b"\x00" in data[:4096]:       # biner: jangan diunggah (tak bisa disamarkan andal)
        p.unlink()
        continue
    text = data.decode("utf-8", errors="replace")
    for v in vals:
        text = text.replace(v, "***")
    text = token_re.sub("***", text)
    p.write_text(text, encoding="utf-8")
PY

# ---------- Anotasi di halaman run (maks 10 per step oleh GitHub) ----------
if [ -s "$OUT/gradle-build.log" ]; then
  strip_ts < "$OUT/gradle-build.log" | grep -E '^e: file://' | head -n 10 | while IFS= read -r line; do
    if [[ "$line" =~ ^e:\ file://([^:]+):([0-9]+):([0-9]+)\ (.*)$ ]]; then
      f="${BASH_REMATCH[1]#"${WORKSPACE%/}/"}"
      msg="${BASH_REMATCH[4]//%/%25}"
      echo "::error file=${f},line=${BASH_REMATCH[2]},col=${BASH_REMATCH[3]},title=Kotlin compile error::${msg}"
    fi
  done
fi
if [ -z "$(ls -A "$OUT" 2>/dev/null)" ]; then
  echo "::warning::Tidak ada bahan untuk diunggah."
  exit 0
fi

# ---------- Ringkasan run + output ----------
{
  echo "## ❌ Build gagal — fase \`${phase}\`"
  echo
  echo '```text'
  head -n 25 "$ERRS"
  echo '```'
  echo
  echo "Artefak diagnosis: \`${NAME}\` (30 hari)."
} >> "$SUMMARY_FILE"

{
  echo "name=${NAME}"
  echo "dir=${OUT}"
  echo "phase=${phase}"
} >> "$OUT_FILE"

echo "Log kegagalan dikumpulkan: ${NAME} ($(du -sh "$OUT" | cut -f1))"
