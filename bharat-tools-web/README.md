# Bharat Tools Web — Companion Progressive Web App

**Tagline:** Every Digital Tool. One Smart Platform.  
**Subscription:** ₹399/month (Recurring, User-Approved, Server-Verified)

This folder (`bharat-tools-web/`) is a **standalone Web & PWA project** built with HTML5, CSS3, and modular ES2022 JavaScript. It is completely separate from the Android application and ready for static hosting on **GitHub Pages** (`https://USERNAME.github.io/bharat-tools-web/`).

---

## Features

- **60 Client-Side Utilities across 5 Toolkits:**
  - **PDF Tools:** Image-to-PDF (offline pure-JS PDF 1.4 binary writer), PDF Preview/Inspector, plus PDF Merge (with file reordering), Split, Compress, Rotate, Extract, Reorder, Watermark, and subscriber-gated Protect/Unlock.
  - **Image Tools:** Compressor, Resizer, Aspect Cropper (1:1, 4:3, 16:9, 3:4), JPG/PNG/WebP Converter, Rotate & Flip, Quality/Dimension Inspector, and subscriber-gated Background Utility.
  - **Text Tools:** Word/Character/Reading-Time Counter, Case Converters, Space Cleaner, Reverser, HTML Cleaner, Duplicate Line Remover, Line-by-Line Diff, JSON/Bullet Formatter, and EN/HI Text-to-Speech.
  - **QR Code Tools:** Pure-JS offline ISO/IEC 18004 QR generator for URL, Text, Phone, Wi-Fi, Email, WhatsApp, and UPI ₹ payments with Color Themes, PNG download, and subscriber-gated SVG export.
  - **Calculators & Productivity:** Expression Calculator, Age, Date Difference, Unit Converter (including Gaj/Bigha), Percentage, BMI, GST (5/12/18/28% + CGST/SGST), EMI, Live ECB Currency Converter, Time Zone Converter, CSPRNG Password Generator, Random Number Generator, Stopwatch, and Countdown Timer.
- **SaaS UI & Accessibility:** Deep Indigo, Electric Blue, and Purple design system via CSS variables, Light & Dark modes, bilingual English/Hindi UI, desktop sidebar + mobile bottom navigation & drawer, global search with recent searches, favorites, and tool history.
- **PWA & Offline Ready:** Relative-path `manifest.webmanifest` and `service-worker.js`.
- **Server-Verified ₹399/month Subscription Architecture:** Explicit user consent required; never activates paid access from unverified client code (`js/subscription.js`).

---

## Project Structure

```text
bharat-tools-web/
├── index.html
├── style.css
├── manifest.webmanifest
├── service-worker.js
├── README.md
├── assets/
│   ├── hero-illustration.svg
│   └── logo.svg
├── icons/
│   ├── icon-192.png
│   └── icon-512.png
└── js/
    ├── app.js
    ├── catalog.js
    ├── i18n.js
    ├── qr-engine.js
    ├── storage.js
    ├── subscription.js
    └── tools-engine.js
```

---

## GitHub Pages Deployment

### Option A — Same Repository (Android + Web uploaded together)
This repository already includes `.github/workflows/deploy-web.yml` to publish **only** `bharat-tools-web/` to GitHub Pages without touching the Android app:
1. Open your GitHub repository → **Settings → Pages**.
2. Under **Build and deployment → Source**, choose **GitHub Actions** (instead of "Deploy from a branch").
3. Open the **Actions** tab → select **Deploy Bharat Tools Web to GitHub Pages** → click **Run workflow** (or push any update to `main`).
4. Once finished, your live web app URL is `https://USERNAME.github.io/REPO_NAME/`.

*(If you keep **Source: Deploy from a branch** on `main / (root)`, append `/bharat-tools-web/` to your URL: `https://USERNAME.github.io/REPO_NAME/bharat-tools-web/`.)*

### Option B — Separate Web-Only Repository
1. Create a new GitHub repository named `bharat-tools-web`.
2. Upload **only the files inside** `bharat-tools-web/` to the repository root (so `index.html` is at `/index.html`, not inside a subfolder).
3. In **Settings → Pages**, select **Deploy from a branch** (`main` / `/ (root)`).
4. Visit `https://USERNAME.github.io/bharat-tools-web/`. All asset, script, manifest, and service-worker paths use `./` relative paths for subpath compatibility.

## Configuring Live Payment Verification

Edit `SUBSCRIPTION_CONFIG.verificationEndpoint` in `js/subscription.js` to point to your backend verification endpoint (`POST`) that verifies Razorpay/UPI Autopay webhooks server-side and returns `{ verified: true, token, receiptNumber, periodEndIso }`.
