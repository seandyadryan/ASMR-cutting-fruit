# Play Console handoff

## Listing

- App title: **ASMR Cutting Fruit** (18 characters).
- Developer: **PT Deploy Dulu Pulang Nanti**.
- Contact: **seandyadryan2@gmail.com**.
- Package: `com.deploydulupulangnanti.asmrcuttingfruit`.
- Primary language: English (United States).
- Suggested type/category: Game / Simulation. Set the final category and price in Play Console.
- Short and full description: copy the matching `.txt` files in this directory.
- All app ingredients and modes are included. There are no ads or in-app purchases. Monetize through an upfront paid listing if desired.

## Assets

| Asset | Folder | Count | Resolution |
|---|---|---:|---|
| Store icon | `icon/` | 1 | 512 × 512 PNG |
| Feature graphic | `feature/` | 1 | 1024 × 500 PNG |
| Phone | `screenshots/phone/` | 6 | 1080 × 1920 PNG, 9:16 |
| 7-inch tablet | `screenshots/tablet-7/` | 4 | 1920 × 1080 PNG, 16:9 |
| 10-inch tablet | `screenshots/tablet-10/` | 4 | 1920 × 1080 PNG, 16:9 |
| Desktop / wide Android | `screenshots/desktop/` | 4 | 1920 × 1080 PNG, 16:9 |
| Promotional video | `video/` | 1 | 1920 × 1080 MP4, 24 seconds, English captions, original sound |

The screenshots are framed compositions made with real, running native Android Views captured by on-device instrumentation. Operating-system icons and transient system windows are excluded. Unframed captures and an actual gameplay frame sequence encoded as MP4 are in `raw/`. Tablet captures use separate device size/density configurations. The desktop set depicts the Android app at a wide desktop size; it does not claim a separate Windows game or Google Play Games on PC certification.

`asset-manifest.json` records exact image sizes, file sizes and description character counts. Each screenshot is under 8 MB; the feature graphic is under 15 MB.

## Privacy and app content

Privacy policy source: `docs/index.html`. Intended hosted URL: https://seandyadryan.github.io/ASMR-cutting-fruit/ (verify published URL before entering it in Play Console). The same core disclosures can be read offline in Settings → Privacy policy.

For this exact version, app inspection shows no internet permission, advertising, analytics, account creation, data transmission or third-party tracking SDKs. Gameplay progress and preferences stay on device and Android backup is disabled. In Data safety, answer according to this behavior: no user data collected or shared by the app. Optional email sent through an external email client is described in the policy. Review the form yourself and keep it aligned with any future SDK or monetization changes.

Complete the Play Console content rating and target-audience questionnaires using the actual game. The only knife is a kitchen utensil used on fruit and vegetables. The app does not provide medical benefits or make health claims. Set the intended audience deliberately; no child-directed classification has been assumed.

## Upload and release

1. Create the app in Play Console and choose paid or free pricing before the first production release.
2. Enrol in Play App Signing and upload the signed release AAB. Retain the JKS upload key and credentials in a secure backup.
3. Upload the icon, feature graphic, screenshots and English listing copy.
4. Upload the provided MP4 to your YouTube channel. Set it public or unlisted, disable ads and age restrictions, then paste that YouTube URL into the store listing. The project does not upload to YouTube for you.
5. Enter the live privacy policy URL, contact email, Data safety and content rating responses.
6. Install the Play-distributed build in an internal test track on physical phones and tablets, check sound/haptics with headphones, and complete any account-specific testing or verification required by Play Console.
7. Review and submit when you are ready. No Play Console publication is performed by this project.

Official requirements checked November 8, 2026: [target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878) and [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311). The app targets API 36 and contains no native `.so` libraries.
