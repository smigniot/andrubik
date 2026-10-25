#!/bin/sh
# Sign an unsigned release APK with the developer key. Runs on the trusted
# machine that holds the key; needs only a JDK (8+), no Android SDK, no podman.
#
#   scripts/sign.sh <unsigned.apk> <keystore.p12>
#     -> AndRubik-<version>-release.apk next to the input, plus the signer
#        certificate SHA-256 to put in AllowedAPKSigningKeys.
#
# apksigner.jar (from Android build-tools 34.0.0, the version AGP uses) is
# fetched once from Google, checked against pinned checksums, and kept alone
# (~1 MB) in $APKSIGNER_JAR. The password is typed at apksigner's prompt.
set -eu

[ $# -eq 2 ] || { echo "usage: $0 <unsigned.apk> <keystore.p12>" >&2; exit 2; }
IN=$1
KS=$2
OUT=$(echo "$IN" | sed 's/-unsigned\.apk$/.apk/')
[ "$OUT" != "$IN" ] || { echo "expected a *-unsigned.apk input" >&2; exit 2; }
ALIAS=${ALIAS:-andrubik}
APKSIGNER_JAR=${APKSIGNER_JAR:-$HOME/andrubik-tools/apksigner.jar}

ZIP_URL=https://dl.google.com/android/repository/build-tools_r34-linux.zip
ZIP_SHA1=d6d58e0c6925a9e4d9a541e84cd1f405c2f9d2a9   # from Google's repository2-3.xml
JAR_SHA256=eefdd6aed9db9fb849e4c98a50d8741e19d1b674ba6547220bcb9c3ed152123a

if [ ! -f "$APKSIGNER_JAR" ]; then
    echo ">> Fetching apksigner.jar (one-time, ~61 MB download, not kept)"
    TMP=$(mktemp -d "${TMPDIR:-/tmp}/apksigner.XXXXXX")
    trap 'rm -rf "$TMP"' EXIT
    curl -sSfL -o "$TMP/bt.zip" "$ZIP_URL"
    echo "$ZIP_SHA1  $TMP/bt.zip" | shasum -a 1 -c -
    unzip -j -q "$TMP/bt.zip" 'android-14/lib/apksigner.jar' -d "$TMP"
    mkdir -p "$(dirname "$APKSIGNER_JAR")"
    mv "$TMP/apksigner.jar" "$APKSIGNER_JAR"
fi
echo "$JAR_SHA256  $APKSIGNER_JAR" | shasum -a 256 -c -

apksigner() { java -jar "$APKSIGNER_JAR" "$@"; }

# v4 (.idsig side file) is only for adb incremental installs; F-Droid doesn't use it.
apksigner sign --ks "$KS" --ks-key-alias "$ALIAS" --v4-signing-enabled false --in "$IN" --out "$OUT"
apksigner verify --verbose "$OUT" | grep '^Verified'

echo ">> $OUT"
echo ">> AllowedAPKSigningKeys (signer cert SHA-256):"
apksigner verify --print-certs "$OUT" | sed -n 's/.*certificate SHA-256 digest: //p' | head -1
