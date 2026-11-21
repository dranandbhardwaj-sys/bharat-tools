import { t } from './js/i18n.js';
import { CATEGORIES, TOOLS, getToolById } from './js/catalog.js';
import {
  loadState,
  saveState,
  resetAllLocalState,
  hasActivePremium,
  submitMembershipRequest,
  cancelMembershipPlan,
  getRuntimeSessionToken,
  setRuntimeSessionToken
} from './js/storage.js';
import { AI_PROVIDERS, generateCreatorContent, testAiConnection } from './js/ai-service.js';

let state = loadState();
let currentView = 'dashboard'; // dashboard | all | workspace | favorites | history | membership | settings
let activeCatFilter = 'all';
let searchQuery = '';
let activeTool = null;
let mobileDrawerOpen = false;
let isGenerating = false;

// In-progress modal form memory so re-renders (theme/lang toggle) keep current draft output
let modalDraft = {
  topic: '',
  context: '',
  tone: 'professional',
  outLang: 'en',
  length: 'medium',
  output: '',
  sourceNote: ''
};

const root = document.getElementById('app-root');
const toastEl = document.getElementById('toast-root');
let toastTimer = null;

function esc(v) {
  return String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function showToast(msg) {
  toastEl.textContent = msg;
  toastEl.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toastEl.classList.add('hidden'), 3000);
}

function applyThemeAndLang() {
  const mode = state.theme || 'dark';
  const dark = mode === 'dark' || (mode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
  document.documentElement.lang = state.language === 'hi' ? 'hi' : 'en';
  document.title = t(state.language).pageTitle;
}

function persistAndRender() {
  saveState(state);
  applyThemeAndLang();
  render();
}

function toolTitle(tool) {
  return state.language === 'hi' ? tool.titleHi : tool.titleEn;
}

function toolDesc(tool) {
  return state.language === 'hi' ? tool.descHi : tool.descEn;
}

function catTitle(cat) {
  return state.language === 'hi' ? cat.titleHi : cat.titleEn;
}

function downloadFile(content, filename, mime = 'text/plain;charset=utf-8') {
  const blob = new Blob([content], { type: mime });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 4000);
}

function openToolModal(tool, prefillTopic = '', prefillOutput = '') {
  activeTool = tool;
  modalDraft = {
    topic: prefillTopic,
    context: state.profile?.niche ? `${state.profile.niche}` : '',
    tone: 'professional',
    outLang: state.language === 'hi' ? 'hi' : 'en',
    length: 'medium',
    output: prefillOutput,
    sourceNote: ''
  };
  mobileDrawerOpen = false;
  render();
}

function renderLangSwitchHtml() {
  const s = t(state.language);
  return `
    <div class="lang-switch" role="group" aria-label="${esc(s.langSwitchAria)}">
      <button type="button" class="lang-switch-btn ${state.language === 'en' ? 'active' : ''}" data-set-lang="en">${esc(s.langEn)}</button>
      <button type="button" class="lang-switch-btn ${state.language === 'hi' ? 'active' : ''}" data-set-lang="hi">${esc(s.langHi)}</button>
    </div>
  `;
}

function renderToolCard(tool) {
  const s = t(state.language);
  const cat = CATEGORIES.find((c) => c.id === tool.cat);
  const isFav = state.favorites.includes(tool.id);
  return `
    <article class="tool-card" data-open-tool="${tool.id}" tabindex="0" role="button" aria-label="${esc(toolTitle(tool))}">
      <div class="tool-card-top">
        <div class="tool-icon-box" style="background:${cat.color}20;color:${cat.color};">${cat.icon}</div>
        <div style="display:flex;align-items:center;gap:6px;">
          <span class="badge ${tool.tier === 'pro' ? 'badge-accent' : 'badge-success'}">${esc(tool.tier === 'pro' ? s.proTag : s.freeTag)}</span>
          <button type="button" class="fav-btn ${isFav ? 'active' : ''}" data-fav-tool="${tool.id}" aria-label="${esc(s.favoritesHeading)}">${isFav ? '★' : '☆'}</button>
        </div>
      </div>
      <div>
        <h3 style="font-size:1rem;">${esc(toolTitle(tool))}</h3>
        <p class="muted" style="font-size:0.84rem;margin-top:4px;">${esc(toolDesc(tool))}</p>
      </div>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-top:auto;padding-top:4px;">
        <span style="font-size:0.76rem;font-weight:700;color:${cat.color};">${esc(catTitle(cat))}</span>
        <span style="font-size:0.82rem;font-weight:600;color:var(--secondary);">${esc(s.openToolBtn)} →</span>
      </div>
    </article>
  `;
}

function renderSidebar() {
  const s = t(state.language);
  const primaryNav = [
    ['dashboard', `🏠 ${s.navDashboard}`, null],
    ['all', `🧰 ${s.navAllTools}`, TOOLS.length],
    ['workspace', `📝 ${s.navWorkspace}`, state.drafts.length],
    ['favorites', `★ ${s.navFavorites}`, state.favorites.length],
    ['history', `🕒 ${s.navHistory}`, state.history.length],
    ['membership', `💎 ${s.navMembership}`, null],
    ['settings', `⚙️ ${s.navSettings}`, null]
  ];

  return `
    ${mobileDrawerOpen ? '<div class="drawer-scrim" id="drawer-scrim"></div>' : ''}
    <aside class="sidebar ${mobileDrawerOpen ? 'open' : ''}" aria-label="Sidebar Navigation">
      <div class="brand-mark" style="padding:4px 6px 12px;">
        <img src="./assets/logo.svg" alt="${esc(s.appName)}" />
        <div>
          <div style="font-size:0.96rem;">${esc(s.appName)}</div>
          <div class="muted" style="font-size:0.74rem;">${esc(s.appSub)}</div>
        </div>
      </div>
      ${primaryNav.map(([route, label, count]) => {
        const active = currentView === route && (route !== 'all' || activeCatFilter === 'all');
        return `
          <button type="button" class="sidebar-item ${active ? 'active' : ''}" data-nav="${route}">
            <span>${esc(label)}</span>
            ${count !== null ? `<span class="sidebar-count">${count}</span>` : ''}
          </button>
        `;
      }).join('')}
      <div class="sidebar-section-label">${esc(s.navCategoriesLabel)}</div>
      ${CATEGORIES.map((c) => {
        const cnt = TOOLS.filter((x) => x.cat === c.id).length;
        const active = currentView === 'all' && activeCatFilter === c.id;
        return `
          <button type="button" class="sidebar-item ${active ? 'active' : ''}" data-cat-nav="${c.id}">
            <span>${c.icon} ${esc(catTitle(c))}</span>
            <span class="sidebar-count">${cnt}</span>
          </button>
        `;
      }).join('')}
    </aside>
  `;
}

