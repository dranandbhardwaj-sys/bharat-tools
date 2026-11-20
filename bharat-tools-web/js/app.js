import { t } from './i18n.js';
import { CATEGORIES, TOOL_CATALOG, getToolById } from './catalog.js';
import { loadState, saveState, hasActiveSubscription } from './storage.js';
import { buildQrPayload, renderQrToCanvas, renderQrToSvg } from './qr-engine.js';
import {
  formatBytes,
  runPdfTool,
  inspectImageFile,
  processImageFile,
  analyzeText,
  transformText,
  evaluateExpression,
  calculateGst,
  calculateEmi,
  calculateAge,
  generateSecurePassword,
  fetchCurrencyRates
} from './tools-engine.js';
import {
  SUBSCRIPTION_CONFIG,
  submitSubscriptionVerification,
  cancelSubscription,
  requestRefund
} from './subscription.js';

let state = loadState();
let currentRoute = state.user ? 'dashboard' : 'landing';
let activeCategoryFilter = 'all';
let searchQuery = '';
let activeTool = null;
let infoModal = null;
let loginModalOpen = false;
let mobileSidebarOpen = false;
let landingPreviewCat = 'pdf';

const root = document.getElementById('app-root');
const toastEl = document.getElementById('toast-root');
let toastTimer = null;

function showToast(msg) {
  toastEl.textContent = msg;
  toastEl.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toastEl.classList.add('hidden'), 3200);
}

function applyLanguageMeta() {
  const lang = state.language === 'hi' ? 'hi' : 'en';
  document.documentElement.lang = lang;
  document.title = t(lang).pageTitle;
}

function setLanguage(lang) {
  state.language = lang === 'hi' ? 'hi' : 'en';
  applyLanguageMeta();
  persistAndRender();
}

function getToolTitle(tool) {
  return state.language === 'hi' ? tool.titleHi : tool.titleEn;
}

function getToolDesc(tool) {
  return state.language === 'hi' ? tool.descHi : tool.descEn;
}

function getCategoryTitle(cat) {
  return state.language === 'hi' ? cat.titleHi : cat.titleEn;
}

function renderLanguageSwitcherHtml() {
  const s = t(state.language);
  return `
    <div class="lang-switch" role="group" aria-label="${escapeHtml(s.langSwitchAria)}">
      <button type="button" class="lang-switch-btn ${state.language === 'en' ? 'active' : ''}" data-set-lang="en" aria-pressed="${state.language === 'en'}">${escapeHtml(s.langEnShort)}</button>
      <button type="button" class="lang-switch-btn ${state.language === 'hi' ? 'active' : ''}" data-set-lang="hi" aria-pressed="${state.language === 'hi'}">${escapeHtml(s.langHiShort)}</button>
    </div>
  `;
}

