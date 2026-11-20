const STORAGE_KEY = 'bharat_tools_web_state_v1';

const DEFAULT_STATE = {
  user: null,
  language: 'en',
  theme: 'system',
  favorites: ['pdf_merge', 'qr_upi', 'util_gst'],
  recentTools: [],
  recentSearches: [],
  history: [],
  subscription: {
    status: 'free', // free | pending | active | expired | cancelled
    autoRenewConsent: false,
    periodEndIso: null,
    billingHistory: [],
    verificationEndpoint: ''
  }
};

export function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return structuredClone(DEFAULT_STATE);
    const parsed = JSON.parse(raw);
    return {
      ...structuredClone(DEFAULT_STATE),
      ...parsed,
      subscription: {
        ...DEFAULT_STATE.subscription,
        ...(parsed.subscription || {}),
        // Security invariant: never trust a locally tampered active status without a valid server signature token
        status:
          parsed?.subscription?.status === 'active' && !parsed?.subscription?.serverVerifiedToken
            ? 'pending'
            : (parsed?.subscription?.status || 'free')
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
    // Ignore storage quota errors cleanly
  }
}

export function hasActiveSubscription(state) {
  const sub = state.subscription;
  if (!sub || sub.status !== 'active' || !sub.serverVerifiedToken) return false;
  if (sub.periodEndIso && new Date(sub.periodEndIso).getTime() <= Date.now()) return false;
  return true;
}
