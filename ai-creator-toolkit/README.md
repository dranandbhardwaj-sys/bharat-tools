# AI Creator Toolkit — All-in-One Creator Studio (PWA)

An installable, mobile-first web application built with HTML5, CSS3, and Vanilla ES2022 JavaScript for writers, YouTubers, Instagram creators, marketers, students, freelancers, and small businesses.

## Features

- **64 Individual Creator Tools across 7 Toolkits:**
  - **AI Writing (14):** Blog Writer, Article, Paragraph, Essay, Story, Email, Professional Email, Letter, Rewrite, Summarizer, Grammar Checker, Hindi↔English, Text Translator
  - **Social Media Creator (11):** Instagram Caption & Bio, Facebook Post, YouTube Description, Title & Shorts Script, Reel Script, Viral Hook, Hashtags, LinkedIn Post, X/Twitter Post
  - **Marketing Tools (10):** Ad Copy, Product Description, Sales Copy, Landing Page Copy, Business Name, Slogan, SEO Meta Description, SEO Keywords, Marketing Email, CTA Generator
  - **AI Image Creator (6):** Image Prompt, Thumbnail Idea, Logo Prompt, Poster Prompt, Product Image Prompt, Social Media Image Prompt
  - **Video Creator (7):** Video Script, Short Video Script, YouTube Outline, Reel Ideas, Shorts Ideas, Storyboard, Scene Description
  - **Business Creator (8):** Invoice Text, Proposal, Business Plan, Resume, Cover Letter, Job Application, Customer Reply, WhatsApp Business Message
  - **Productivity (8):** AI Brainstorming, To-Do Generator, Meeting Summary, Notes, Presentation Outline, FAQ, Checklist, Content Calendar
- **Bilingual Global UI (English + हिन्दी):** Instant navbar language switcher (`EN | हिन्दी`) and multilingual output generation (`EN`, `HI`, or `EN + HI`).
- **Creator Workspace & History:** Saved Drafts editor, Content History restore, Copy, Download `.txt` / `.md`, and JSON bulk export.
- **Transparent AI Architecture (`js/ai-service.js`):** Runs honestly in **Demo Mode (Offline Template Engine)** by default, or connects to your own HTTPS backend proxy / OpenAI-compatible endpoint configured in **Profile & Settings** (zero secret API keys embedded in frontend source).
- **Membership UI:** Free Tier vs ₹399/month Premium plan cards with consent-gated pending verification billing workflow (never fakes payment confirmation).
- **PWA & GitHub Pages Ready:** Includes `manifest.webmanifest`, `service-worker.js`, `.nojekyll`, and relative `./` paths.

## Project Structure

```text
ai-creator-toolkit/
├── index.html
├── style.css
├── script.js
├── manifest.webmanifest
├── service-worker.js
├── README.md
├── assets/
│   ├── logo.svg
│   └── illustrations/
│       ├── hero-creator.svg
│       └── workspace-empty.svg
├── icons/
│   ├── icon-192.png
│   └── icon-512.png
└── js/
    ├── ai-service.js
    ├── catalog.js
    ├── i18n.js
    ├── storage.js
    └── templates.js
```

## Deploying to GitHub Pages

Upload the contents of `ai-creator-toolkit/` to the root of a repository (so `index.html` is at `/index.html`) and enable **Settings → Pages → Deploy from a branch (`main` / `/ (root)`)**.