function applyTheme() {
  const mode = state.theme || 'system';
  const dark = mode === 'dark' || (mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function persistAndRender() {
  saveState(state);
  applyTheme();
  applyLanguageMeta();
  render();
}

function recordHistory(tool, summary) {
  state.recentTools = [tool.id, ...state.recentTools.filter((id) => id !== tool.id)].slice(0, 12);
  state.history = [
    { id: `h_${Date.now()}`, toolId: tool.id, title: getToolTitle(tool), summary, at: new Date().toLocaleTimeString() },
    ...state.history
  ].slice(0, 40);
  saveState(state);
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

function renderToolCardHtml(tool) {
  const s = t(state.language);
  const isFav = state.favorites.includes(tool.id);
  const cat = CATEGORIES.find((c) => c.id === tool.category);
  const locked = tool.proOnly && !hasActiveSubscription(state);
  const toolTitle = getToolTitle(tool);
  return `
    <article class="tool-card" data-open-tool="${tool.id}" tabindex="0" role="button" aria-label="${escapeHtml(toolTitle)}">
      <div class="tool-card-top">
        <div class="tool-icon-box" style="background:${cat.color}20;color:${cat.color};">${cat.id.toUpperCase()}</div>
        <div style="display:flex;align-items:center;gap:6px;">
          ${tool.proOnly ? `<span class="badge ${locked ? 'badge-accent' : 'badge-success'}">${locked ? s.proBadge : s.activeBadge}</span>` : ''}
          <button class="fav-btn ${isFav ? 'active' : ''}" data-fav-tool="${tool.id}" aria-label="${escapeHtml(s.favAria)}" title="${escapeHtml(s.favoritesTitle)}">${isFav ? '★' : '☆'}</button>
        </div>
      </div>
      <div>
        <h3 style="font-size:1rem;">${escapeHtml(toolTitle)}</h3>
        <p class="muted" style="font-size:0.84rem;margin-top:4px;">${escapeHtml(getToolDesc(tool))}</p>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:auto;padding-top:4px;">
        <span class="mono" style="font-size:0.74rem;color:${cat.color};font-weight:700;">${cat.id.toUpperCase()}</span>
        <span style="font-size:0.82rem;color:var(--secondary);font-weight:600;">${s.openTool} →</span>
      </div>
    </article>
  `;
}

function renderLandingPage() {
  const s = t(state.language);
  const previewTools = TOOL_CATALOG.filter((tool) => tool.category === landingPreviewCat).slice(0, 3);

  return `
    <header class="landing-header">
      <div class="landing-header-inner">
        <div class="brand-mark">
          <img src="./assets/logo.svg" alt="${escapeHtml(s.appName)}" />
          <div>
            <div>${escapeHtml(s.appName)}</div>
            <div class="mono muted" style="font-size:0.74rem;">${escapeHtml(s.priceMonthly)}</div>
          </div>
        </div>
        <nav class="landing-nav" aria-label="Landing Navigation">
          <a href="#landing-tools">${escapeHtml(s.navToolsLink)}</a>
          <a href="#landing-features">${escapeHtml(s.navFeaturesLink)}</a>
          <a href="#landing-pricing">${escapeHtml(s.navPricingLink)}</a>
          <a href="#landing-footer">${escapeHtml(s.navAboutLink)}</a>
        </nav>
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;justify-content:flex-end;">
          ${renderLanguageSwitcherHtml()}
          <button class="btn btn-outline btn-sm" id="btn-theme-toggle" aria-label="${escapeHtml(s.themeToggleAria)}">${state.theme === 'dark' ? s.themeLightBtn : s.themeDarkBtn}</button>
          <button class="btn btn-outline btn-sm" id="btn-open-login">${escapeHtml(s.login)}</button>
          <button class="btn btn-primary btn-sm" id="btn-enter-workspace">${escapeHtml(s.getStarted)}</button>
        </div>
      </div>
    </header>

    <main class="landing-container">
      <section class="hero-box">
        <div>
          <span class="badge badge-primary" style="background:rgba(255,255,255,0.16);color:#FFF;">${escapeHtml(s.heroBadge)}</span>
          <h1 style="margin-top:14px;">${escapeHtml(s.tagline)}</h1>
          <p class="hero-sub">${escapeHtml(s.subheadline)}</p>
          <div class="hero-actions">
            <button class="btn btn-primary" id="btn-hero-explore">${escapeHtml(s.exploreTools)}</button>
            <button class="btn btn-accent" id="btn-hero-membership">${escapeHtml(s.startMembership)}</button>
            <button class="btn btn-outline" style="background:rgba(255,255,255,0.12);color:#FFF;border-color:rgba(255,255,255,0.35);" id="btn-hero-workspace">${escapeHtml(s.launchWorkspace)}</button>
          </div>
        </div>
        <div class="hero-visual">
          <img src="./assets/hero-illustration.svg" alt="${escapeHtml(s.appName)}" />
        </div>
      </section>

      <section id="landing-tools">
        <div style="display:flex;justify-content:space-between;align-items:flex-end;flex-wrap:wrap;gap:12px;margin-bottom:16px;">
          <div>
            <h2>${escapeHtml(s.previewHeading)}</h2>
            <p class="muted">${escapeHtml(s.previewSubheading)}</p>
          </div>
          <div class="chips-row">
            ${CATEGORIES.map((c) => `<button class="chip ${landingPreviewCat === c.id ? 'active' : ''}" data-preview-cat="${c.id}">${escapeHtml(getCategoryTitle(c))}</button>`).join('')}
          </div>
        </div>
        <div class="grid-3">
          ${previewTools.map((tool) => renderToolCardHtml(tool)).join('')}
        </div>
      </section>

      <section id="landing-features">
        <h2 style="margin-bottom:14px;">${escapeHtml(s.featuresHeading)}</h2>
        <div class="grid-3">
          ${s.features.map(([title, desc]) => `
            <div class="card">
              <h3>${escapeHtml(title)}</h3>
              <p class="muted" style="font-size:0.88rem;margin-top:6px;">${escapeHtml(desc)}</p>
            </div>
          `).join('')}
        </div>
      </section>

      <section id="landing-pricing">
        <h2 style="margin-bottom:14px;">${escapeHtml(s.pricingHeading)}</h2>
        <div class="card" style="border-color:var(--secondary);">
          <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
            <div>
              <h3>${escapeHtml(s.pricingPlanName)}</h3>
              <p class="muted" style="font-size:0.9rem;">${escapeHtml(s.pricingPlanSub)}</p>
            </div>
            <span class="badge badge-accent">${escapeHtml(s.pricingPlanBadge)}</span>
          </div>
          <div class="mono" style="font-size:2rem;font-weight:700;color:var(--primary);margin:14px 0;">₹399 <span style="font-size:0.95rem;font-weight:500;" class="muted">${escapeHtml(s.pricingPerMonth)}</span></div>
          <ul class="muted" style="font-size:0.9rem;padding-left:20px;display:grid;gap:6px;margin-bottom:18px;">
            ${s.pricingBullets.map((line) => `<li>${escapeHtml(line)}</li>`).join('')}
          </ul>
          <button class="btn btn-accent" id="btn-pricing-subscribe">${escapeHtml(s.pricingCta)}</button>
        </div>
      </section>

      <footer id="landing-footer" style="border-top:1px solid var(--border);padding-top:22px;display:flex;justify-content:space-between;flex-wrap:wrap;gap:14px;">
        <div>
          <strong>${escapeHtml(s.appName)}</strong>
          <p class="muted" style="font-size:0.84rem;">${escapeHtml(s.tagline)}</p>
        </div>
        <div style="display:flex;flex-wrap:wrap;gap:14px;">
          ${s.footerTopics.map(([topicKey, topicLabel]) =>
            `<button class="btn btn-outline btn-sm" data-info-topic="${escapeHtml(topicKey)}">${escapeHtml(topicLabel)}</button>`
          ).join('')}
        </div>
      </footer>
    </main>
  `;
}

function renderSidebarNav() {
  const s = t(state.language);
  const subStatusKey = hasActiveSubscription(state) ? 'active' : state.subscription.status;
  const subStatusLabel = s.statusMap[subStatusKey] || subStatusKey.toUpperCase();
  const items = [
    ['dashboard', s.navDashboard],
    ['all', s.navAllTools],
    ['cat:pdf', s.navPdf],
    ['cat:image', s.navImage],
    ['cat:text', s.navText],
    ['cat:qr', s.navQr],
    ['cat:utility', s.navCalculators],
    ['favorites', s.navFavorites],
    ['history', s.navHistory],
    ['membership', s.navMembership],
    ['settings', s.navSettings],
    ['support', s.navSupport]
  ];
  return `
    <aside class="sidebar ${mobileSidebarOpen ? 'open' : ''}" aria-label="Workspace Sidebar">
      <div class="brand-mark" style="padding:4px 6px 12px;">
        <img src="./assets/logo.svg" alt="${escapeHtml(s.appName)}" />
        <div>
          <div style="font-size:0.98rem;">${escapeHtml(s.appName)}</div>
          <span class="badge ${hasActiveSubscription(state) ? 'badge-success' : 'badge-primary'}">
            ${escapeHtml(subStatusLabel)}
          </span>
        </div>
      </div>
      ${items.map(([route, label]) => {
        const isActive = currentRoute === route || (route === 'all' && currentRoute === 'dashboard' && activeCategoryFilter === 'all');
        return `<button class="sidebar-item ${isActive ? 'active' : ''}" data-nav="${route}">${escapeHtml(label)}</button>`;
      }).join('')}
      <div style="margin-top:auto;padding-top:12px;border-top:1px solid var(--border);display:grid;gap:8px;">
        <button class="btn btn-outline btn-sm" id="btn-back-landing">${escapeHtml(s.backToLanding)}</button>
      </div>
    </aside>
  `;
}

function renderDashboardContent() {
  const s = t(state.language);
  const subActive = hasActiveSubscription(state);
  const filtered = TOOL_CATALOG.filter((tool) => {
    const catOk = activeCategoryFilter === 'all' || tool.category === activeCategoryFilter;
    const q = searchQuery.trim().toLowerCase();
    const qOk =
      !q ||
      tool.titleEn.toLowerCase().includes(q) ||
      tool.titleHi.toLowerCase().includes(q) ||
      tool.descEn.toLowerCase().includes(q) ||
      tool.descHi.toLowerCase().includes(q) ||
      tool.category.includes(q);
    return catOk && qOk;
  });

  if (currentRoute === 'favorites') {
    const favs = state.favorites.map(getToolById).filter(Boolean);
    return `
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <h2>${escapeHtml(s.favoritesTitle)} (${favs.length})</h2>
      </div>
      ${favs.length === 0 ? `<div class="card"><p class="muted">${escapeHtml(s.noFavorites)}</p></div>` : `<div class="tools-grid">${favs.map(renderToolCardHtml).join('')}</div>`}
    `;
  }

  if (currentRoute === 'history') {
    return `
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
        <h2>${escapeHtml(s.navHistory)} (${state.history.length})</h2>
        ${state.history.length ? `<button class="btn btn-danger btn-sm" id="btn-clear-history">${escapeHtml(s.clearHistory)}</button>` : ''}
      </div>
      ${state.history.length === 0 ? `<div class="card"><p class="muted">${escapeHtml(s.noHistory)}</p></div>` : `
        <div style="display:grid;gap:10px;">
          ${state.history.map((h) => {
            const matchedTool = getToolById(h.toolId);
            const itemTitle = matchedTool ? getToolTitle(matchedTool) : h.title;
            return `
            <div class="card" style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
              <div>
                <strong>${escapeHtml(itemTitle)}</strong>
                <div class="mono muted" style="font-size:0.82rem;">${escapeHtml(h.summary)} • ${escapeHtml(h.at)}</div>
              </div>
              <button class="btn btn-outline btn-sm" data-open-tool="${h.toolId}">${escapeHtml(s.restoreTask)} →</button>
            </div>
          `;
          }).join('')}
        </div>
      `}
    `;
  }

  if (currentRoute === 'membership') {
    const sub = state.subscription;
    const statusText = s.statusMap[sub.status] || sub.status.toUpperCase();
    return `
      <div class="card" style="border-color:var(--secondary);">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
          <div>
            <h2>${escapeHtml(s.subscriptionTitle)}</h2>
            <p class="muted">${escapeHtml(s.subscriptionTerms)}</p>
          </div>
          <span class="badge ${subActive ? 'badge-success' : 'badge-warning'}">${escapeHtml(statusText)}</span>
        </div>

        <div class="grid-3" style="margin-top:16px;">
          <div class="card" style="background:var(--surface-soft);box-shadow:none;">
            <div class="muted" style="font-size:0.8rem;">${escapeHtml(s.planPriceLabel)}</div>
            <div class="mono" style="font-size:1.3rem;font-weight:700;">${escapeHtml(s.planPriceValue)}</div>
          </div>
          <div class="card" style="background:var(--surface-soft);box-shadow:none;">
            <div class="muted" style="font-size:0.8rem;">${escapeHtml(s.billingCycleLabel)}</div>
            <div class="mono" style="font-size:1.05rem;font-weight:700;">${escapeHtml(s.billingCycleValue)}</div>
          </div>
          <div class="card" style="background:var(--surface-soft);box-shadow:none;">
            <div class="muted" style="font-size:0.8rem;">${escapeHtml(s.renewalConsentLabel)}</div>
            <div class="mono" style="font-size:1.05rem;font-weight:700;">${escapeHtml(sub.autoRenewConsent ? s.consentApproved : s.consentOff)}</div>
          </div>
        </div>

        ${subActive || sub.status === 'pending' ? `
          <div style="margin-top:16px;display:flex;gap:10px;flex-wrap:wrap;">
            <button class="btn btn-danger" id="btn-cancel-subscription">${escapeHtml(s.cancelSubBtn)}</button>
          </div>
        ` : ''}

        <form id="sub-form" style="margin-top:18px;display:grid;gap:12px;">
          <h3>${escapeHtml(s.subFormTitle)}</h3>
          <p class="muted" style="font-size:0.84rem;">${escapeHtml(s.subSecurityNote)}</p>
          <div class="grid-2">
            <div>
              <label for="sub-method" style="font-size:0.84rem;font-weight:600;">${escapeHtml(s.paymentMethodLabel)}</label>
              <select id="sub-method" class="select">
                <option value="upi_autopay">${escapeHtml(s.methodUpiAutopay)}</option>
                <option value="gateway_mandate">${escapeHtml(s.methodGateway)}</option>
                <option value="upi_reference">${escapeHtml(s.methodUpiRef)}</option>
              </select>
            </div>
            <div>
              <label for="sub-ref" style="font-size:0.84rem;font-weight:600;">${escapeHtml(s.refIdLabel)}</label>
              <input id="sub-ref" class="input" placeholder="${escapeHtml(s.refIdPlaceholder)}" required minlength="4" />
            </div>
          </div>
          <label style="display:flex;align-items:flex-start;gap:10px;font-size:0.86rem;cursor:pointer;">
            <input type="checkbox" id="sub-consent" style="margin-top:4px;" />
            <span>${escapeHtml(s.consentCheckboxText)}</span>
          </label>
          <button type="submit" class="btn btn-primary">${escapeHtml(s.submitSubBtn)}</button>
        </form>
      </div>

      <div class="card">
        <h3 style="margin-bottom:10px;">${escapeHtml(s.billingHistoryTitle)} (${sub.billingHistory.length})</h3>
        ${sub.billingHistory.length === 0 ? `<p class="muted">${escapeHtml(s.noBillingRecords)}</p>` : `
          <div style="display:grid;gap:10px;">
            ${sub.billingHistory.map((b) => {
              const recStatus = s.statusMap[b.status] || b.status.toUpperCase();
              return `
              <div style="border:1px solid var(--border);border-radius:12px;padding:12px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
                <div>
                  <strong>${escapeHtml(s.priceMonthly)} • ${escapeHtml(recStatus)}</strong>
                  <div class="mono muted" style="font-size:0.8rem;">${escapeHtml(b.periodLabel)} • ${escapeHtml(s.refLabel)}: ${escapeHtml(b.referenceId)} (${escapeHtml(b.paymentMethod)})</div>
                  ${b.receiptNumber ? `<div class="mono" style="font-size:0.8rem;color:var(--success);">${escapeHtml(s.receiptLabel)}: ${escapeHtml(b.receiptNumber)}</div>` : ''}
                </div>
                ${b.status === 'active' ? `<button class="btn btn-outline btn-sm" data-refund-id="${b.id}">${escapeHtml(s.requestRefundBtn)}</button>` : ''}
              </div>
            `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  if (currentRoute === 'settings') {
    return `
      <div class="card" style="display:grid;gap:16px;">
        <h2>${escapeHtml(s.navSettings)}</h2>
        <div>
          <div style="font-weight:600;margin-bottom:8px;">${escapeHtml(s.themeLabel)}</div>
          <div class="chips-row">
            ${['system', 'light', 'dark'].map((mode) => `<button class="chip ${state.theme === mode ? 'active' : ''}" data-set-theme="${mode}">${escapeHtml(s.themeModes[mode])}</button>`).join('')}
          </div>
        </div>
        <div>
          <div style="font-weight:600;margin-bottom:8px;">${escapeHtml(s.languageLabel)}</div>
          <div class="chips-row">
            <button class="chip ${state.language === 'en' ? 'active' : ''}" data-set-lang="en">${escapeHtml(s.langEnFull)}</button>
            <button class="chip ${state.language === 'hi' ? 'active' : ''}" data-set-lang="hi">${escapeHtml(s.langHiFull)}</button>
          </div>
        </div>
        <div>
          <div style="font-weight:600;margin-bottom:4px;">${escapeHtml(s.privacyTitle)}</div>
          <p class="muted" style="font-size:0.88rem;">${escapeHtml(s.privacyDesc)}</p>
        </div>
      </div>
    `;
  }

  if (currentRoute === 'support') {
    return `
      <div class="card" style="display:grid;gap:12px;">
        <h2>${escapeHtml(s.navSupport)}</h2>
        <p class="muted">${escapeHtml(s.supportDesc)}</p>
        <div class="chips-row">
          ${s.footerTopics.filter(([k]) => ['Privacy Policy', 'Terms', 'Refund Policy'].includes(k)).map(([topicKey, topicLabel]) =>
            `<button class="btn btn-outline btn-sm" data-info-topic="${escapeHtml(topicKey)}">${escapeHtml(topicLabel)}</button>`
          ).join('')}
        </div>
      </div>
    `;
  }

  const popular = TOOL_CATALOG.filter((tool) => tool.popular);
  const recent = state.recentTools.map(getToolById).filter(Boolean).slice(0, 6);
  const quickIds = ['pdf_merge', 'img_compress', 'qr_upi', 'util_gst'].map(getToolById).filter(Boolean);
  const activeCat = CATEGORIES.find((c) => c.id === activeCategoryFilter);

  return `
    <section class="card" style="background:linear-gradient(135deg,#101F52,#1E3A8A,#43238A);color:#FFF;border:none;">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;">
        <div>
          <span class="badge" style="background:rgba(255,255,255,0.18);color:#FFF;">${escapeHtml(subActive ? s.activeSubBadge : s.planBadge)}</span>
          <h2 style="margin-top:8px;">${escapeHtml(state.user ? `${s.welcomeUserPrefix}${state.user.name}` : s.welcomeGuest)}</h2>
          <p style="color:#E0E7FF;font-size:0.92rem;margin-top:4px;">${escapeHtml(s.tagline)}</p>
        </div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <div style="background:rgba(255,255,255,0.12);padding:10px 14px;border-radius:12px;">
            <div style="font-size:0.75rem;color:#DBEAFE;">${escapeHtml(s.historyRunsLabel)}</div>
            <div class="mono" style="font-weight:700;">${state.history.length}</div>
          </div>
          <div style="background:rgba(255,255,255,0.12);padding:10px 14px;border-radius:12px;">
            <div style="font-size:0.75rem;color:#DBEAFE;">${escapeHtml(s.favoritesTitle)}</div>
            <div class="mono" style="font-weight:700;">${state.favorites.length}</div>
          </div>
        </div>
      </div>
    </section>

    <section style="display:grid;gap:10px;">
      <div style="display:flex;gap:8px;">
        <input id="global-search" class="input" type="search" value="${escapeHtml(searchQuery)}" placeholder="${escapeHtml(s.searchPlaceholder)}" aria-label="${escapeHtml(s.searchAria)}" />
        ${searchQuery ? `<button class="btn btn-outline" id="btn-clear-search">${escapeHtml(s.clearBtn)}</button>` : ''}
      </div>
      ${state.recentSearches.length && !searchQuery ? `
        <div class="chips-row">
          ${state.recentSearches.map((q) => `<button class="chip" data-recent-search="${escapeHtml(q)}">⌕ ${escapeHtml(q)}</button>`).join('')}
        </div>
      ` : ''}
      <div class="chips-row">
        <button class="chip ${activeCategoryFilter === 'all' ? 'active' : ''}" data-filter-cat="all">${escapeHtml(s.navAllTools)}</button>
        ${CATEGORIES.map((c) => `<button class="chip ${activeCategoryFilter === c.id ? 'active' : ''}" data-filter-cat="${c.id}">${escapeHtml(getCategoryTitle(c))}</button>`).join('')}
      </div>
    </section>

    ${!searchQuery && activeCategoryFilter === 'all' ? `
      <section>
        <h3 style="margin-bottom:10px;">${escapeHtml(s.quickActions)}</h3>
        <div class="chips-row">
          ${quickIds.map((tool) => `<button class="btn btn-outline btn-sm" data-open-tool="${tool.id}">⚡ ${escapeHtml(getToolTitle(tool))}</button>`).join('')}
        </div>
      </section>
      <section>
        <h3 style="margin-bottom:10px;">${escapeHtml(s.popularTools)}</h3>
        <div class="tools-grid">${popular.map(renderToolCardHtml).join('')}</div>
      </section>
      ${recent.length ? `
        <section>
          <h3 style="margin-bottom:10px;">${escapeHtml(s.recentlyUsed)}</h3>
          <div class="chips-row">
            ${recent.map((tool) => `<button class="chip" data-open-tool="${tool.id}">${escapeHtml(getToolTitle(tool))}</button>`).join('')}
          </div>
        </section>
      ` : ''}
    ` : ''}

    <section>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
        <h3>${escapeHtml(activeCategoryFilter === 'all' ? s.navAllTools : (activeCat ? getCategoryTitle(activeCat) : ''))}</h3>
        <span class="badge">${filtered.length} ${escapeHtml(s.toolsCountSuffix)}</span>
      </div>
      ${filtered.length === 0 ? `
        <div class="card" style="text-align:center;">
          <p class="muted">${escapeHtml(`${s.noSearchPrefix}${searchQuery}${s.noSearchSuffix}`)}</p>
        </div>
      ` : `<div class="tools-grid">${filtered.map(renderToolCardHtml).join('')}</div>`}
    </section>
  `;
}

function renderWorkspaceShell() {
  const s = t(state.language);
  return `
    <div class="app-shell">
      ${renderSidebarNav()}
      <div class="main-area">
        <header class="topbar">
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="btn btn-outline btn-sm mobile-only" id="btn-mobile-drawer" aria-label="${escapeHtml(s.menuAria)}">☰</button>
            <strong style="font-family:var(--font-display);">${escapeHtml(s.appName)}</strong>
          </div>
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;justify-content:flex-end;">
            ${renderLanguageSwitcherHtml()}
            <button class="btn btn-outline btn-sm" id="btn-theme-toggle" aria-label="${escapeHtml(s.themeToggleAria)}">${state.theme === 'dark' ? '☀' : '☾'}</button>
            <button class="btn btn-primary btn-sm" data-nav="membership">${escapeHtml(s.priceShort)}</button>
          </div>
        </header>
        <main class="workspace-body">${renderDashboardContent()}</main>
      </div>
    </div>

    <nav class="bottom-nav" aria-label="Mobile Navigation">
      <button class="${currentRoute === 'dashboard' ? 'active' : ''}" data-nav="dashboard"><span>🏠</span><span>${escapeHtml(s.navDashboard)}</span></button>
      <button class="${currentRoute === 'all' ? 'active' : ''}" data-nav="all"><span>🧰</span><span>${escapeHtml(s.bottomTools)}</span></button>
      <button class="${currentRoute === 'favorites' ? 'active' : ''}" data-nav="favorites"><span>★</span><span>${escapeHtml(s.navFavorites)}</span></button>
      <button class="${currentRoute === 'membership' ? 'active' : ''}" data-nav="membership"><span>💎</span><span>${escapeHtml(s.bottomPlan)}</span></button>
    </nav>
  `;
}

function renderToolWorkspaceModal() {
  if (!activeTool) return '';
  const s = t(state.language);
  const tool = activeTool;
  const locked = tool.proOnly && !hasActiveSubscription(state);
  const title = getToolTitle(tool);
  const desc = getToolDesc(tool);

  let bodyHtml = '';
  if (locked) {
    bodyHtml = `
      <div class="card" style="background:var(--surface-soft);display:grid;gap:10px;">
        <span class="badge badge-accent">${escapeHtml(s.proFeatureBadge)}</span>
        <p>${escapeHtml(desc)} ${escapeHtml(s.proRequiredSuffix)}</p>
        <button class="btn btn-accent" id="btn-modal-go-membership">${escapeHtml(s.viewMembershipBtn)}</button>
      </div>
    `;
  } else if (tool.category === 'pdf') {
    const isImgToPdf = tool.id === 'image_to_pdf';
    bodyHtml = `
      <div class="upload-zone" id="ws-upload-zone" tabindex="0">
        <strong>${escapeHtml(isImgToPdf ? s.dropImagesPrompt : tool.id === 'pdf_merge' ? s.dropPdfsPrompt : s.dropPdfPrompt)}</strong>
        <span class="muted" style="font-size:0.84rem;">${escapeHtml(s.localProcessingNote)}</span>
        <input id="ws-file-input" type="file" ${isImgToPdf ? 'accept="image/*" multiple' : tool.id === 'pdf_merge' ? 'accept="application/pdf" multiple' : 'accept="application/pdf"'} class="hidden" />
      </div>
      <div id="ws-file-list" style="display:grid;gap:6px;margin-top:10px;"></div>
      ${tool.id === 'pdf_rotate' ? `<select id="ws-pdf-rot" class="select" style="margin-top:10px;"><option value="90">${escapeHtml(s.rotate90)}</option><option value="180">${escapeHtml(s.rotate180)}</option><option value="270">${escapeHtml(s.rotate270)}</option></select>` : ''}
      ${tool.id === 'pdf_extract' || tool.id === 'pdf_reorder' ? `<input id="ws-pdf-pages" class="input" style="margin-top:10px;" value="1" placeholder="${escapeHtml(s.pagesPlaceholder)}" />` : ''}
      ${tool.id === 'pdf_watermark' ? `<input id="ws-pdf-wm" class="input" style="margin-top:10px;" value="BHARAT TOOLS" placeholder="${escapeHtml(s.watermarkPlaceholder)}" />` : ''}
      <div style="display:flex;gap:10px;margin-top:12px;">
        <button class="btn btn-primary" style="flex:1;" id="ws-run-pdf">${escapeHtml(`${s.runPrefix}${title}`)}</button>
        <button class="btn btn-outline" id="ws-reset">${escapeHtml(s.resetBtn)}</button>
      </div>
      <div id="ws-output" style="margin-top:14px;"></div>
    `;
  } else if (tool.category === 'image') {
    bodyHtml = `
      <div class="upload-zone" id="ws-upload-zone" tabindex="0">
        <strong>${escapeHtml(s.dropImagePrompt)}</strong>
        <span class="muted" style="font-size:0.84rem;">${escapeHtml(s.imageFormatsNote)}</span>
        <input id="ws-file-input" type="file" accept="image/*" class="hidden" />
      </div>
      <div id="ws-img-meta" class="muted" style="font-size:0.84rem;margin-top:8px;"></div>
      ${tool.id === 'img_compress' || tool.id === 'img_webp' ? `<label style="display:block;margin-top:10px;font-size:0.86rem;">${escapeHtml(s.qualityLabel)}: <span id="ws-q-val">78</span>%<input id="ws-img-quality" type="range" min="15" max="100" value="78" style="width:100%;" /></label>` : ''}
      ${tool.id === 'img_resize' ? `<div class="grid-2" style="margin-top:10px;"><input id="ws-img-w" class="input" type="number" value="1080" placeholder="${escapeHtml(s.widthPlaceholder)}" /><input id="ws-img-h" class="input" type="number" value="1080" placeholder="${escapeHtml(s.heightPlaceholder)}" /></div>` : ''}
      ${tool.id === 'img_crop' ? `<select id="ws-img-crop" class="select" style="margin-top:10px;"><option value="1:1">${escapeHtml(s.crop11)}</option><option value="4:3">${escapeHtml(s.crop43)}</option><option value="16:9">${escapeHtml(s.crop169)}</option><option value="3:4">${escapeHtml(s.crop34)}</option></select>` : ''}
      ${tool.id === 'img_rotate' ? `<div class="grid-2" style="margin-top:10px;"><select id="ws-img-rot" class="select"><option value="90">90°</option><option value="180">180°</option><option value="270">270°</option></select><select id="ws-img-flip" class="select"><option value="none">${escapeHtml(s.noFlip)}</option><option value="h">${escapeHtml(s.flipH)}</option><option value="v">${escapeHtml(s.flipV)}</option></select></div>` : ''}
      <div style="display:flex;gap:10px;margin-top:12px;">
        <button class="btn btn-primary" style="flex:1;" id="ws-run-img">${escapeHtml(s.processImageBtn)}</button>
        <button class="btn btn-outline" id="ws-reset">${escapeHtml(s.resetBtn)}</button>
      </div>
      <div id="ws-output" style="margin-top:14px;"></div>
    `;
  } else if (tool.category === 'text') {
    bodyHtml = `
      <textarea id="ws-txt-in" class="textarea" placeholder="${escapeHtml(s.textInputPlaceholder)}">${escapeHtml(s.textDefaultSample)}</textarea>
      ${tool.id === 'txt_compare' ? `<textarea id="ws-txt-b" class="textarea" style="margin-top:8px;" placeholder="${escapeHtml(s.comparePlaceholder)}"></textarea>` : ''}
      <div class="chips-row" style="margin-top:10px;">
        <button class="chip" data-txt-op="upper">${escapeHtml(s.opUpper)}</button>
        <button class="chip" data-txt-op="lower">${escapeHtml(s.opLower)}</button>
        <button class="chip" data-txt-op="title">${escapeHtml(s.opTitle)}</button>
        <button class="chip" data-txt-op="spaces">${escapeHtml(s.opSpaces)}</button>
        <button class="chip" data-txt-op="dedup">${escapeHtml(s.opDedup)}</button>
        <button class="chip" data-txt-op="reverse">${escapeHtml(s.opReverse)}</button>
        <button class="chip" data-txt-op="json">${escapeHtml(s.opJson)}</button>
        <button class="chip" id="ws-tts-en">${escapeHtml(s.speakEn)}</button>
        <button class="chip" id="ws-tts-hi">${escapeHtml(s.speakHi)}</button>
        ${tool.id === 'txt_compare' ? `<button class="chip active" data-txt-op="compare">${escapeHtml(s.opCompare)}</button>` : ''}
      </div>
      <div id="ws-txt-stats" class="mono muted" style="font-size:0.82rem;margin-top:10px;"></div>
      <textarea id="ws-txt-out" class="textarea" style="margin-top:10px;" readonly placeholder="${escapeHtml(s.textOutputPlaceholder)}"></textarea>
      <div style="display:flex;gap:8px;margin-top:8px;">
        <button class="btn btn-primary btn-sm" id="ws-txt-copy">${escapeHtml(s.copyOutputBtn)}</button>
        <button class="btn btn-outline btn-sm" id="ws-reset">${escapeHtml(s.resetBtn)}</button>
      </div>
    `;
  } else if (tool.category === 'qr') {
    bodyHtml = `
      <div style="display:grid;gap:10px;">
        <select id="ws-qr-type" class="select">
          <option value="url" ${tool.id === 'qr_url' ? 'selected' : ''}>${escapeHtml(s.qrTypes.url)}</option>
          <option value="upi" ${tool.id === 'qr_upi' ? 'selected' : ''}>${escapeHtml(s.qrTypes.upi)}</option>
          <option value="wifi" ${tool.id === 'qr_wifi' ? 'selected' : ''}>${escapeHtml(s.qrTypes.wifi)}</option>
          <option value="whatsapp" ${tool.id === 'qr_whatsapp' ? 'selected' : ''}>${escapeHtml(s.qrTypes.whatsapp)}</option>
          <option value="text" ${tool.id === 'qr_text' ? 'selected' : ''}>${escapeHtml(s.qrTypes.text)}</option>
          <option value="phone" ${tool.id === 'qr_phone' ? 'selected' : ''}>${escapeHtml(s.qrTypes.phone)}</option>
          <option value="email" ${tool.id === 'qr_email' ? 'selected' : ''}>${escapeHtml(s.qrTypes.email)}</option>
        </select>
        <div id="ws-qr-fields" style="display:grid;gap:8px;"></div>
        <div class="grid-2">
          <select id="ws-qr-fg" class="select">
            ${Object.entries(s.qrFgColors).map(([hex, label]) => `<option value="${hex}">${escapeHtml(label)}</option>`).join('')}
          </select>
          <select id="ws-qr-bg" class="select">
            ${Object.entries(s.qrBgColors).map(([hex, label]) => `<option value="${hex}">${escapeHtml(label)}</option>`).join('')}
          </select>
        </div>
        <button class="btn btn-primary" id="ws-run-qr">${escapeHtml(s.generateQrBtn)}</button>
        <div id="ws-qr-preview" style="display:grid;place-items:center;gap:10px;margin-top:8px;"></div>
      </div>
    `;
  } else {
    bodyHtml = renderUtilityBody(tool.id);
  }

  return `
    <div class="modal-backdrop" id="tool-modal-backdrop" role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}">
      <div class="modal-card">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:14px;">
          <div>
            <span class="badge badge-primary">${tool.category.toUpperCase()}</span>
            <h2 style="margin-top:6px;">${escapeHtml(title)}</h2>
            <p class="muted" style="font-size:0.86rem;">${escapeHtml(desc)}</p>
          </div>
          <button class="btn btn-outline btn-sm" id="btn-close-tool" aria-label="${escapeHtml(s.closeWorkspaceAria)}">✕</button>
        </div>
        ${bodyHtml}
      </div>
    </div>
  `;
}

function renderUtilityBody(id) {
  const u = t(state.language).utilities;
  const s = t(state.language);
  if (id === 'util_gst') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-gst-amt" class="input" type="number" value="1000" placeholder="${escapeHtml(u.amountInr)}" />
        <div class="grid-2">
          <select id="u-gst-rate" class="select"><option value="5">${escapeHtml(u.gst5)}</option><option value="12">${escapeHtml(u.gst12)}</option><option value="18" selected>${escapeHtml(u.gst18)}</option><option value="28">${escapeHtml(u.gst28)}</option></select>
          <select id="u-gst-mode" class="select"><option value="excl">${escapeHtml(u.gstExcl)}</option><option value="incl">${escapeHtml(u.gstIncl)}</option></select>
        </div>
        <button class="btn btn-primary" id="u-gst-run">${escapeHtml(u.calcGstBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_emi') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-emi-p" class="input" type="number" value="500000" placeholder="${escapeHtml(u.loanAmt)}" />
        <div class="grid-2">
          <input id="u-emi-r" class="input" type="number" step="0.1" value="8.5" placeholder="${escapeHtml(u.interestRate)}" />
          <input id="u-emi-n" class="input" type="number" value="60" placeholder="${escapeHtml(u.tenureMonths)}" />
        </div>
        <button class="btn btn-primary" id="u-emi-run">${escapeHtml(u.calcEmiBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_password' || id === 'util_random') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-pwd-len" class="input" type="number" value="16" min="6" max="64" placeholder="${escapeHtml(u.pwdLengthPlaceholder)}" />
        <button class="btn btn-primary" id="u-pwd-run">${escapeHtml(id === 'util_password' ? u.genPwdBtn : u.genRandomBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_currency') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-fx-amt" class="input" type="number" value="1000" placeholder="${escapeHtml(u.amtInrFx)}" />
        <button class="btn btn-primary" id="u-fx-run">${escapeHtml(u.fetchFxBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_age' || id === 'util_datediff') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-date-a" class="input" type="date" value="2000-01-01" />
        ${id === 'util_datediff' ? `<input id="u-date-b" class="input" type="date" value="2026-11-20" />` : ''}
        <button class="btn btn-primary" id="u-date-run">${escapeHtml(u.calculateBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_bmi') {
    return `
      <div style="display:grid;gap:10px;">
        <div class="grid-2"><input id="u-bmi-kg" class="input" type="number" value="68" placeholder="${escapeHtml(u.weightKg)}" /><input id="u-bmi-cm" class="input" type="number" value="172" placeholder="${escapeHtml(u.heightCm)}" /></div>
        <button class="btn btn-primary" id="u-bmi-run">${escapeHtml(u.calcBmiBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  if (id === 'util_stopwatch' || id === 'util_timer') {
    return `
      <div style="display:grid;gap:12px;text-align:center;">
        <div id="u-clock-disp" class="mono" style="font-size:2.2rem;font-weight:700;">${id === 'util_timer' ? '05:00' : '00:00.0'}</div>
        <div style="display:flex;gap:8px;justify-content:center;">
          <button class="btn btn-primary" id="u-clock-start">${escapeHtml(u.startBtn)}</button>
          <button class="btn btn-outline" id="u-clock-stop">${escapeHtml(u.pauseBtn)}</button>
          <button class="btn btn-outline" id="u-clock-reset">${escapeHtml(s.resetBtn)}</button>
        </div>
      </div>
    `;
  }
  if (id === 'util_unit' || id === 'util_time' || id === 'util_percent') {
    return `
      <div style="display:grid;gap:10px;">
        <input id="u-uni-val" class="input" type="number" value="100" placeholder="${escapeHtml(u.valuePlaceholder)}" />
        <button class="btn btn-primary" id="u-uni-run">${escapeHtml(u.computeBtn)}</button>
        <div id="ws-output" class="mono"></div>
      </div>
    `;
  }
  return `
    <div style="display:grid;gap:10px;">
      <input id="u-calc-expr" class="input mono" value="(399 * 12) - 500" placeholder="${escapeHtml(u.exprPlaceholder)}" />
      <button class="btn btn-primary" id="u-calc-run">${escapeHtml(u.evaluateBtn)}</button>
      <div id="ws-output" class="mono" style="font-size:1.25rem;font-weight:700;"></div>
    </div>
  `;
}

function renderModals() {
  const s = t(state.language);
  let html = '';
  if (infoModal) {
    const topicPair = s.footerTopics.find(([key]) => key === infoModal);
    const modalHeading = topicPair ? topicPair[1] : infoModal;
    html += `
      <div class="modal-backdrop" role="dialog" aria-modal="true">
        <div class="modal-card" style="max-width:480px;">
          <h2>${escapeHtml(modalHeading)}</h2>
          <p class="muted" style="margin:12px 0 18px;">${escapeHtml(s.infoModalCopy[infoModal] || '')}</p>
          <button class="btn btn-primary" id="btn-close-info">${escapeHtml(s.closeBtn)}</button>
        </div>
      </div>
    `;
  }
  if (loginModalOpen) {
    html += `
      <div class="modal-backdrop" role="dialog" aria-modal="true">
        <div class="modal-card" style="max-width:440px;">
          <h2>${escapeHtml(s.loginTitle)}</h2>
          <p class="muted" style="margin:8px 0 14px;font-size:0.9rem;">${escapeHtml(s.loginDesc)}</p>
          <form id="login-form" style="display:grid;gap:10px;">
            <input id="login-name" class="input" placeholder="${escapeHtml(s.loginNamePlaceholder)}" required value="${escapeHtml(state.user?.name || '')}" />
            <input id="login-email" class="input" type="email" placeholder="${escapeHtml(s.loginEmailPlaceholder)}" required value="${escapeHtml(state.user?.email || '')}" />
            <div style="display:flex;gap:8px;">
              <button type="submit" class="btn btn-primary" style="flex:1;">${escapeHtml(s.continueBtn)}</button>
              <button type="button" class="btn btn-outline" id="btn-close-login">${escapeHtml(s.cancelBtn)}</button>
            </div>
          </form>
        </div>
      </div>
    `;
  }
  html += renderToolWorkspaceModal();
  return html;
}

function render() {
  root.innerHTML = (currentRoute === 'landing' ? renderLandingPage() : renderWorkspaceShell()) + renderModals();
  bindEvents();
}

let clockInterval = null;

function bindEvents() {
  clearInterval(clockInterval);

  document.getElementById('btn-theme-toggle')?.addEventListener('click', () => {
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    persistAndRender();
  });
  document.getElementById('btn-lang-toggle')?.addEventListener('click', () => {
    setLanguage(state.language === 'en' ? 'hi' : 'en');
  });
  document.getElementById('btn-open-login')?.addEventListener('click', () => { loginModalOpen = true; render(); });
  document.getElementById('btn-close-login')?.addEventListener('click', () => { loginModalOpen = false; render(); });
  document.getElementById('btn-close-info')?.addEventListener('click', () => { infoModal = null; render(); });
  document.getElementById('btn-enter-workspace')?.addEventListener('click', () => { currentRoute = 'dashboard'; render(); });
  document.getElementById('btn-hero-workspace')?.addEventListener('click', () => { currentRoute = 'dashboard'; render(); });
  document.getElementById('btn-hero-explore')?.addEventListener('click', () => { document.getElementById('landing-tools')?.scrollIntoView({ behavior: 'smooth' }); });
  document.getElementById('btn-hero-membership')?.addEventListener('click', () => { document.getElementById('landing-pricing')?.scrollIntoView({ behavior: 'smooth' }); });
  document.getElementById('btn-pricing-subscribe')?.addEventListener('click', () => { currentRoute = 'membership'; render(); });
  document.getElementById('btn-back-landing')?.addEventListener('click', () => { currentRoute = 'landing'; mobileSidebarOpen = false; render(); });
  document.getElementById('btn-mobile-drawer')?.addEventListener('click', () => { mobileSidebarOpen = !mobileSidebarOpen; render(); });
  document.getElementById('btn-close-tool')?.addEventListener('click', () => { activeTool = null; render(); });
  document.getElementById('btn-modal-go-membership')?.addEventListener('click', () => { activeTool = null; currentRoute = 'membership'; render(); });

  document.getElementById('login-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    const s = t(state.language);
    const name = document.getElementById('login-name').value.trim();
    const email = document.getElementById('login-email').value.trim();
    state.user = { name, email };
    loginModalOpen = false;
    currentRoute = 'dashboard';
    persistAndRender();
    showToast(`${s.welcomeToastPrefix}${name}`);
  });

  document.querySelectorAll('[data-preview-cat]').forEach((el) => {
    el.addEventListener('click', () => { landingPreviewCat = el.dataset.previewCat; render(); });
  });
  document.querySelectorAll('[data-info-topic]').forEach((el) => {
    el.addEventListener('click', () => { infoModal = el.dataset.infoTopic; render(); });
  });
  document.querySelectorAll('[data-nav]').forEach((el) => {
    el.addEventListener('click', () => {
      const target = el.dataset.nav;
      if (target.startsWith('cat:')) {
        activeCategoryFilter = target.split(':')[1];
        currentRoute = 'all';
      } else {
        if (target === 'all') activeCategoryFilter = 'all';
        currentRoute = target;
      }
      mobileSidebarOpen = false;
      render();
    });
  });
  document.querySelectorAll('[data-filter-cat]').forEach((el) => {
    el.addEventListener('click', () => { activeCategoryFilter = el.dataset.filterCat; render(); });
  });
  document.querySelectorAll('[data-recent-search]').forEach((el) => {
    el.addEventListener('click', () => { searchQuery = el.dataset.recentSearch; render(); });
  });
  document.querySelectorAll('[data-fav-tool]').forEach((el) => {
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = el.dataset.favTool;
      if (state.favorites.includes(id)) state.favorites = state.favorites.filter((x) => x !== id);
      else state.favorites = [id, ...state.favorites];
      persistAndRender();
    });
  });
  document.querySelectorAll('[data-open-tool]').forEach((el) => {
    el.addEventListener('click', () => {
      const tool = getToolById(el.dataset.openTool);
      if (!tool) return;
      if (searchQuery.trim().length >= 2) {
        state.recentSearches = [searchQuery.trim(), ...state.recentSearches.filter((q) => q !== searchQuery.trim())].slice(0, 6);
        saveState(state);
      }
      activeTool = tool;
      render();
    });
  });

  const searchInput = document.getElementById('global-search');
  searchInput?.addEventListener('input', (e) => {
    searchQuery = e.target.value;
    render();
    const restored = document.getElementById('global-search');
    if (restored) {
      restored.focus();
      restored.setSelectionRange(searchQuery.length, searchQuery.length);
    }
  });
  document.getElementById('btn-clear-search')?.addEventListener('click', () => { searchQuery = ''; render(); });
  document.getElementById('btn-clear-history')?.addEventListener('click', () => {
    state.history = [];
    state.recentTools = [];
    persistAndRender();
    showToast(t(state.language).historyClearedToast);
  });

  document.querySelectorAll('[data-set-theme]').forEach((el) => {
    el.addEventListener('click', () => { state.theme = el.dataset.setTheme; persistAndRender(); });
  });
  document.querySelectorAll('[data-set-lang]').forEach((el) => {
    el.addEventListener('click', () => { setLanguage(el.dataset.setLang); });
  });

  document.getElementById('sub-form')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    try {
      await submitSubscriptionVerification(state, {
        paymentMethod: document.getElementById('sub-method').value,
        referenceId: document.getElementById('sub-ref').value,
        consentAccepted: document.getElementById('sub-consent').checked,
        notes: 'Web subscription request'
      });
      persistAndRender();
      showToast(t(state.language).subSubmittedToast);
    } catch (err) {
      showToast(err.message);
    }
  });
  document.getElementById('btn-cancel-subscription')?.addEventListener('click', () => {
    cancelSubscription(state);
    persistAndRender();
    showToast(t(state.language).subCancelledToast);
  });
  document.querySelectorAll('[data-refund-id]').forEach((el) => {
    el.addEventListener('click', () => {
      requestRefund(state, el.dataset.refundId);
      persistAndRender();
      showToast(t(state.language).refundRequestedToast);
    });
  });

  if (activeTool) bindActiveToolEvents(activeTool);
}

function bindActiveToolEvents(tool) {
  let selectedFiles = [];
  const dropZone = document.getElementById('ws-upload-zone');
  const fileInput = document.getElementById('ws-file-input');
  const outEl = document.getElementById('ws-output');

  const refreshFileList = () => {
    const listEl = document.getElementById('ws-file-list');
    if (!listEl) return;
    listEl.innerHTML = selectedFiles.map((f, i) => `
      <div style="display:flex;justify-content:space-between;align-items:center;border:1px solid var(--border);padding:6px 10px;border-radius:10px;font-size:0.84rem;">
        <span>${i + 1}. ${escapeHtml(f.name)} (${formatBytes(f.size)})</span>
        ${tool.id === 'pdf_merge' ? `
          <span style="display:flex;gap:4px;">
            <button class="btn btn-outline btn-sm" data-move-up="${i}" ${i === 0 ? 'disabled' : ''}>↑</button>
            <button class="btn btn-outline btn-sm" data-move-down="${i}" ${i === selectedFiles.length - 1 ? 'disabled' : ''}>↓</button>
          </span>` : ''}
      </div>
    `).join('');
    listEl.querySelectorAll('[data-move-up]').forEach((b) => b.addEventListener('click', () => {
      const idx = Number(b.dataset.moveUp);
      [selectedFiles[idx - 1], selectedFiles[idx]] = [selectedFiles[idx], selectedFiles[idx - 1]];
      refreshFileList();
    }));
    listEl.querySelectorAll('[data-move-down]').forEach((b) => b.addEventListener('click', () => {
      const idx = Number(b.dataset.moveDown);
      [selectedFiles[idx], selectedFiles[idx + 1]] = [selectedFiles[idx + 1], selectedFiles[idx]];
      refreshFileList();
    }));
  };

  if (dropZone && fileInput) {
    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
    dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
    dropZone.addEventListener('drop', async (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
      selectedFiles = Array.from(e.dataTransfer.files || []);
      refreshFileList();
      if (tool.category === 'image' && selectedFiles[0]) {
        const info = await inspectImageFile(selectedFiles[0]);
        document.getElementById('ws-img-meta').textContent = `${info.width}×${info.height} • ${info.megapixels} • ${formatBytes(info.bytes)}`;
      }
    });
    fileInput.addEventListener('change', async () => {
      selectedFiles = Array.from(fileInput.files || []);
      refreshFileList();
      if (tool.category === 'image' && selectedFiles[0]) {
        const info = await inspectImageFile(selectedFiles[0]);
        document.getElementById('ws-img-meta').textContent = `${info.width}×${info.height} • ${info.megapixels} • ${formatBytes(info.bytes)}`;
      }
    });
  }

  document.getElementById('ws-reset')?.addEventListener('click', () => {
    selectedFiles = [];
    if (fileInput) fileInput.value = '';
    if (outEl) outEl.innerHTML = '';
    refreshFileList();
    const txtIn = document.getElementById('ws-txt-in');
    const txtOut = document.getElementById('ws-txt-out');
    if (txtIn) txtIn.value = '';
    if (txtOut) txtOut.value = '';
  });

  document.getElementById('ws-run-pdf')?.addEventListener('click', async () => {
    const s = t(state.language);
    try {
      outEl.innerHTML = `<p class="muted">${escapeHtml(s.processingText)}</p>`;
      const res = await runPdfTool(tool.id, selectedFiles, {
        rotateDeg: document.getElementById('ws-pdf-rot')?.value,
        pageSpec: document.getElementById('ws-pdf-pages')?.value,
        watermarkText: document.getElementById('ws-pdf-wm')?.value
      });
      recordHistory(tool, res[0].summary);
      outEl.innerHTML = res.map((item, idx) => `
        <div class="card" style="background:var(--surface-soft);margin-bottom:8px;">
          <div><strong>${escapeHtml(item.name)}</strong></div>
          <div class="muted" style="font-size:0.84rem;margin:4px 0 8px;">${escapeHtml(item.summary)}</div>
          ${item.previewUrl ? `<iframe src="${item.previewUrl}" title="PDF Preview" style="width:100%;height:280px;border:1px solid var(--border);border-radius:10px;margin-bottom:8px;"></iframe>` : ''}
          <button class="btn btn-primary btn-sm" data-dl-pdf="${idx}">${escapeHtml(`${s.downloadPrefix}${item.name}`)}</button>
        </div>
      `).join('');
      outEl.querySelectorAll('[data-dl-pdf]').forEach((b) => b.addEventListener('click', () => {
        const item = res[Number(b.dataset.dlPdf)];
        downloadBlob(item.blob, item.name);
      }));
    } catch (err) {
      outEl.innerHTML = `<p style="color:var(--danger);">${escapeHtml(err.message)}</p>`;
    }
  });

  const qRange = document.getElementById('ws-img-quality');
  qRange?.addEventListener('input', () => { document.getElementById('ws-q-val').textContent = qRange.value; });

  document.getElementById('ws-run-img')?.addEventListener('click', async () => {
    const s = t(state.language);
    try {
      if (!selectedFiles[0]) throw new Error(s.selectImageError);
      const flip = document.getElementById('ws-img-flip')?.value;
      const res = await processImageFile(selectedFiles[0], tool.id, {
        quality: qRange?.value || 80,
        width: document.getElementById('ws-img-w')?.value,
        height: document.getElementById('ws-img-h')?.value,
        ratio: document.getElementById('ws-img-crop')?.value,
        rotateDeg: document.getElementById('ws-img-rot')?.value,
        flipH: flip === 'h',
        flipV: flip === 'v'
      });
      recordHistory(tool, `${res.width}×${res.height} • ${formatBytes(res.outputBytes)}`);
      outEl.innerHTML = `
        <div class="card" style="background:var(--surface-soft);display:grid;gap:10px;">
          <div class="mono" style="font-size:0.84rem;">${res.width}×${res.height} • ${formatBytes(res.outputBytes)} (${formatBytes(res.originalBytes)})</div>
          <img src="${res.previewUrl}" alt="Processed preview" style="max-width:100%;max-height:240px;object-fit:contain;border-radius:10px;" />
          <button class="btn btn-primary btn-sm" id="ws-dl-img">${escapeHtml(`${s.downloadPrefix}${res.name}`)}</button>
        </div>
      `;
      document.getElementById('ws-dl-img').addEventListener('click', () => downloadBlob(res.blob, res.name));
    } catch (err) {
      outEl.innerHTML = `<p style="color:var(--danger);">${escapeHtml(err.message)}</p>`;
    }
  });

  if (tool.category === 'text') {
    const s = t(state.language);
    const inEl = document.getElementById('ws-txt-in');
    const outTxt = document.getElementById('ws-txt-out');
    const statsEl = document.getElementById('ws-txt-stats');
    const updateStats = () => {
      const st = analyzeText(inEl.value);
      statsEl.textContent = state.language === 'hi'
        ? `शब्द: ${st.words} • अक्षर: ${st.charsWithSpaces} (बिना स्पेस ${st.charsNoSpaces}) • पंक्तियाँ: ${st.lines} • ~${st.readMins} मिनट पठन`
        : `Words: ${st.words} • Chars: ${st.charsWithSpaces} (${st.charsNoSpaces} no spaces) • Lines: ${st.lines} • ~${st.readMins}m read`;
    };
    updateStats();
    inEl.addEventListener('input', updateStats);
    document.querySelectorAll('[data-txt-op]').forEach((btn) => btn.addEventListener('click', () => {
      try {
        outTxt.value = transformText(btn.dataset.txtOp, inEl.value, document.getElementById('ws-txt-b')?.value || '');
        recordHistory(tool, `${btn.dataset.txtOp} (${analyzeText(outTxt.value).words})`);
      } catch (err) {
        showToast(err.message);
      }
    }));
    document.getElementById('ws-txt-copy')?.addEventListener('click', () => {
      navigator.clipboard?.writeText(outTxt.value || inEl.value);
      showToast(s.copiedToast);
    });
    const speak = (lang) => {
      if (!('speechSynthesis' in window)) return showToast(s.speechUnavailable);
      window.speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(inEl.value);
      u.lang = lang;
      window.speechSynthesis.speak(u);
    };
    document.getElementById('ws-tts-en')?.addEventListener('click', () => speak('en-IN'));
    document.getElementById('ws-tts-hi')?.addEventListener('click', () => speak('hi-IN'));
  }

  if (tool.category === 'qr') {
    const s = t(state.language);
    const qf = s.qrFields;
    const typeSel = document.getElementById('ws-qr-type');
    const fieldsBox = document.getElementById('ws-qr-fields');
    const renderFields = () => {
      const tp = typeSel.value;
      if (tp === 'upi') {
        fieldsBox.innerHTML = `<input id="qr-vpa" class="input" placeholder="${escapeHtml(qf.vpa)}" /><input id="qr-pn" class="input" placeholder="${escapeHtml(qf.payee)}" /><input id="qr-am" class="input" type="number" placeholder="${escapeHtml(qf.amount)}" />`;
      } else if (tp === 'wifi') {
        fieldsBox.innerHTML = `<input id="qr-ssid" class="input" placeholder="${escapeHtml(qf.ssid)}" /><input id="qr-pass" class="input" placeholder="${escapeHtml(qf.password)}" />`;
      } else if (tp === 'whatsapp') {
        fieldsBox.innerHTML = `<input id="qr-waph" class="input" placeholder="${escapeHtml(qf.waPhone)}" /><input id="qr-wamsg" class="input" placeholder="${escapeHtml(qf.waMsg)}" />`;
      } else if (tp === 'email') {
        fieldsBox.innerHTML = `<input id="qr-em" class="input" placeholder="${escapeHtml(qf.email)}" /><input id="qr-emsub" class="input" placeholder="${escapeHtml(qf.subject)}" />`;
      } else if (tp === 'phone') {
        fieldsBox.innerHTML = `<input id="qr-ph" class="input" placeholder="+919876543210" />`;
      } else if (tp === 'text') {
        fieldsBox.innerHTML = `<textarea id="qr-txt" class="textarea" placeholder="${escapeHtml(qf.text)}">${escapeHtml(s.appName)}</textarea>`;
      } else {
        fieldsBox.innerHTML = `<input id="qr-url" class="input" value="https://bharattools.in" placeholder="${escapeHtml(qf.url)}" />`;
      }
    };
    renderFields();
    typeSel.addEventListener('change', renderFields);
    document.getElementById('ws-run-qr').addEventListener('click', () => {
      try {
        const payload = buildQrPayload(typeSel.value, {
          url: document.getElementById('qr-url')?.value,
          text: document.getElementById('qr-txt')?.value,
          phone: document.getElementById('qr-ph')?.value,
          ssid: document.getElementById('qr-ssid')?.value,
          password: document.getElementById('qr-pass')?.value,
          email: document.getElementById('qr-em')?.value,
          subject: document.getElementById('qr-emsub')?.value,
          waPhone: document.getElementById('qr-waph')?.value,
          waMsg: document.getElementById('qr-wamsg')?.value,
          vpa: document.getElementById('qr-vpa')?.value,
          payee: document.getElementById('qr-pn')?.value,
          amount: document.getElementById('qr-am')?.value
        });
        const fg = document.getElementById('ws-qr-fg').value;
        const bg = document.getElementById('ws-qr-bg').value;
        const preview = document.getElementById('ws-qr-preview');
        preview.innerHTML = `<canvas id="qr-canvas" style="border-radius:12px;border:1px solid var(--border);"></canvas><div class="mono muted" style="font-size:0.78rem;word-break:break-all;">${escapeHtml(payload)}</div><div style="display:flex;gap:8px;"><button class="btn btn-primary btn-sm" id="qr-dl-png">${escapeHtml(s.downloadPngBtn)}</button><button class="btn btn-outline btn-sm" id="qr-dl-svg">${escapeHtml(s.downloadSvgBtn)} ${hasActiveSubscription(state) ? '' : `(${escapeHtml(s.priceShort)})`}</button></div>`;
        const canvas = document.getElementById('qr-canvas');
        renderQrToCanvas(canvas, payload, fg, bg);
        recordHistory(tool, payload);
        document.getElementById('qr-dl-png').addEventListener('click', () => canvas.toBlob((b) => downloadBlob(b, 'bharat_qr.png')));
        document.getElementById('qr-dl-svg').addEventListener('click', () => {
          if (!hasActiveSubscription(state)) return showToast(s.svgProToast);
          const svg = renderQrToSvg(payload, fg, bg);
          downloadBlob(new Blob([svg], { type: 'image/svg+xml' }), 'bharat_qr.svg');
        });
      } catch (err) {
        showToast(err.message);
      }
    });
  }

  // Utilities
  document.getElementById('u-calc-run')?.addEventListener('click', () => {
    try {
      const res = evaluateExpression(document.getElementById('u-calc-expr').value);
      outEl.textContent = `= ${res}`;
      recordHistory(tool, `= ${res}`);
    } catch (e) { outEl.textContent = e.message; }
  });
  document.getElementById('u-gst-run')?.addEventListener('click', () => {
    try {
      const r = calculateGst(document.getElementById('u-gst-amt').value, document.getElementById('u-gst-rate').value, document.getElementById('u-gst-mode').value === 'incl');
      outEl.innerHTML = state.language === 'hi'
        ? `मूल राशि: ₹${r.base.toFixed(2)}<br>CGST: ₹${r.cgst.toFixed(2)} | SGST: ₹${r.sgst.toFixed(2)}<br>कुल: ₹${r.total.toFixed(2)}`
        : `Base: ₹${r.base.toFixed(2)}<br>CGST: ₹${r.cgst.toFixed(2)} | SGST: ₹${r.sgst.toFixed(2)}<br>Total: ₹${r.total.toFixed(2)}`;
      recordHistory(tool, `₹${r.total.toFixed(2)}`);
    } catch (e) { outEl.textContent = e.message; }
  });
  document.getElementById('u-emi-run')?.addEventListener('click', () => {
    try {
      const r = calculateEmi(document.getElementById('u-emi-p').value, document.getElementById('u-emi-r').value, document.getElementById('u-emi-n').value);
      outEl.innerHTML = state.language === 'hi'
        ? `मासिक EMI: ₹${r.emi.toFixed(2)}<br>कुल ब्याज: ₹${r.totalInterest.toFixed(2)}<br>कुल देय: ₹${r.totalPayment.toFixed(2)}`
        : `Monthly EMI: ₹${r.emi.toFixed(2)}<br>Total Interest: ₹${r.totalInterest.toFixed(2)}<br>Total Payable: ₹${r.totalPayment.toFixed(2)}`;
      recordHistory(tool, `EMI ₹${r.emi.toFixed(2)}`);
    } catch (e) { outEl.textContent = e.message; }
  });
  document.getElementById('u-pwd-run')?.addEventListener('click', () => {
    const n = Math.min(64, Math.max(6, Number(document.getElementById('u-pwd-len').value || 16)));
    if (tool.id === 'util_password') {
      const pwd = generateSecurePassword(n);
      outEl.textContent = pwd;
      recordHistory(tool, `${n}-char`);
    } else {
      const arr = new Uint32Array(1);
      crypto.getRandomValues(arr);
      const val = (arr[0] % n) + 1;
      outEl.textContent = `(1..${n}): ${val}`;
      recordHistory(tool, `${val}`);
    }
  });
  document.getElementById('u-fx-run')?.addEventListener('click', async () => {
    const u = t(state.language).utilities;
    outEl.textContent = u.fetchingFx;
    const amt = Number(document.getElementById('u-fx-amt').value || 1000);
    const fx = await fetchCurrencyRates('INR');
    const lines = ['USD', 'EUR', 'GBP', 'AED', 'SGD', 'JPY'].filter((c) => fx.rates[c]).map((c) => `${c}: ${(amt * fx.rates[c]).toFixed(2)}`);
    outEl.innerHTML = `₹${amt} INR (${fx.live ? 'Live ECB' : 'Offline Reference'} • ${fx.date})<br>${lines.join('<br>')}`;
  });
  document.getElementById('u-date-run')?.addEventListener('click', () => {
    try {
      if (tool.id === 'util_age') {
        const a = calculateAge(document.getElementById('u-date-a').value);
        outEl.textContent = state.language === 'hi'
          ? `${a.years} वर्ष, ${a.months} माह, ${a.days} दिन (कुल ${a.totalDays} दिन)`
          : `${a.years} years, ${a.months} months, ${a.days} days (${a.totalDays} days total)`;
      } else {
        const d1 = new Date(document.getElementById('u-date-a').value);
        const d2 = new Date(document.getElementById('u-date-b').value);
        const days = Math.round(Math.abs(d2 - d1) / (1000 * 60 * 60 * 24));
        outEl.textContent = state.language === 'hi'
          ? `${days} दिन (${(days / 7).toFixed(1)} सप्ताह)`
          : `${days} days (${(days / 7).toFixed(1)} weeks)`;
      }
    } catch (e) { outEl.textContent = e.message; }
  });
  document.getElementById('u-bmi-run')?.addEventListener('click', () => {
    const u = t(state.language).utilities;
    const kg = Number(document.getElementById('u-bmi-kg').value);
    const m = Number(document.getElementById('u-bmi-cm').value) / 100;
    if (!(kg > 0 && m > 0)) return (outEl.textContent = u.bmiInvalid);
    const bmi = kg / (m * m);
    outEl.textContent = `BMI: ${bmi.toFixed(1)} ${u.bmiInfoSuffix}`;
  });
  document.getElementById('u-uni-run')?.addEventListener('click', () => {
    const v = Number(document.getElementById('u-uni-val').value || 0);
    if (tool.id === 'util_unit') outEl.innerHTML = `${v} m = ${(v * 3.28084).toFixed(2)} ft | ${v} Gaj = ${(v * 9).toFixed(1)} sq.ft | ${v} kg = ${(v * 2.20462).toFixed(2)} lb`;
    else if (tool.id === 'util_percent') outEl.textContent = `18% × ${v} = ${(v * 0.18).toFixed(2)} | ${v} + 18% = ${(v * 1.18).toFixed(2)}`;
    else {
      const now = new Date();
      outEl.innerHTML = `IST: ${now.toLocaleTimeString('en-IN', { timeZone: 'Asia/Kolkata' })}<br>UTC: ${now.toLocaleTimeString('en-GB', { timeZone: 'UTC' })}<br>EST: ${now.toLocaleTimeString('en-US', { timeZone: 'America/New_York' })}`;
    }
  });
  if (tool.id === 'util_stopwatch' || tool.id === 'util_timer') {
    let ms = tool.id === 'util_timer' ? 300000 : 0;
    const disp = document.getElementById('u-clock-disp');
    const fmtClock = () => {
      const s = Math.floor(ms / 1000);
      const m = Math.floor(s / 60);
      const rem = s % 60;
      disp.textContent = `${String(m).padStart(2, '0')}:${String(rem).padStart(2, '0')}`;
    };
    document.getElementById('u-clock-start').addEventListener('click', () => {
      clearInterval(clockInterval);
      clockInterval = setInterval(() => {
        ms = tool.id === 'util_timer' ? Math.max(0, ms - 1000) : ms + 1000;
        fmtClock();
      }, 1000);
    });
    document.getElementById('u-clock-stop').addEventListener('click', () => clearInterval(clockInterval));
    document.getElementById('u-clock-reset').addEventListener('click', () => {
      clearInterval(clockInterval);
      ms = tool.id === 'util_timer' ? 300000 : 0;
      fmtClock();
    });
  }
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./service-worker.js').catch(() => {});
  });
}

applyTheme();
applyLanguageMeta();
render();
