// Bharat Tools ₹399/month Recurring Subscription & Payment Architecture
// Security invariant: Never activate paid access from an unverified client response.

export const SUBSCRIPTION_CONFIG = {
  priceInr: 399,
  currency: 'INR',
  billingCycle: 'monthly_recurring',
  refundWindowDays: 7,
  verificationEndpoint: '' // Configure your backend verification endpoint e.g. https://api.bharattools.in/v1/subscriptions/verify
};

export async function submitSubscriptionVerification(state, { paymentMethod, referenceId, consentAccepted, notes }) {
  if (!consentAccepted) {
    throw new Error('Explicit user consent is required for ₹399/month recurring billing.');
  }
  const cleanRef = (referenceId || '').trim();
  if (cleanRef.length < 4) {
    throw new Error('Enter a valid UPI Autopay Mandate ID or Gateway Reference ID (min 4 chars).');
  }

  const now = new Date();
  const end = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
  const fmt = (d) => d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  const record = {
    id: `sub_${Date.now()}`,
    amountInr: SUBSCRIPTION_CONFIG.priceInr,
    billingCycle: SUBSCRIPTION_CONFIG.billingCycle,
    paymentMethod,
    referenceId: cleanRef,
    status: 'pending',
    userConsentAccepted: true,
    receiptNumber: '',
    periodLabel: `${fmt(now)} – ${fmt(end)}`,
    notes: (notes || '').trim(),
    createdAtIso: now.toISOString()
  };

  // Optional live backend verification call when endpoint is configured
  if (SUBSCRIPTION_CONFIG.verificationEndpoint) {
    const resp = await fetch(SUBSCRIPTION_CONFIG.verificationEndpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(record)
    });
    if (!resp.ok) {
      record.status = 'failed';
      state.subscription.billingHistory = [record, ...state.subscription.billingHistory];
      throw new Error('Payment verification gateway rejected or could not confirm the mandate.');
    }
    const verified = await resp.json();
    if (verified?.verified === true && verified?.token) {
      record.status = 'active';
      record.receiptNumber = verified.receiptNumber || `BT-SUB-${Date.now().toString().slice(-6)}`;
      state.subscription.status = 'active';
      state.subscription.autoRenewConsent = true;
      state.subscription.periodEndIso = verified.periodEndIso || end.toISOString();
      state.subscription.serverVerifiedToken = verified.token;
    }
  } else {
    // Static GitHub Pages hosting: keep strictly PENDING until verified by server/webhook
    state.subscription.status = 'pending';
    state.subscription.autoRenewConsent = true;
  }

  state.subscription.billingHistory = [record, ...state.subscription.billingHistory];
  return record;
}

export function cancelSubscription(state) {
  state.subscription.status = 'cancelled';
  state.subscription.autoRenewConsent = false;
  state.subscription.serverVerifiedToken = null;
  state.subscription.billingHistory = state.subscription.billingHistory.map((r) =>
    r.status === 'active' || r.status === 'pending'
      ? { ...r, status: 'cancelled', notes: 'Cancelled by user via Membership Dashboard' }
      : r
  );
}

export function requestRefund(state, purchaseId) {
  state.subscription.billingHistory = state.subscription.billingHistory.map((r) =>
    r.id === purchaseId ? { ...r, status: 'refund_requested' } : r
  );
}
