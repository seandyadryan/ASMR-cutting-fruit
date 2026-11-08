# ASMR Cutting Fruit

A little slice of quiet. An offline native Android game by **PT Deploy Dulu Pulang Nanti**.

**Package:** `com.deploydulupulangnanti.asmrcuttingfruit` · **Version:** 1.0.0 (1) · **Android:** 8.0+ (API 26), target API 36.

## The game

- Swipe through 12 fruits and vegetables. Convex polygon clipping splits the actual ingredient artwork along your gesture; pieces separate with gentle motion.
- Free Slice has no time limit. Focus Session is a foreground-only 60-second break. Daily Basket selects five deterministic ingredients using the device's local calendar date and requires six cuts on each.
- Juicy, soft and crisp synthesized cutting sounds, optional room ambience and haptics, adjustable volume and reduced motion.
- Maple, walnut and sage board appearances. Local Journal with slice count, tried ingredients and unique daily basket completions.
- Native Kotlin Activity, standard Android View controls and a hardware-accelerated custom Canvas game view. Responsive portrait/landscape layouts, keyboard focus, labeled controls and a screen-reader slice action.
- No internet permission, ads, accounts, analytics, billing, native `.so` libraries or runtime downloads.

## Build locally

Open this folder in Android Studio, or use JDK 17+ and Android SDK platform 36 / build-tools 36.0.0:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

Configure your SDK location in ignored `local.properties` or `ANDROID_HOME`. Gradle 8.13 wrapper, Android Gradle Plugin 8.13.2, Kotlin 2.2.21. Maven dependencies are resolved only at build time.

### Signed release

The requested upload keystore is stored **outside this repository** at `D:\KEYSTORE\asmrcuttingfruit.jks`, alias `asmrcuttingfruit`. Keep an independent backup of that file and your credentials. Signing configuration is read from environment variables or the ignored `keystore.properties` file:

```properties
KEYSTORE_PATH=D:/KEYSTORE/asmrcuttingfruit.jks
KEYSTORE_PASSWORD=<your store password>
KEY_ALIAS=asmrcuttingfruit
KEY_PASSWORD=<your key password>
```

```powershell
.\gradlew.bat testDebugUnitTest lintRelease assembleRelease bundleRelease
```

Outputs: `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/bundle/release/app-release.aab`, and `app/build/outputs/mapping/release/mapping.txt`. A release build fails explicitly when signing is missing. Enrol the AAB with Play App Signing on first upload; this JKS serves as your upload key.

## GitHub runner

Workflow: `.github/workflows/android-release.yml`, GitHub-hosted `ubuntu-latest`.

**Ordinary commits and branch pushes do not run CI.** Use Actions → Android release → Run workflow, or push a `v*` version tag after a finished release. The workflow runs unit tests, release lint, and produces a signed APK/AAB plus R8 mapping. Artifacts are retained for 14 days. No self-hosted runner is required.

Signing secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. To configure from your local ignored properties without printing credentials:

```powershell
.\tools\configure-signing.ps1
```

The privacy policy has a separate **manual-only** `Publish privacy policy` workflow. GitHub Pages is configured for Actions deployments, so ordinary commits do not trigger a site build. Run that workflow when you choose to publish a policy update. Public URL: https://seandyadryan.github.io/ASMR-cutting-fruit/.

## Play Store delivery

- `store/short-description.txt` — English description, at most 80 characters.
- `store/full-description.txt` — English full description, at most 4,000 characters.
- `store/icon/` — 512×512 store icon.
- `store/feature/` — 1024×500 feature graphic.
- `store/screenshots/` — device-framed marketing screenshots with English feature captions. Actual app captures are in `store/raw/`.
- `store/video/` — 1080p promotional MP4 for uploading to YouTube yourself.
- `docs/index.html` — public privacy policy, no scripts or analytics. In-app policy is also available offline from Settings.
- `store/PLAY-CONSOLE.md` — submission notes and asset inventory.
- `output/qa/VERIFICATION.md` — validation results and remaining release checks.

The app is prepared for a paid upfront listing or free distribution; the price is configured by you in Play Console. There is no payment or advertising SDK in the app.

## Rebuilding the creative assets

Original generated artwork and prompts: `art/`. Fonts and OFL licenses: `app/src/main/res/font/`, `tools/fonts/`.

Install Python 3.12+ with `pillow`, `numpy` and `imageio-ffmpeg`, then run:

```text
python tools/prepare_assets.py
python tools/create_store_assets.py
```

`tools/capture_native.py` runs the `StoreCaptureTest` instrumentation on an installed debug app and exports the actual native Android View hierarchy at four device configurations. Native captures exclude operating-system status icons and transient system windows. A sequence of actual game frames supplies the gameplay video. Then `tools/create_store_assets.py` adds English captions and complete device frames, creates the feature graphic, and composes the promotional video. Sound effects are original synthesized textures, not recordings of real produce.

## Support and privacy

Developer: **PT Deploy Dulu Pulang Nanti**. Contact: **seandyadryan2@gmail.com**.

Progress and preferences remain in private on-device storage. Android backup is disabled. Clear app storage or uninstall to delete. Optional support email is handled outside the app through the user's email client. See [privacy policy](docs/index.html).
