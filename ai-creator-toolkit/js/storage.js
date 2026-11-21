const STORAGE_KEY = 'ai_creator_toolkit_state_v1';
const SESSION_KEY_STORE = 'ai_creator_runtime_token';

export const PREMIUM_PLAN = {
  id: 'ai_creator_premium',
  name: 'AI Creator Premium',
  priceInr: 399,
  currency: 'INR',
  billing: 'Monthly subscription',
  duration: '1 month'
};

const DEFAULT_STATE = {
  profile: {
    name: 'Creator',
    niche: 'Digital Creator'
  },
  language: 'en',
  theme: 'dark',
  favorites: ['ai_blog_writer', 'instagram_caption', 'reel_script', 'image_prompt'],
  recentTools: [],
  history: [],
  drafts: [],
  usage: {
    totalGenerations: 0,
    todayDate: new Date().toISOString().slice(0, 10),
    todayCount: 0
  },
  aiConfig: {
    mode: 'demo', // demo | api
    provider: 'custom_proxy',
    model: 'gemini-2.5-flash',
    endpointUrl: ''
  },
  subscription: {
    planName: 'Free',
    priceInr: 399,
    currency: 'INR',
    duration: '1 month',
    status: 'free', // free | pending | active | cancelled
    autoRenewConsent: false,
    renewalDateIso: null,
    serverVerifiedToken: null,
    billingHistory: []
  }
};

export function loadState() {
  const today = new Date().toISOString().slice(0, 10);
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return structuredClone(DEFAULT_STATE);
    const parsed = JSON.parse(raw);
    const sub = parsed.subscription || {};
    const usage = parsed.usage || {};
    return {
      ...structuredClone(DEFAULT_STATE),
      ...parsed,
      profile: { ...DEFAULT_STATE.profile, ...(parsed.profile || {}) },
      aiConfig: { ...DEFAULT_STATE.aiConfig, ...(parsed.aiConfig || {}) },
      usage: {
        totalGenerations: Number(usage.totalGenerations || 0),
        todayDate: today,
        todayCount: usage.todayDate === today ? Number(usage.todayCount || 0) : 0
      },
      subscription: {
        ...DEFAULT_STATE.subscription,
        ...sub,
        priceInr: PREMIUM_PLAN.priceInr,
        currency: PREMIUM_PLAN.currency,
        duration: PREMIUM_PLAN.duration,
        // Security invariant: never allow unverified frontend active status
        status: sub.status === 'active' && !sub.serverVerifiedToken ? 'pending' : (sub.status || 'free')
      }
    };
  } catch {
    return structuredClone(DEFAULT_STATE);
  }
}

export function saveState(state) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Ignore storage quota issues cleanly
  }
}

export function getRuntimeSessionToken() {
  try {
    return sessionStorage.getItem(SESSION_KEY_STORE) || '';
  } catch {
    return '';
  }
}

export function setRuntimeSessionToken(token) {
  try {
    if (token) sessionStorage.setItem(SESSION_KEY_STORE, token);
    else sessionStorage.removeItem(SESSION_KEY_STORE);
  } catch {
    // Ignore sessionStorage restrictions
  }
}

export function resetAllLocalState() {
  try {
    localStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(SESSION_KEY_STORE);
  } catch {
    // Ignore
  }
  return structuredClone(DEFAULT_STATE);
}

export function hasActivePremium(state) {
  const sub = state.subscription;
  return Boolean(sub && sub.status === 'active' && sub.serverVerifiedToken);
}

export function submitMembershipRequest(state, { paymentMethod, referenceId, consentAccepted }) {
  if (!consentAccepted) {
    throw new Error('CONSENT_REQUIRED');
  }
  const cleanRef = String(referenceId || '').trim();
  if (cleanRef.length < 4) {
    throw new Error('INVALID_REF');
  }
  const renewalDate = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
  const rec = {
    id: `bill_${Date.now()}`,
    plan: `${PREMIUM_PLAN.name} (₹${PREMIUM_PLAN.priceInr} ${PREMIUM_PLAN.currency} / ${PREMIUM_PLAN.duration})`,
    amountInr: PREMIUM_PLAN.priceInr,
    currency: PREMIUM_PLAN.currency,
    duration: PREMIUM_PLAN.duration,
    paymentMethod,
    referenceId: cleanRef,
    status: 'pending',
    createdAt: new Date().toLocaleString()
  };
  state.subscription.planName = PREMIUM_PLAN.name;
  state.subscription.priceInr = PREMIUM_PLAN.priceInr;
  state.subscription.currency = PREMIUM_PLAN.currency;
  state.subscription.duration = PREMIUM_PLAN.duration;
  state.subscription.status = 'pending';
  state.subscription.autoRenewConsent = true;
  state.subscription.renewalDateIso = renewalDate.toISOString();
  state.subscription.billingHistory = [rec, ...state.subscription.billingHistory];
  return rec;
}

export function cancelMembershipPlan(state) {
  state.subscription.planName = 'Free';
  state.subscription.status = 'cancelled';
  state.subscription.autoRenewConsent = false;
  state.subscription.renewalDateIso = null;
  state.subscription.serverVerifiedToken = null;
  state.subscription.billingHistory = state.subscription.billingHistory.map((item) =>
    item.status === 'pending' || item.status === 'active'
      ? { ...item, status: 'cancelled' }
      : item
  );
}