function renderDashboardOrTools(showFullWelcome) {
  const s = t(state.language);
  const q = searchQuery.trim().toLowerCase();
  const filtered = TOOLS.filter((tool) => {
    const catOk = activeCatFilter === 'all' || tool.cat === activeCatFilter;
    const qOk =
      !q ||
      tool.titleEn.toLowerCase().includes(q) ||
      tool.titleHi.toLowerCase().includes(q) ||
      tool.descEn.toLowerCase().includes(q) ||
      tool.descHi.toLowerCase().includes(q) ||
      tool.cat.includes(q);
    return catOk && qOk;
  });

  const quickTools = TOOLS.filter((x) => x.quick);
  const favTools = state.favorites.map(getToolById).filter(Boolean).slice(0, 4);
  const recentItems = state.history.slice(0, 4);
  const activeCatObj = CATEGORIES.find((c) => c.id === activeCatFilter);

  return `
    ${showFullWelcome ? `
      <section class="hero-banner">
        <div>
          <span class="badge" style="background:rgba(255,255,255,0.16);color:#FFF;">${esc(s.heroBadge)}</span>
          <h1 style="margin-top:12px;">${esc(s.heroTitle)}</h1>
          <p>${esc(s.heroSubtitle)}</p>
          <div style="display:flex;flex-wrap:wrap;gap:10px;margin-top:18px;">
            <button type="button" class="btn btn-primary" id="btn-hero-all">${esc(s.heroCtaPrimary)}</button>
            <button type="button" class="btn btn-outline" style="background:rgba(255,255,255,0.12);color:#FFF;border-color:rgba(255,255,255,0.28);" data-nav="settings">${esc(s.heroCtaSecondary)}</button>
          </div>
        </div>
        <img src="./assets/illustrations/hero-creator.svg" alt="${esc(s.appName)}" />
      </section>

      <section class="stats-grid">
        <div class="stat-card">
          <span class="muted" style="font-size:0.8rem;">${esc(s.statTotalRuns)}</span>
          <span class="stat-value">${state.usage.totalGenerations}</span>
        </div>
        <div class="stat-card">
          <span class="muted" style="font-size:0.8rem;">${esc(s.statTodayRuns)}</span>
          <span class="stat-value">${state.usage.todayCount}</span>
        </div>
        <div class="stat-card">
          <span class="muted" style="font-size:0.8rem;">${esc(s.statSavedDrafts)}</span>
          <span class="stat-value">${state.drafts.length}</span>
        </div>
        <div class="stat-card">
          <span class="muted" style="font-size:0.8rem;">${esc(s.statFavorites)}</span>
          <span class="stat-value">${state.favorites.length}</span>
        </div>
      </section>

      <section>
        <h2 style="font-size:1.15rem;margin-bottom:12px;">${esc(s.quickCreateHeading)}</h2>
        <div class="tools-grid">${quickTools.map(renderToolCard).join('')}</div>
      </section>

      <div class="grid-2">
        <section class="card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
            <h3 style="font-size:1.02rem;">${esc(s.recentContentHeading)}</h3>
            <button type="button" class="btn btn-soft btn-sm" data-nav="history">${esc(s.navHistory)}</button>
          </div>
          ${recentItems.length === 0 ? `<p class="muted" style="font-size:0.88rem;">${esc(s.noRecentContent)}</p>` : `
            <div style="display:grid;gap:8px;">
              ${recentItems.map((h) => {
                const tl = getToolById(h.toolId);
                return `
                  <div style="border:1px solid var(--border);border-radius:12px;padding:10px 12px;display:flex;justify-content:space-between;align-items:center;gap:10px;">
                    <div style="min-width:0;">
                      <strong style="font-size:0.88rem;">${esc(tl ? toolTitle(tl) : h.toolTitle)}</strong>
                      <div class="muted" style="font-size:0.8rem;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${esc(h.topic)}</div>
                    </div>
                    <button type="button" class="btn btn-outline btn-sm" data-restore-history="${h.id}">${esc(s.restoreBtn)}</button>
                  </div>
                `;
              }).join('')}
            </div>
          `}
        </section>

        <section class="card">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
            <h3 style="font-size:1.02rem;">${esc(s.favoritesHeading)}</h3>
            <button type="button" class="btn btn-soft btn-sm" data-nav="favorites">${esc(s.navFavorites)}</button>
          </div>
          ${favTools.length === 0 ? `<p class="muted" style="font-size:0.88rem;">${esc(s.noFavorites)}</p>` : `
            <div style="display:grid;gap:8px;">
              ${favTools.map((tl) => `
                <div style="border:1px solid var(--border);border-radius:12px;padding:10px 12px;display:flex;justify-content:space-between;align-items:center;gap:10px;">
                  <div>
                    <strong style="font-size:0.88rem;">${esc(toolTitle(tl))}</strong>
                    <div class="muted" style="font-size:0.78rem;">${esc(toolDesc(tl))}</div>
                  </div>
                  <button type="button" class="btn btn-primary btn-sm" data-open-tool="${tl.id}">${esc(s.openToolBtn)}</button>
                </div>
              `).join('')}
            </div>
          `}
        </section>
      </div>
    ` : ''}

    <section style="display:grid;gap:12px;">
      <div style="display:flex;gap:8px;">
        <input id="tool-search" class="input" type="search" value="${esc(searchQuery)}" placeholder="${esc(s.searchPlaceholder)}" aria-label="${esc(s.searchAria)}" />
        ${searchQuery ? `<button type="button" class="btn btn-outline" id="btn-clear-search">${esc(s.clearBtn)}</button>` : ''}
      </div>
      <div class="chips-row">
        <button type="button" class="chip ${activeCatFilter === 'all' ? 'active' : ''}" data-filter-cat="all">${esc(s.allFilter)} (${TOOLS.length})</button>
        ${CATEGORIES.map((c) => `
          <button type="button" class="chip ${activeCatFilter === c.id ? 'active' : ''}" data-filter-cat="${c.id}">${c.icon} ${esc(catTitle(c))}</button>
        `).join('')}
      </div>
    </section>

    <section>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
        <h2 style="font-size:1.15rem;">${esc(activeCatObj ? catTitle(activeCatObj) : s.allToolsHeading)}</h2>
        <span class="badge">${filtered.length} ${esc(s.toolsUnit)}</span>
      </div>
      ${filtered.length === 0 ? `
        <div class="card" style="text-align:center;">
          <p class="muted">${esc(s.noSearchMatch)}</p>
        </div>
      ` : `<div class="tools-grid">${filtered.map(renderToolCard).join('')}</div>`}
    </section>
  `;
}

