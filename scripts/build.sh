#!/bin/sh
# Build the unsigned release APK inside F-Droid's own build image, the way
# F-Droid's builder does it. Runs on the build machine; never touches a key.
#
#   scripts/build.sh            -> dist/AndRubik-<version>-release-unsigned.apk
#
# Only git-tracked (plus new, non-ignored) files are copied into the build, so
# host leftovers (node_modules, build/, generated assets) can't leak in.
# Sign the result with scripts/sign.sh.
set -eu

IMAGE=registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie
# Same path as F-Droid's buildserver, so nothing path-dependent can differ.
BUILD_PATH=/home/vagrant/build/io.github.smigniot.andrubik

cd "$(dirname "$0")/.."
VERSION=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts)
COMMIT=$(git rev-parse HEAD)
DIRTY=$(git status --porcelain --untracked-files=no | grep -q . && echo " (uncommitted changes!)" || true)

SRC=$(mktemp -d "${TMPDIR:-/tmp}/andrubik-src.XXXXXX")
trap 'rm -rf "$SRC"' EXIT
git ls-files -z --cached --others --exclude-standard | tar cf - --null -T - | tar xf - -C "$SRC"

echo ">> Building AndRubik $VERSION from $COMMIT$DIRTY"
# Named volumes cache Gradle and the SDK components AGP downloads; they hold
# downloads only, not build outputs, so they don't affect the APK.
podman run --rm --platform linux/amd64 \
    -v "$SRC":"$BUILD_PATH" \
    -v andrubik-gradle:/root/.gradle \
    -v andrubik-sdk:/opt/android-sdk \
    -w "$BUILD_PATH" \
    -e ANDROID_HOME=/opt/android-sdk \
    "$IMAGE" sh -euc '
        apt-get update -qq && apt-get install -y -qq npm >/dev/null
        ./gradlew --no-daemon assembleRelease
    '

mkdir -p dist
APK=dist/AndRubik-$VERSION-release-unsigned.apk
cp "$SRC/app/build/outputs/apk/release/AndRubik-$VERSION-release-unsigned.apk" "$APK"
echo ">> $APK"
shasum -a 256 "$APK"
