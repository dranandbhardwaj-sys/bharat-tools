const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";
const ADMIN_UID = "admin_uid_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validUserPayload(uid) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    displayName: "Aarav Sharma",
    email: "aarav@example.com",
    preferredLanguage: "en",
    themeMode: "system",
    referralCode: "BHARAT12",
    favorites: ["pdf_merge", "qr_upi"],
    recentTools: ["gst_calc"],
    subscriptionStatus: "free",
    autoRenewConsent: false,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(unauth.collection("purchases").where("userId", "==", ALICE_UID).get());
  await assertFails(unauth.collection("announcements").where("active", "==", true).get());
});

test("User profile: owner create, partial update, cross-user denial, and subscription bypass prevention", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "aarav@example.com", email_verified: true }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Reject creating with active subscription directly (no client-side bypass)
  await assertFails(
    bob.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID),
      subscriptionStatus: "active",
    })
  );

  // Reject self-upgrading subscriptionStatus on update
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      subscriptionStatus: "active",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid partial update of preferences & favorites
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "hi",
      themeMode: "dark",
      favorites: ["pdf_merge"],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(bob.collection("users").doc(ALICE_UID).get());
  await assertFails(bob.collection("users").get());
});

test("Email spoofing attack without email_verified=true is rejected for admin access", async () => {
  const spoofedAdmin = testEnv.authenticatedContext("spoof_1", {
    email: "dranandbhardwaj@gmail.com",
    email_verified: false,
  }).firestore();

  await assertFails(spoofedAdmin.collection("users").get());
  await assertFails(
    spoofedAdmin.collection("announcements").doc("ann_1").set({
      title: "Fake",
      titleHi: "",
      body: "Spoofed",
      bodyHi: "",
      active: true,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Verified admin verifies ₹399/month subscription; user can cancel or request refund", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "aarav@example.com", email_verified: true }).firestore();
  const admin = testEnv.authenticatedContext(ADMIN_UID, {
    email: "dranandbhardwaj@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(alice.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Reject subscription submission without userConsentAccepted == true
  await assertFails(
    alice.collection("purchases").doc("sub_bad").set({
      userId: ALICE_UID,
      userEmail: "aarav@example.com",
      amountInr: 399,
      billingCycle: "monthly_recurring",
      paymentMethod: "upi_autopay",
      referenceId: "UMND4123456789",
      status: "pending",
      userConsentAccepted: false,
      receiptNumber: "",
      periodLabel: "Nov 2026 - Dec 2026",
      notes: "Monthly subscription",
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Valid pending monthly subscription request with explicit user consent
  await assertSucceeds(
    alice.collection("purchases").doc("sub_1").set({
      userId: ALICE_UID,
      userEmail: "aarav@example.com",
      amountInr: 399,
      billingCycle: "monthly_recurring",
      paymentMethod: "upi_autopay",
      referenceId: "UMND4123456789",
      status: "pending",
      userConsentAccepted: true,
      receiptNumber: "",
      periodLabel: "Monthly Cycle",
      notes: "UPI Autopay mandate",
      createdAt: new Date(Date.now() - 2000),
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Alice cannot self-activate her subscription
  await assertFails(
    alice.collection("purchases").doc("sub_1").update({
      status: "active",
      receiptNumber: "BT-SUB-001",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin verifies subscription active
  await assertSucceeds(
    admin.collection("purchases").doc("sub_1").update({
      status: "active",
      receiptNumber: "BT-SUB-2026-001",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    admin.collection("users").doc(ALICE_UID).update({
      subscriptionStatus: "active",
      autoRenewConsent: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can cancel her active subscription cleanly
  await assertSucceeds(
    alice.collection("purchases").doc("sub_1").update({
      status: "cancelled",
      notes: "User cancelled monthly renewal",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).update({
      subscriptionStatus: "cancelled",
      autoRenewConsent: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("User tool_history subcollection and support_tickets enforce strict ownership", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID, { email: "aarav@example.com", email_verified: true }).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("tool_history").doc("hist_1").set({
      userId: ALICE_UID,
      toolId: "qr_upi",
      title: "UPI QR Generated",
      summary: "upi://pay?pa=shop@upi",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(alice.collection("users").doc(ALICE_UID).collection("tool_history").get());
  await assertFails(bob.collection("users").doc(ALICE_UID).collection("tool_history").get());

  await assertSucceeds(
    alice.collection("support_tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      userEmail: "aarav@example.com",
      category: "billing",
      subject: "Subscription receipt question",
      message: "Where can I download my monthly receipt?",
      status: "open",
      adminReply: "",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(alice.collection("support_tickets").where("userId", "==", ALICE_UID).get());
  await assertFails(bob.collection("support_tickets").where("userId", "==", ALICE_UID).get());
});