function renderWorkspaceView() {
  const s = t(state.language);
  return `
    <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px;">
      <div>
        <h2>${esc(s.workspaceTitle)}</h2>
        <p class="muted" style="font-size:0.9rem;">${esc(s.workspaceSubtitle)}</p>
      </div>
      <div style="display:flex;flex-wrap:wrap;gap:8px;">
        <button type="button" class="btn btn-primary btn-sm" id="btn-new-draft">${esc(s.newDraftBtn)}</button>
        <button type="button" class="btn btn-outline btn-sm" id="btn-export-json">${esc(s.exportAllJsonBtn)}</button>
        <button type="button" class="btn btn-outline btn-sm" id="btn-export-md">${esc(s.exportAllMdBtn)}</button>
      </div>
    </div>

    ${state.drafts.length === 0 ? `
      <div class="card" style="text-align:center;display:grid;place-items:center;gap:12px;padding:30px;">
        <img src="./assets/illustrations/workspace-empty.svg" alt="" style="max-width:220px;" />
        <p class="muted" style="max-width:460px;">${esc(s.noDrafts)}</p>
        <button type="button" class="btn btn-primary btn-sm" id="btn-new-draft-empty">${esc(s.newDraftBtn)}</button>
      </div>
    ` : `
      <div style="display:grid;gap:14px;">
        ${state.drafts.map((d) => `
          <div class="card" style="display:grid;gap:10px;">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
              <span class="badge badge-primary">${esc(d.toolTitle || 'Draft')}</span>
              <span class="mono muted" style="font-size:0.78rem;">${esc(d.updatedAt)}</span>
            </div>
            <input class="input" data-draft-title="${d.id}" value="${esc(d.title)}" placeholder="${esc(s.draftTitlePlaceholder)}" />
            <textarea class="textarea" data-draft-body="${d.id}" placeholder="${esc(s.draftBodyPlaceholder)}">${esc(d.content)}</textarea>
            <div style="display:flex;flex-wrap:wrap;gap:8px;">
              <button type="button" class="btn btn-primary btn-sm" data-save-draft="${d.id}">${esc(s.saveChangesBtn)}</button>
              <button type="button" class="btn btn-outline btn-sm" data-copy-draft="${d.id}">${esc(s.copyBtn)}</button>
              <button type="button" class="btn btn-outline btn-sm" data-dl-draft="${d.id}">${esc(s.downloadTxtBtn)}</button>
              <button type="button" class="btn btn-danger btn-sm" data-del-draft="${d.id}">${esc(s.deleteBtn)}</button>
            </div>
          </div>
        `).join('')}
      </div>
    `}
  `;
}

function renderFavoritesView() {
  const s = t(state.language);
  const favs = state.favorites.map(getToolById).filter(Boolean);
  return `
    <h2>${esc(s.favoritesHeading)} (${favs.length})</h2>
    ${favs.length === 0
      ? `<div class="card"><p class="muted">${esc(s.noFavorites)}</p></div>`
      : `<div class="tools-grid">${favs.map(renderToolCard).join('')}</div>`}
  `;
}

