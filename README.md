# Bharat Tools

Mobile-first digital utility suite for PDF, Image, Text, QR Code and everyday calculators, in Hindi and English. A companion web/PWA version lives in `bharat-tools-web/`.

## Project type

Android app (Kotlin, Gradle). Application ID: `com.aistudio.bharattools.kxmpzq`

## Build

```bash
./gradlew assembleDebug
```

## Setup

- If the app uses Gemini, copy `.env.example` to `.env` and add your own `GEMINI_API_KEY`. Never commit real keys.
- Release builds need your own keystore. Keep it outside the repo and pass `STORE_PASSWORD` / `KEY_PASSWORD` as environment variables.