function renderHistoryView() {
  const s = t(state.language);
  return `
    <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
      <h2>${esc(s.historyTitle)} (${state.history.length})</h2>
      ${state.history.length ? `<button type="button" class="btn btn-danger btn-sm" id="btn-clear-history">${esc(s.clearHistoryBtn)}</button>` : ''}
    </div>
    ${state.history.length === 0 ? `<div class="card"><p class="muted">${esc(s.noHistory)}</p></div>` : `
      <div style="display:grid;gap:12px;">
        ${state.history.map((h) => {
          const tl = getToolById(h.toolId);
          return `
            <div class="card" style="display:grid;gap:8px;">
              <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;">
                <strong>${esc(tl ? toolTitle(tl) : h.toolTitle)}</strong>
                <span class="badge ${h.source === 'api' ? 'badge-success' : 'badge-warning'}">${esc(h.source === 'api' ? 'API' : 'DEMO')} • ${esc(h.createdAt)}</span>
              </div>
              <div class="muted" style="font-size:0.84rem;">${esc(h.topic)}</div>
              <pre style="margin:0;white-space:pre-wrap;font-family:inherit;font-size:0.86rem;background:var(--surface-soft);padding:12px;border-radius:12px;max-height:160px;overflow:auto;">${esc(h.output)}</pre>
              <div style="display:flex;flex-wrap:wrap;gap:8px;">
                <button type="button" class="btn btn-primary btn-sm" data-restore-history="${h.id}">${esc(s.restoreBtn)}</button>
                <button type="button" class="btn btn-outline btn-sm" data-copy-history="${h.id}">${esc(s.copyBtn)}</button>
                <button type="button" class="btn btn-outline btn-sm" data-dl-history="${h.id}">${esc(s.downloadTxtBtn)}</button>
                <button type="button" class="btn btn-outline btn-sm" data-md-history="${h.id}">${esc(s.downloadMdBtn)}</button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `}
  `;
}

function renderMembershipView() {
  const s = t(state.language);
  const sub = state.subscription;
  const active = hasActivePremium(state);
  const statusBadge = active
    ? s.activePlanBadge
    : sub.status === 'pending'
      ? s.pendingPlanBadge
      : sub.status === 'cancelled'
        ? s.cancelledPlanBadge
        : s.freePlanBadge;
  const renewalText = active && sub.renewalDateIso
    ? `${s.subRenewalActivePrefix}${new Date(sub.renewalDateIso).toLocaleDateString()}`
    : sub.status === 'pending'
      ? s.subRenewalPending
      : s.subRenewalOff;

  return `
    <div style="display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px;">
      <div>
        <h2>${esc(s.membershipTitle)}</h2>
        <p class="muted">${esc(s.membershipSubtitle)}</p>
      </div>
      <span class="badge ${active ? 'badge-success' : sub.status === 'pending' ? 'badge-warning' : sub.status === 'cancelled' ? 'badge-danger' : 'badge-primary'}">${esc(s.currentPlanLabel)}: ${esc(statusBadge)}</span>
    </div>

    <!-- Subscription Status & Renewal Information Page Section -->
    <div class="card" style="display:grid;gap:14px;">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
        <h3>${esc(s.subStatusHeading)}</h3>
        ${sub.status === 'pending' || active ? `<button type="button" class="btn btn-danger btn-sm" id="btn-cancel-status">${esc(s.cancelSubBtn)}</button>` : ''}
      </div>
      <div class="stats-grid">
        <div class="stat-card" style="background:var(--surface-soft);">
          <span class="muted" style="font-size:0.78rem;">${esc(s.subPlanLabel)}</span>
          <strong style="font-size:1rem;">${esc(active ? s.planProName : sub.status === 'pending' ? `${s.planProName} (${s.pendingPlanBadge})` : s.planFreeName)}</strong>
        </div>
        <div class="stat-card" style="background:var(--surface-soft);">
          <span class="muted" style="font-size:0.78rem;">${esc(s.subPriceLabel)}</span>
          <strong class="mono" style="font-size:1rem;">${esc(s.subPriceValue)}</strong>
        </div>
        <div class="stat-card" style="background:var(--surface-soft);">
          <span class="muted" style="font-size:0.78rem;">${esc(s.subDurationLabel)}</span>
          <strong style="font-size:1rem;">${esc(s.subDurationValue)}</strong>
        </div>
        <div class="stat-card" style="background:var(--surface-soft);">
          <span class="muted" style="font-size:0.78rem;">${esc(s.subUsageLabel)}</span>
          <strong style="font-size:0.95rem;">${esc(active ? s.subUsagePremium : s.subUsageFree)}</strong>
        </div>
      </div>
      <div style="border:1px solid var(--border);border-radius:12px;padding:12px;font-size:0.88rem;">
        <strong>${esc(s.subRenewalLabel)}:</strong> <span class="muted">${esc(renewalText)}</span>
      </div>
    </div>

    <div class="grid-2">
      <div class="card" style="display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3>${esc(s.planFreeName)}</h3>
          <span class="badge badge-primary">${esc(s.freeTag)}</span>
        </div>
        <div class="mono" style="font-size:1.85rem;font-weight:700;">${esc(s.planFreePrice)}</div>
        <ul class="muted" style="padding-left:18px;display:grid;gap:8px;font-size:0.9rem;">
          ${s.planFreeFeatures.map((f) => `<li>${esc(f)}</li>`).join('')}
        </ul>
      </div>

      <div class="card" style="border-color:var(--accent);display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <h3>${esc(s.planProName)}</h3>
          <span class="badge badge-accent">${esc(s.proTag)}</span>
        </div>
        <div class="mono" style="font-size:1.85rem;font-weight:700;color:var(--accent);">${esc(s.planProPrice)}</div>
        <div class="muted" style="font-size:0.82rem;margin-top:-8px;">${esc(s.planProCurrencyNote)}</div>
        <ul class="muted" style="padding-left:18px;display:grid;gap:6px;font-size:0.88rem;">
          ${s.planProFeatures.map((f) => `<li>${esc(f)}</li>`).join('')}
        </ul>
        <button type="button" class="btn btn-primary" id="btn-focus-billing">${esc(s.upgradeBtn)}</button>
      </div>
    </div>

    <div class="card" style="display:grid;gap:12px;">
      <h3>${esc(s.billingSectionTitle)}</h3>
      <p class="muted" style="font-size:0.86rem;">${esc(s.billingSecurityNote)}</p>
      <form id="membership-form" style="display:grid;gap:12px;">
        <div class="grid-2">
          <div>
            <label class="field-label" for="bill-method">${esc(s.paymentMethodLabel)}</label>
            <select id="bill-method" class="select">
              <option value="UPI Autopay">UPI Autopay (₹399/month)</option>
              <option value="Payment Gateway Placeholder">Payment Gateway Mandate (₹399/month)</option>
              <option value="UPI UTR Reference">UPI Reference / UTR</option>
            </select>
          </div>
          <div>
            <label class="field-label" for="bill-ref">${esc(s.paymentRefLabel)}</label>
            <input id="bill-ref" class="input" placeholder="${esc(s.paymentRefPlaceholder)}" required minlength="4" />
          </div>
        </div>
        <label style="display:flex;align-items:flex-start;gap:10px;font-size:0.86rem;cursor:pointer;">
          <input type="checkbox" id="bill-consent" style="margin-top:4px;" />
          <span>${esc(s.consentText)}</span>
        </label>
        <div style="display:flex;flex-wrap:wrap;gap:10px;">
          <button type="submit" class="btn btn-primary">${esc(s.submitBillingBtn)}</button>
          ${sub.status === 'pending' || active ? `<button type="button" class="btn btn-danger" id="btn-cancel-membership">${esc(s.cancelSubBtn)}</button>` : ''}
        </div>
      </form>
    </div>

    <div class="card">
      <h3 style="margin-bottom:10px;">${esc(s.billingHistoryTitle)} (${sub.billingHistory.length})</h3>
      ${sub.billingHistory.length === 0 ? `<p class="muted">${esc(s.noBillingRecords)}</p>` : `
        <div style="display:grid;gap:8px;">
          ${sub.billingHistory.map((b) => `
            <div style="border:1px solid var(--border);border-radius:12px;padding:10px 12px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
              <div>
                <strong>${esc(b.plan)} • ${esc(b.status.toUpperCase())}</strong>
                <div class="mono muted" style="font-size:0.78rem;">${esc(b.paymentMethod)} • ${esc(b.referenceId)} • ${esc(b.createdAt)}</div>
              </div>
            </div>
          `).join('')}
        </div>
      `}
    </div>
  `;
}

function renderSettingsView() {
  const s = t(state.language);
  const cfg = state.aiConfig;
  const activeProv = AI_PROVIDERS.find((p) => p.id === cfg.provider) || AI_PROVIDERS[0];

  return `
    <h2>${esc(s.settingsTitle)}</h2>

    <div class="grid-2">
      <form id="profile-form" class="card" style="display:grid;gap:12px;">
        <h3>${esc(s.profileSection)}</h3>
        <div>
          <label class="field-label" for="prof-name">${esc(s.nameLabel)}</label>
          <input id="prof-name" class="input" value="${esc(state.profile.name)}" required />
        </div>
        <div>
          <label class="field-label" for="prof-niche">${esc(s.nicheLabel)}</label>
          <input id="prof-niche" class="input" value="${esc(state.profile.niche)}" />
        </div>
        <button type="submit" class="btn btn-primary">${esc(s.saveProfileBtn)}</button>
      </form>

      <div class="card" style="display:grid;gap:14px;">
        <h3>${esc(s.appearanceSection)}</h3>
        <div>
          <div class="field-label">${esc(s.themeLabel)}</div>
          <div class="chips-row">
            ${['dark', 'light', 'system'].map((m) => `
              <button type="button" class="chip ${state.theme === m ? 'active' : ''}" data-set-theme="${m}">${esc(s.themeOptions[m])}</button>
            `).join('')}
          </div>
        </div>
        <div>
          <div class="field-label">${esc(s.uiLangLabel)}</div>
          <div class="chips-row">
            <button type="button" class="chip ${state.language === 'en' ? 'active' : ''}" data-set-lang="en">English</button>
            <button type="button" class="chip ${state.language === 'hi' ? 'active' : ''}" data-set-lang="hi">हिन्दी (Hindi)</button>
          </div>
        </div>
      </div>
    </div>

    <form id="ai-config-form" class="card" style="display:grid;gap:12px;">
      <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
        <h3>${esc(s.aiSectionTitle)}</h3>
        <span class="badge ${cfg.mode === 'api' ? 'badge-success' : 'badge-warning'}">${esc(cfg.mode === 'api' ? s.apiModePill : s.demoModePill)}</span>
      </div>
      <p class="muted" style="font-size:0.86rem;">${esc(s.aiSecurityNotice)}</p>
      <div class="grid-2">
        <div>
          <label class="field-label" for="ai-mode">${esc(s.aiModeLabel)}</label>
          <select id="ai-mode" class="select">
            <option value="demo" ${cfg.mode === 'demo' ? 'selected' : ''}>${esc(s.aiModeDemo)}</option>
            <option value="api" ${cfg.mode === 'api' ? 'selected' : ''}>${esc(s.aiModeApi)}</option>
          </select>
        </div>
        <div>
          <label class="field-label" for="ai-provider">${esc(s.aiProviderLabel)}</label>
          <select id="ai-provider" class="select">
            ${AI_PROVIDERS.map((p) => `<option value="${p.id}" ${cfg.provider === p.id ? 'selected' : ''}>${esc(p.label)}</option>`).join('')}
          </select>
        </div>
      </div>
      <div class="grid-2">
        <div>
          <label class="field-label" for="ai-model">${esc(s.aiModelLabel)}</label>
          <select id="ai-model" class="select">
            ${activeProv.models.map((m) => `<option value="${m}" ${cfg.model === m ? 'selected' : ''}>${esc(m)}</option>`).join('')}
          </select>
        </div>
        <div>
          <label class="field-label" for="ai-endpoint">${esc(s.aiEndpointLabel)}</label>
          <input id="ai-endpoint" class="input" type="url" value="${esc(cfg.endpointUrl)}" placeholder="${esc(s.aiEndpointPlaceholder)}" />
        </div>
      </div>
      <div>
        <label class="field-label" for="ai-session-token">${esc(s.aiSessionKeyLabel)}</label>
        <input id="ai-session-token" class="input" type="password" value="${esc(getRuntimeSessionToken())}" placeholder="${esc(s.aiSessionKeyPlaceholder)}" autocomplete="off" />
      </div>
      <div style="display:flex;flex-wrap:wrap;gap:10px;align-items:center;">
        <button type="submit" class="btn btn-primary">${esc(s.saveAiBtn)}</button>
        <button type="button" class="btn btn-outline" id="btn-test-ai">${esc(s.testAiBtn)}</button>
        <button type="button" class="btn btn-danger" id="btn-reset-all" style="margin-left:auto;">${esc(s.resetDataBtn)}</button>
      </div>
      <div id="ai-test-status" class="mono muted" style="font-size:0.84rem;"></div>
    </form>
  `;
}

function renderToolModal() {
  if (!activeTool) return '';
  const s = t(state.language);
  const tool = activeTool;
  const cat = CATEGORIES.find((c) => c.id === tool.cat);
  const isFav = state.favorites.includes(tool.id);
  const isApi = state.aiConfig.mode === 'api' && state.aiConfig.endpointUrl;

  return `
    <div class="modal-backdrop" id="tool-backdrop" role="dialog" aria-modal="true" aria-label="${esc(toolTitle(tool))}">
      <div class="modal-card">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;margin-bottom:12px;">
          <div>
            <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
              <span class="badge" style="background:${cat.color}20;color:${cat.color};">${cat.icon} ${esc(catTitle(cat))}</span>
              <span class="badge ${tool.tier === 'pro' ? 'badge-accent' : 'badge-success'}">${esc(tool.tier === 'pro' ? s.proTag : s.freeTag)}</span>
            </div>
            <h2 style="margin-top:8px;">${esc(toolTitle(tool))}</h2>
            <p class="muted" style="font-size:0.86rem;">${esc(toolDesc(tool))}</p>
          </div>
          <div style="display:flex;gap:8px;align-items:center;">
            <button type="button" class="btn btn-outline btn-sm" id="modal-fav-btn">${esc(isFav ? s.favBtnAdded : s.favBtnAdd)}</button>
            <button type="button" class="btn btn-outline btn-sm" id="btn-close-modal" aria-label="${esc(s.closeAria)}">✕</button>
          </div>
        </div>

        <div class="card" style="background:var(--surface-soft);padding:10px 14px;margin-bottom:14px;font-size:0.82rem;box-shadow:none;">
          ${esc(isApi ? s.modeBannerApi : s.modeBannerDemo)}
          ${tool.tier === 'pro' && !hasActivePremium(state) ? `
            <div style="margin-top:6px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px;">
              <span style="color:var(--accent);font-weight:600;">${esc(s.proNotice)}</span>
              <button type="button" class="btn btn-primary btn-sm" id="btn-modal-upgrade">${esc(s.upgradeBtn)}</button>
            </div>` : ''}
        </div>

        <div style="display:grid;gap:12px;">
          <div>
            <label class="field-label" for="ws-topic">${esc(s.topicLabel)}</label>
            <textarea id="ws-topic" class="textarea" style="min-height:92px;" placeholder="${esc(s.topicPlaceholder)}">${esc(modalDraft.topic)}</textarea>
          </div>
          <div>
            <label class="field-label" for="ws-context">${esc(s.contextLabel)}</label>
            <input id="ws-context" class="input" value="${esc(modalDraft.context)}" placeholder="${esc(s.contextPlaceholder)}" />
          </div>
          <div class="grid-3">
            <div>
              <label class="field-label" for="ws-tone">${esc(s.toneLabel)}</label>
              <select id="ws-tone" class="select">
                ${Object.entries(s.tones).map(([k, lbl]) => `<option value="${k}" ${modalDraft.tone === k ? 'selected' : ''}>${esc(lbl)}</option>`).join('')}
              </select>
            </div>
            <div>
              <label class="field-label" for="ws-out-lang">${esc(s.languageLabel)}</label>
              <select id="ws-out-lang" class="select">
                ${Object.entries(s.outLanguages).map(([k, lbl]) => `<option value="${k}" ${modalDraft.outLang === k ? 'selected' : ''}>${esc(lbl)}</option>`).join('')}
              </select>
            </div>
            <div>
              <label class="field-label" for="ws-length">${esc(s.lengthLabel)}</label>
              <select id="ws-length" class="select">
                ${Object.entries(s.lengths).map(([k, lbl]) => `<option value="${k}" ${modalDraft.length === k ? 'selected' : ''}>${esc(lbl)}</option>`).join('')}
              </select>
            </div>
          </div>

          <div style="display:flex;gap:10px;flex-wrap:wrap;">
            <button type="button" class="btn btn-primary" style="flex:1;" id="btn-generate" ${isGenerating ? 'disabled' : ''}>
              ${isGenerating ? `<span class="spinner"></span> ${esc(s.generatingText)}` : `✨ ${esc(s.generateBtn)}`}
            </button>
            <button type="button" class="btn btn-outline" id="btn-ws-clear">${esc(s.clearResultBtn)}</button>
          </div>

          <div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
              <label class="field-label" for="ws-result" style="margin:0;">${esc(s.resultLabel)}</label>
              ${modalDraft.sourceNote ? `<span class="badge badge-primary">${esc(modalDraft.sourceNote)}</span>` : ''}
            </div>
            <textarea id="ws-result" class="result-box" placeholder="${esc(s.resultPlaceholder)}">${esc(modalDraft.output)}</textarea>
          </div>

          <div style="display:flex;flex-wrap:wrap;gap:8px;">
            <button type="button" class="btn btn-soft btn-sm" id="btn-ws-copy">📋 ${esc(s.copyBtn)}</button>
            <button type="button" class="btn btn-soft btn-sm" id="btn-ws-dl">⬇ ${esc(s.downloadTxtBtn)}</button>
            <button type="button" class="btn btn-soft btn-sm" id="btn-ws-save-history">🕒 ${esc(s.saveHistoryBtn)}</button>
            <button type="button" class="btn btn-soft btn-sm" id="btn-ws-save-draft">📝 ${esc(s.saveDraftBtn)}</button>
          </div>
        </div>
      </div>
    </div>
  `;
}

function renderMainContent() {
  switch (currentView) {
    case 'all': return renderDashboardOrTools(false);
    case 'workspace': return renderWorkspaceView();
    case 'favorites': return renderFavoritesView();
    case 'history': return renderHistoryView();
    case 'membership': return renderMembershipView();
    case 'settings': return renderSettingsView();
    default: return renderDashboardOrTools(true);
  }
}

function render() {
  const s = t(state.language);
  const isApi = state.aiConfig.mode === 'api' && state.aiConfig.endpointUrl;
  root.innerHTML = `
    <div class="app-shell">
      ${renderSidebar()}
      <div class="main-area">
        <header class="topbar">
          <div style="display:flex;align-items:center;gap:10px;">
            <button type="button" class="btn btn-outline btn-sm mobile-only" id="btn-drawer" aria-label="${esc(s.menuAria)}">☰</button>
            <strong style="font-family:var(--font-display);">${esc(s.appName)}</strong>
            <span class="badge ${isApi ? 'badge-success' : 'badge-warning'}" style="display:inline-flex;">${esc(isApi ? s.apiModePill : s.demoModePill)}</span>
          </div>
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;justify-content:flex-end;">
            ${renderLangSwitchHtml()}
            <button type="button" class="btn btn-outline btn-sm" id="btn-theme-toggle" aria-label="${esc(s.themeToggleAria)}">${state.theme === 'dark' ? '☀' : '☾'}</button>
            <button type="button" class="btn btn-primary btn-sm" data-nav="membership">${esc(s.topbarUpgradeBtn)}</button>
          </div>
        </header>
        <main class="workspace-body">${renderMainContent()}</main>
      </div>
    </div>

    <nav class="bottom-nav" aria-label="Mobile Navigation">
      <button type="button" class="${currentView === 'dashboard' ? 'active' : ''}" data-nav="dashboard"><span>🏠</span><span>${esc(s.bottomHome)}</span></button>
      <button type="button" class="${currentView === 'all' ? 'active' : ''}" data-nav="all"><span>🧰</span><span>${esc(s.bottomTools)}</span></button>
      <button type="button" class="${currentView === 'workspace' ? 'active' : ''}" data-nav="workspace"><span>📝</span><span>${esc(s.bottomWorkspace)}</span></button>
      <button type="button" class="${currentView === 'history' ? 'active' : ''}" data-nav="history"><span>🕒</span><span>${esc(s.bottomHistory)}</span></button>
      <button type="button" class="${currentView === 'membership' ? 'active' : ''}" data-nav="membership"><span>💎</span><span>${esc(s.bottomPlan)}</span></button>
    </nav>

    ${renderToolModal()}
  `;
  bindEvents();
}

function syncModalDraftFromDom() {
  const topicEl = document.getElementById('ws-topic');
  if (!topicEl) return;
  modalDraft.topic = topicEl.value;
  modalDraft.context = document.getElementById('ws-context')?.value || '';
  modalDraft.tone = document.getElementById('ws-tone')?.value || 'professional';
  modalDraft.outLang = document.getElementById('ws-out-lang')?.value || 'en';
  modalDraft.length = document.getElementById('ws-length')?.value || 'medium';
  modalDraft.output = document.getElementById('ws-result')?.value || '';
}

function bindEvents() {
  const s = t(state.language);

  document.getElementById('btn-drawer')?.addEventListener('click', () => {
    mobileDrawerOpen = !mobileDrawerOpen;
    render();
  });
  document.getElementById('drawer-scrim')?.addEventListener('click', () => {
    mobileDrawerOpen = false;
    render();
  });

  document.getElementById('btn-theme-toggle')?.addEventListener('click', () => {
    syncModalDraftFromDom();
    state.theme = state.theme === 'dark' ? 'light' : 'dark';
    persistAndRender();
  });

  document.querySelectorAll('[data-set-lang]').forEach((btn) => {
    btn.addEventListener('click', () => {
      syncModalDraftFromDom();
      state.language = btn.dataset.setLang === 'hi' ? 'hi' : 'en';
      persistAndRender();
    });
  });

  document.querySelectorAll('[data-set-theme]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.theme = btn.dataset.setTheme;
      persistAndRender();
    });
  });

  document.querySelectorAll('[data-nav]').forEach((btn) => {
    btn.addEventListener('click', () => {
      currentView = btn.dataset.nav;
      if (currentView === 'all') activeCatFilter = 'all';
      mobileDrawerOpen = false;
      render();
    });
  });

  document.querySelectorAll('[data-cat-nav]').forEach((btn) => {
    btn.addEventListener('click', () => {
      currentView = 'all';
      activeCatFilter = btn.dataset.catNav;
      mobileDrawerOpen = false;
      render();
    });
  });

  document.getElementById('btn-hero-all')?.addEventListener('click', () => {
    currentView = 'all';
    activeCatFilter = 'all';
    render();
  });

  document.querySelectorAll('[data-filter-cat]').forEach((btn) => {
    btn.addEventListener('click', () => {
      activeCatFilter = btn.dataset.filterCat;
      render();
    });
  });

  const searchInput = document.getElementById('tool-search');
  searchInput?.addEventListener('input', (e) => {
    searchQuery = e.target.value;
    render();
    const restored = document.getElementById('tool-search');
    if (restored) {
      restored.focus();
      restored.setSelectionRange(searchQuery.length, searchQuery.length);
    }
  });
  document.getElementById('btn-clear-search')?.addEventListener('click', () => {
    searchQuery = '';
    render();
  });

  document.querySelectorAll('[data-fav-tool]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = btn.dataset.favTool;
      if (state.favorites.includes(id)) {
        state.favorites = state.favorites.filter((x) => x !== id);
        showToast(s.toastFavRemoved);
      } else {
        state.favorites = [id, ...state.favorites];
        showToast(s.toastFavAdded);
      }
      persistAndRender();
    });
  });

  document.querySelectorAll('[data-open-tool]').forEach((card) => {
    card.addEventListener('click', () => {
      const tool = getToolById(card.dataset.openTool);
      if (tool) openToolModal(tool);
    });
  });

  // History actions
  document.getElementById('btn-clear-history')?.addEventListener('click', () => {
    state.history = [];
    persistAndRender();
  });
  document.querySelectorAll('[data-restore-history]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = state.history.find((h) => h.id === btn.dataset.restoreHistory);
      if (!item) return;
      const tool = getToolById(item.toolId) || TOOLS[0];
      openToolModal(tool, item.topic, item.output);
    });
  });
  document.querySelectorAll('[data-copy-history]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = state.history.find((h) => h.id === btn.dataset.copyHistory);
      if (!item) return;
      navigator.clipboard?.writeText(item.output);
      showToast(s.toastCopied);
    });
  });
  document.querySelectorAll('[data-dl-history]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = state.history.find((h) => h.id === btn.dataset.dlHistory);
      if (item) downloadFile(item.output, `${item.toolId}.txt`);
    });
  });
  document.querySelectorAll('[data-md-history]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const item = state.history.find((h) => h.id === btn.dataset.mdHistory);
      if (item) downloadFile(`# ${item.toolTitle}\n\n${item.output}`, `${item.toolId}.md`, 'text/markdown;charset=utf-8');
    });
  });

  // Drafts / Workspace actions
  const createBlankDraft = () => {
    state.drafts = [{
      id: `dr_${Date.now()}`,
      toolTitle: 'Creator Draft',
      title: 'Untitled Creator Draft',
      content: '',
      updatedAt: new Date().toLocaleString()
    }, ...state.drafts];
    persistAndRender();
  };
  document.getElementById('btn-new-draft')?.addEventListener('click', createBlankDraft);
  document.getElementById('btn-new-draft-empty')?.addEventListener('click', createBlankDraft);
  document.getElementById('btn-export-json')?.addEventListener('click', () => {
    downloadFile(JSON.stringify({ drafts: state.drafts, history: state.history }, null, 2), 'ai-creator-toolkit-export.json', 'application/json');
  });
  document.getElementById('btn-export-md')?.addEventListener('click', () => {
    const md = state.drafts.map((d) => `# ${d.title}\n\n${d.content}`).join('\n\n---\n\n');
    downloadFile(md || '# No Drafts', 'ai-creator-drafts.md', 'text/markdown;charset=utf-8');
  });
  document.querySelectorAll('[data-save-draft]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.saveDraft;
      const title = document.querySelector(`[data-draft-title="${id}"]`)?.value || 'Draft';
      const content = document.querySelector(`[data-draft-body="${id}"]`)?.value || '';
      state.drafts = state.drafts.map((d) => d.id === id ? { ...d, title, content, updatedAt: new Date().toLocaleString() } : d);
      persistAndRender();
      showToast(s.toastSavedDraft);
    });
  });
  document.querySelectorAll('[data-copy-draft]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const content = document.querySelector(`[data-draft-body="${btn.dataset.copyDraft}"]`)?.value || '';
      navigator.clipboard?.writeText(content);
      showToast(s.toastCopied);
    });
  });
  document.querySelectorAll('[data-dl-draft]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const id = btn.dataset.dlDraft;
      const title = document.querySelector(`[data-draft-title="${id}"]`)?.value || 'draft';
      const content = document.querySelector(`[data-draft-body="${id}"]`)?.value || '';
      downloadFile(content, `${title.toLowerCase().replace(/[^a-z0-9]+/g, '_')}.txt`);
    });
  });
  document.querySelectorAll('[data-del-draft]').forEach((btn) => {
    btn.addEventListener('click', () => {
      state.drafts = state.drafts.filter((d) => d.id !== btn.dataset.delDraft);
      persistAndRender();
    });
  });

  // Membership actions
  document.getElementById('btn-focus-billing')?.addEventListener('click', () => {
    document.getElementById('bill-ref')?.focus();
  });
  document.getElementById('membership-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    try {
      submitMembershipRequest(state, {
        paymentMethod: document.getElementById('bill-method').value,
        referenceId: document.getElementById('bill-ref').value,
        consentAccepted: document.getElementById('bill-consent').checked
      });
      persistAndRender();
      showToast(s.toastBillingSubmitted);
    } catch (err) {
      if (err.message === 'CONSENT_REQUIRED') showToast(s.errConsent);
      else if (err.message === 'INVALID_REF') showToast(s.errRef);
      else showToast(err.message);
    }
  });
  const handleCancel = () => {
    cancelMembershipPlan(state);
    persistAndRender();
    showToast(s.toastBillingCancelled);
  };
  document.getElementById('btn-cancel-membership')?.addEventListener('click', handleCancel);
  document.getElementById('btn-cancel-status')?.addEventListener('click', handleCancel);
  document.getElementById('btn-modal-upgrade')?.addEventListener('click', () => {
    activeTool = null;
    currentView = 'membership';
    render();
  });

  // Profile & AI Settings
  document.getElementById('profile-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    state.profile.name = document.getElementById('prof-name').value.trim() || 'Creator';
    state.profile.niche = document.getElementById('prof-niche').value.trim();
    persistAndRender();
    showToast(s.toastProfileSaved);
  });
  document.getElementById('ai-provider')?.addEventListener('change', (e) => {
    const prov = AI_PROVIDERS.find((p) => p.id === e.target.value);
    if (prov) {
      state.aiConfig.provider = prov.id;
      state.aiConfig.model = prov.models[0];
      persistAndRender();
    }
  });
  document.getElementById('ai-config-form')?.addEventListener('submit', (e) => {
    e.preventDefault();
    state.aiConfig = {
      mode: document.getElementById('ai-mode').value,
      provider: document.getElementById('ai-provider').value,
      model: document.getElementById('ai-model').value,
      endpointUrl: document.getElementById('ai-endpoint').value.trim()
    };
    setRuntimeSessionToken(document.getElementById('ai-session-token').value.trim());
    persistAndRender();
    showToast(s.toastAiSaved);
  });
  document.getElementById('btn-test-ai')?.addEventListener('click', async () => {
    const draftCfg = {
      mode: document.getElementById('ai-mode').value,
      provider: document.getElementById('ai-provider').value,
      model: document.getElementById('ai-model').value,
      endpointUrl: document.getElementById('ai-endpoint').value.trim()
    };
    setRuntimeSessionToken(document.getElementById('ai-session-token').value.trim());
    const out = document.getElementById('ai-test-status');
    out.textContent = '...';
    const res = await testAiConnection(draftCfg);
    out.style.color = res.ok ? 'var(--success)' : 'var(--danger)';
    out.textContent = res.message;
  });
  document.getElementById('btn-reset-all')?.addEventListener('click', () => {
    state = resetAllLocalState();
    persistAndRender();
    showToast(s.toastResetDone);
  });

  // Active Tool Modal actions
  document.getElementById('btn-close-modal')?.addEventListener('click', () => {
    activeTool = null;
    isGenerating = false;
    render();
  });
  document.getElementById('modal-fav-btn')?.addEventListener('click', () => {
    if (!activeTool) return;
    syncModalDraftFromDom();
    if (state.favorites.includes(activeTool.id)) {
      state.favorites = state.favorites.filter((x) => x !== activeTool.id);
      showToast(s.toastFavRemoved);
    } else {
      state.favorites = [activeTool.id, ...state.favorites];
      showToast(s.toastFavAdded);
    }
    persistAndRender();
  });
  document.getElementById('btn-ws-clear')?.addEventListener('click', () => {
    modalDraft.topic = '';
    modalDraft.output = '';
    modalDraft.sourceNote = '';
    render();
    showToast(s.toastCleared);
  });
  document.getElementById('btn-ws-copy')?.addEventListener('click', () => {
    const txt = document.getElementById('ws-result')?.value || '';
    if (!txt) return;
    navigator.clipboard?.writeText(txt);
    showToast(s.toastCopied);
  });
  document.getElementById('btn-ws-dl')?.addEventListener('click', () => {
    const txt = document.getElementById('ws-result')?.value || '';
    if (!txt || !activeTool) return;
    downloadFile(txt, `${activeTool.id}.txt`);
  });
  document.getElementById('btn-ws-save-history')?.addEventListener('click', () => {
    if (!activeTool) return;
    syncModalDraftFromDom();
    if (!modalDraft.output.trim()) return;
    state.history = [{
      id: `hist_${Date.now()}`,
      toolId: activeTool.id,
      toolTitle: toolTitle(activeTool),
      topic: modalDraft.topic,
      output: modalDraft.output,
      source: state.aiConfig.mode === 'api' && state.aiConfig.endpointUrl ? 'api' : 'demo',
      createdAt: new Date().toLocaleTimeString()
    }, ...state.history].slice(0, 60);
    persistAndRender();
    showToast(s.toastSavedHistory);
  });
  document.getElementById('btn-ws-save-draft')?.addEventListener('click', () => {
    if (!activeTool) return;
    syncModalDraftFromDom();
    if (!modalDraft.output.trim()) return;
    state.drafts = [{
      id: `dr_${Date.now()}`,
      toolTitle: toolTitle(activeTool),
      title: modalDraft.topic ? `${toolTitle(activeTool)} — ${modalDraft.topic.slice(0, 42)}` : toolTitle(activeTool),
      content: modalDraft.output,
      updatedAt: new Date().toLocaleString()
    }, ...state.drafts];
    persistAndRender();
    showToast(s.toastSavedDraft);
  });
  document.getElementById('btn-generate')?.addEventListener('click', async () => {
    if (!activeTool) return;
    syncModalDraftFromDom();
    if (!modalDraft.topic.trim()) {
      showToast(s.errEmptyTopic);
      document.getElementById('ws-topic')?.focus();
      return;
    }
    isGenerating = true;
    render();
    try {
      const res = await generateCreatorContent(activeTool, {
        topic: modalDraft.topic,
        context: modalDraft.context,
        tone: modalDraft.tone,
        language: modalDraft.outLang,
        length: modalDraft.length
      }, state.aiConfig);
      modalDraft.output = res.text;
      modalDraft.sourceNote = res.modelUsed;
      state.usage.totalGenerations += 1;
      state.usage.todayCount += 1;
      state.recentTools = [activeTool.id, ...state.recentTools.filter((id) => id !== activeTool.id)].slice(0, 12);
      state.history = [{
        id: `hist_${Date.now()}`,
        toolId: activeTool.id,
        toolTitle: toolTitle(activeTool),
        topic: modalDraft.topic,
        output: res.text,
        source: res.source,
        createdAt: new Date().toLocaleTimeString()
      }, ...state.history].slice(0, 60);
      saveState(state);
    } catch (err) {
      showToast(err.message);
    } finally {
      isGenerating = false;
      render();
    }
  });
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./service-worker.js').catch(() => {});
  });
}

applyThemeAndLang();
render();
