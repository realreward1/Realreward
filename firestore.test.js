const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const OWNER_UID = "owner_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(ms = 2000) {
  return new Date(Date.now() - ms);
}

function ownerDb() {
  return testEnv.authenticatedContext(OWNER_UID, {
    email: "wahidzroman@gmail.com",
    email_verified: true,
  }).firestore();
}

function spoofedOwnerDb() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: "wahidzroman@gmail.com",
    email_verified: false,
  }).firestore();
}

function validUserPayload(uid, mobile = "01712345678", deviceId = "device_abc_123") {
  const ts = pastDate();
  return {
    userId: uid,
    accountId: "RR-BD-1001",
    email: "alice@example.com",
    fullName: "Alice Rahman",
    username: "alice_bd",
    mobileNumber: mobile,
    deviceId: deviceId,
    bkashPersonal: mobile,
    nagadPersonal: mobile,
    vipTier: "REGULAR",
    banned: false,
    twoFactorEnabled: true,
    loginAlertsEnabled: true,
    registrationBonusClaimed: true,
    availableBalanceBdt: 10,
    pendingBalanceBdt: 0,
    totalEarningsBdt: 10,
    totalWithdrawalsBdt: 0,
    dailyEarningsBdt: 0,
    completedTasksCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("tasks").get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
});

test("User registration grants exact 10 BDT bonus, rejects inflated balance or spoofed banned/VIP", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Valid signup succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Inflated balance fails
  const cheated = validUserPayload(BOB_UID, "01812345678", "device_bob_1");
  cheated.availableBalanceBdt = 5000;
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(cheated));

  // Self-assigned DIAMOND VIP on signup fails
  const vipCheat = validUserPayload(BOB_UID, "01812345678", "device_bob_1");
  vipCheat.vipTier = "DIAMOND";
  await assertFails(bobDb.collection("users").doc(BOB_UID).set(vipCheat));
});

test("Single-account mobile_registry and device_registry reject duplicates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(aliceDb.collection("mobile_registry").doc("01712345678").set({
    userId: ALICE_UID,
    createdAt: ts,
  }));
  await assertSucceeds(aliceDb.collection("device_registry").doc("dev_001").set({
    userId: ALICE_UID,
    createdAt: ts,
  }));

  // Bob cannot overwrite or claim Alice's mobile or device
  await assertFails(bobDb.collection("mobile_registry").doc("01712345678").set({
    userId: BOB_UID,
    createdAt: ts,
  }));
  await assertFails(bobDb.collection("device_registry").doc("dev_001").set({
    userId: BOB_UID,
    createdAt: ts,
  }));
});

test("Cross-user PII read and regular user balance/VIP/ban tampering are blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Bob cannot read or list Alice's private user profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Alice cannot modify her own balance or VIP tier
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    availableBalanceBdt: 9999,
    updatedAt: pastDate(500),
  }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    vipTier: "DIAMOND",
    updatedAt: pastDate(500),
  }));

  // Alice CAN update safe profile fields
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    fullName: "Alice New Name",
    updatedAt: pastDate(500),
  }));
});

test("Spoofed unverified owner email cannot access admin operations", async () => {
  const spoof = spoofedOwnerDb();
  const ts = pastDate();

  await assertFails(spoof.collection("users").get());
  await assertFails(spoof.collection("tasks").doc("task_spoof").set({
    titleBn: "Spoof",
    titleEn: "Spoof",
    category: "SURVEY",
    rewardBdt: 100,
    descriptionBn: "Desc",
    descriptionEn: "Desc",
    instructionsBn: "Inst",
    instructionsEn: "Inst",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: true,
    active: true,
    createdBy: "spoof",
    createdAt: ts,
    updatedAt: ts,
  }));
});

test("Verified Owner Admin can create tasks, manage users, update payment config, and broadcast notices", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Admin lists all users
  await assertSucceeds(admin.collection("users").get());

  // Admin upgrades Alice VIP & bans/unbans
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    vipTier: "GOLD",
    banned: false,
    availableBalanceBdt: 500,
    updatedAt: pastDate(500),
  }));

  // Admin creates a task
  await assertSucceeds(admin.collection("tasks").doc("task_101").set({
    titleBn: "সার্ভে টাস্ক",
    titleEn: "Survey Task",
    category: "SURVEY",
    rewardBdt: 50,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Details",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Instructions",
    taskLink: "https://example.com",
    deadlineText: "30 Dec 2026",
    totalSlots: 100,
    availableSlots: 100,
    featured: true,
    active: true,
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Admin configures bKash/Nagad & VIP prices
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").set({
    bkashNumber: "01711112222",
    nagadNumber: "01811112222",
    vipBronzePriceBdt: 150,
    vipSilverPriceBdt: 350,
    vipGoldPriceBdt: 750,
    vipDiamondPriceBdt: 1500,
    latestAppVersion: "2.0.0",
    appDownloadUrl: "https://example.com/real-reward.apk",
    updateMessageBn: "নতুন আপডেট",
    updateMessageEn: "New update",
    updatedAt: ts,
  }));

  // Admin posts announcement
  await assertSucceeds(admin.collection("announcements").doc("ann_1").set({
    category: "ANNOUNCEMENT",
    titleBn: "নতুন ফিচার",
    titleEn: "New Feature",
    messageBn: "স্বাগতম",
    messageEn: "Welcome",
    appVersion: "2.0.0",
    updateUrl: "https://example.com",
    createdAt: ts,
  }));
});

test("Withdrawal enforces minimum 300 BDT + 10 BDT fee and scoped queries", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    availableBalanceBdt: 400,
    updatedAt: pastDate(500),
  }));

  // Under 300 BDT fails
  await assertFails(aliceDb.collection("withdrawals").doc("w_bad_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 250,
    feeBdt: 10,
    totalDeductedBdt: 260,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Missing 10 BDT fee fails
  await assertFails(aliceDb.collection("withdrawals").doc("w_bad_2").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 300,
    feeBdt: 0,
    totalDeductedBdt: 300,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Valid 300 + 10 = 310 withdrawal succeeds
  await assertSucceeds(aliceDb.collection("withdrawals").doc("w_ok_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 300,
    feeBdt: 10,
    totalDeductedBdt: 310,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Admin transitions withdrawal PENDING -> APPROVED -> COMPLETED
  await assertSucceeds(admin.collection("withdrawals").doc("w_ok_1").update({
    status: "APPROVED",
    adminNote: "Approved by owner",
    updatedAt: pastDate(300),
  }));
  await assertSucceeds(admin.collection("withdrawals").doc("w_ok_1").update({
    status: "COMPLETED",
    adminNote: "Payment Success",
    updatedAt: pastDate(100),
  }));

  // Scoped query succeeds, unfiltered query fails for regular user
  await assertSucceeds(aliceDb.collection("withdrawals").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("withdrawals").get());
});

test("IP/VPN auto-ban allows irreversible ban lock and blocks user self-unban", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Safe IP telemetry update succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    lastIpAddress: "103.10.20.30",
    lastCountryCode: "BD",
    vpnDetected: false,
    updatedAt: pastDate(400),
  }));

  // Auto-ban on VPN detection succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    banned: true,
    banReason: "VPN_DETECTED",
    lastIpAddress: "185.1.2.3",
    lastCountryCode: "DE",
    vpnDetected: true,
    updatedAt: pastDate(300),
  }));

  // User cannot unban themselves
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    banned: false,
    updatedAt: pastDate(200),
  }));

  // Owner Admin can unban
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    banned: false,
    banReason: null,
    updatedAt: pastDate(100),
  }));
});

test("Task proof one-per-task, VIP request, and support ticket rules enforce ownership and admin review", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  await assertSucceeds(admin.collection("tasks").doc("t1").set({
    titleBn: "টাস্ক ১",
    titleEn: "Task 1",
    category: "SURVEY",
    rewardBdt: 40,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Desc",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Inst",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: true,
    active: true,
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Valid proof docId = taskId_uid
  await assertSucceeds(aliceDb.collection("task_proofs").doc(`t1_${ALICE_UID}`).set({
    taskId: "t1",
    taskTitle: "Task 1",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 40,
    proofNote: "Completed survey ref #99",
    status: "PENDING_REVIEW",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Self-approval by user fails
  await assertFails(aliceDb.collection("task_proofs").doc(`t1_${ALICE_UID}`).update({
    status: "APPROVED",
    updatedAt: pastDate(500),
  }));

  // Admin approval succeeds
  await assertSucceeds(admin.collection("task_proofs").doc(`t1_${ALICE_UID}`).update({
    status: "APPROVED",
    adminNote: "Verified",
    updatedAt: pastDate(500),
  }));

  // VIP request & support ticket
  await assertSucceeds(aliceDb.collection("vip_requests").doc("vip_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    targetTier: "GOLD",
    method: "BKASH_PERSONAL",
    senderNumber: "01712345678",
    transactionId: "TRX998877",
    amountBdt: 750,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertFails(bobDb.collection("vip_requests").doc("vip_1").get());

  await assertSucceeds(aliceDb.collection("support_tickets").doc("sup_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    category: "TASK_ISSUE",
    subject: "Help needed",
    message: "Checking my submission status",
    status: "OPEN",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(admin.collection("support_tickets").doc("sup_1").update({
    status: "RESOLVED",
    adminReply: "Approved!",
    updatedAt: pastDate(500),
  }));
});

test("Referral enforces atomic 15 BDT credit, blocks standalone tampering, and logs scam/removed accounts", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Register Alice with referral fields
  const aliceDoc = validUserPayload(ALICE_UID, "01712345678", "device_alice_1");
  aliceDoc.referralCode = "RR10001";
  aliceDoc.referralCount = 0;
  aliceDoc.referralEarningsBdt = 0;
  aliceDoc.referralScamCount = 0;
  aliceDoc.permanentlyBanned = false;
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(aliceDoc));
  await assertSucceeds(aliceDb.collection("leaderboard_public").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 0,
    earningsBdt: 10,
    updatedAt: ts,
  }));
  await assertSucceeds(aliceDb.collection("referral_codes").doc("RR10001").set({
    code: "RR10001",
    userId: ALICE_UID,
    username: "alice_bd",
    deviceId: "device_alice_1",
    lastIpAddress: "103.1.2.3",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Bob CANNOT credit Alice +15 standalone without creating /referrals/BOB_UID
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({
    availableBalanceBdt: 25,
    totalEarningsBdt: 25,
    referralCount: 1,
    referralEarningsBdt: 15,
    updatedAt: pastDate(500),
  }));

  // Bob validly creates /referrals/BOB_UID + credits Alice +15 in an atomic batch
  const batch = bobDb.batch();
  batch.set(bobDb.collection("referrals").doc(BOB_UID), {
    referrerId: ALICE_UID,
    referrerUsername: "alice_bd",
    referredId: BOB_UID,
    referredUsername: "bob_bd",
    referredEmail: "bob@example.com",
    referralCode: "RR10001",
    referredIp: "103.44.55.66",
    referredDeviceId: "device_bob_1",
    bonusBdt: 15,
    status: "CREDITED",
    createdAt: ts,
  });
  batch.update(bobDb.collection("users").doc(ALICE_UID), {
    availableBalanceBdt: 25,
    totalEarningsBdt: 25,
    referralCount: 1,
    referralEarningsBdt: 15,
    updatedAt: pastDate(500),
  });
  batch.update(bobDb.collection("leaderboard_public").doc(ALICE_UID), {
    earningsBdt: 25,
    availableBalanceBdt: 25,
    updatedAt: pastDate(500),
  });
  batch.set(bobDb.collection("transactions").doc(`ref_${BOB_UID}`), {
    userId: ALICE_UID,
    titleBn: "রেফারেল বোনাস (+৳১৫)",
    titleEn: "Referral Bonus (+৳15)",
    type: "BONUS",
    amountBdt: 15,
    status: "COMPLETED",
    referenceId: `REF_${BOB_UID}`,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Removed account blocks re-registration in /users/{uid}
  await assertSucceeds(admin.collection("removed_accounts").doc("charlie_uid").set({
    userId: "charlie_uid",
    email: "charlie@example.com",
    reason: "PERMANENT_SCAM",
    removedAt: ts,
  }));
  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();
  await assertFails(charlieDb.collection("users").doc("charlie_uid").set(
    validUserPayload("charlie_uid", "01912345678", "device_charlie_1")
  ));

  // Admin saves maintenance stop timer & dashboard top notice in app_config
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").set({
    bkashNumber: "01711112222",
    nagadNumber: "01811112222",
    vipBronzePriceBdt: 150,
    vipSilverPriceBdt: 350,
    vipGoldPriceBdt: 750,
    vipDiamondPriceBdt: 1500,
    latestAppVersion: "2.1.0",
    appStopped: true,
    stopMessageBn: "আপডেটের কাজ চলছে",
    stopMessageEn: "System Maintenance",
    stopEndEpochMillis: Date.now() + 3600000,
    dashboardTopNoticeActive: true,
    dashboardTopNoticeBn: "জরুরি বিজ্ঞপ্তি",
    dashboardTopNoticeEn: "Important Notice",
    updatedAt: ts,
  }));
});

test("One-time permanent wallet setup locks bKash/Nagad, enforces 1-wallet-1-account, and supports 1-time admin reset", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin = ownerDb();
  const ts = pastDate(500);

  const aliceDoc = validUserPayload(ALICE_UID, "01712345678", "device_alice_w");
  aliceDoc.paymentAccountsLocked = false;
  aliceDoc.walletChangeApprovedOnce = false;
  aliceDoc.walletResetCount = 0;
  aliceDoc.securityPasswordHash = "hash_initial";
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(aliceDoc));

  // Profile name + password change succeeds
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).update({
    fullName: "Alice New Name",
    securityPasswordHash: "hash_updated",
    updatedAt: pastDate(450),
  }));

  // First-time permanent setup of bKash & Nagad atomically claims wallet_registry
  const setupBatch = aliceDb.batch();
  setupBatch.set(aliceDb.collection("wallet_registry").doc("01712345678"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  setupBatch.set(aliceDb.collection("wallet_registry").doc("01812345678"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  setupBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    bkashPersonal: "01712345678",
    nagadPersonal: "01812345678",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 0,
    updatedAt: pastDate(400),
  });
  await assertSucceeds(setupBatch.commit());

  // Second attempt by Alice to change locked bKash/Nagad fails!
  const secondBatch = aliceDb.batch();
  secondBatch.set(aliceDb.collection("wallet_registry").doc("01911111111"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  secondBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    bkashPersonal: "01911111111",
    nagadPersonal: "01611111111",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 0,
    updatedAt: pastDate(350),
  });
  await assertFails(secondBatch.commit());

  // Alice cannot self-unlock paymentAccountsLocked!
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({
    paymentAccountsLocked: false,
    walletChangeApprovedOnce: true,
    updatedAt: pastDate(320),
  }));

  // 1-Wallet-1-Account: Bob cannot claim Alice's bKash or Nagad number!
  const bobDoc = validUserPayload(BOB_UID, "01922223333", "device_bob_w");
  bobDoc.paymentAccountsLocked = false;
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(bobDoc));

  await assertFails(bobDb.collection("wallet_registry").doc("01712345678").set({
    userId: BOB_UID,
    createdAt: ts,
  }));
  const bobStealBatch = bobDb.batch();
  bobStealBatch.set(bobDb.collection("wallet_registry").doc("01712345678"), {
    userId: BOB_UID,
    createdAt: ts,
  });
  bobStealBatch.set(bobDb.collection("wallet_registry").doc("01622223333"), {
    userId: BOB_UID,
    createdAt: ts,
  });
  bobStealBatch.update(bobDb.collection("users").doc(BOB_UID), {
    bkashPersonal: "01712345678",
    nagadPersonal: "01622223333",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 0,
    updatedAt: pastDate(300),
  });
  await assertFails(bobStealBatch.commit());

  // Bob succeeds with his own unique numbers
  const bobOkBatch = bobDb.batch();
  bobOkBatch.set(bobDb.collection("wallet_registry").doc("01922223333"), {
    userId: BOB_UID,
    createdAt: ts,
  });
  bobOkBatch.set(bobDb.collection("wallet_registry").doc("01622223333"), {
    userId: BOB_UID,
    createdAt: ts,
  });
  bobOkBatch.update(bobDb.collection("users").doc(BOB_UID), {
    bkashPersonal: "01922223333",
    nagadPersonal: "01622223333",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 0,
    updatedAt: pastDate(290),
  });
  await assertSucceeds(bobOkBatch.commit());

  // Admin grants 1-time wallet change approval to Alice
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    paymentAccountsLocked: false,
    walletChangeApprovedOnce: true,
    updatedAt: pastDate(250),
  }));

  // Alice re-sets her wallets once (releases old registry, claims new unique numbers, re-locks)
  const resetBatch = aliceDb.batch();
  resetBatch.delete(aliceDb.collection("wallet_registry").doc("01712345678"));
  resetBatch.delete(aliceDb.collection("wallet_registry").doc("01812345678"));
  resetBatch.set(aliceDb.collection("wallet_registry").doc("01799998888"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  resetBatch.set(aliceDb.collection("wallet_registry").doc("01899998888"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  resetBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    bkashPersonal: "01799998888",
    nagadPersonal: "01899998888",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 1,
    updatedAt: pastDate(200),
  });
  await assertSucceeds(resetBatch.commit());

  // After 1-time reset locks again, Alice cannot update wallets a second time!
  const postResetFailBatch = aliceDb.batch();
  postResetFailBatch.set(aliceDb.collection("wallet_registry").doc("01511112222"), {
    userId: ALICE_UID,
    createdAt: ts,
  });
  postResetFailBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    bkashPersonal: "01511112222",
    nagadPersonal: "01899998888",
    paymentAccountsLocked: true,
    walletChangeApprovedOnce: false,
    walletResetCount: 2,
    updatedAt: pastDate(150),
  });
  await assertFails(postResetFailBatch.commit());
});

test("Admin can delete support tickets and toggle supportEnabled OFF/ON", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin = ownerDb();
  const ts = pastDate(500);

  // Support enabled by default when app_config missing or supportEnabled: true
  await assertSucceeds(aliceDb.collection("support_tickets").doc("tkt_1").set({
    userId: ALICE_UID,
    fullName: "Alice Rahman",
    username: "alice_bd",
    category: "WALLET_WITHDRAWAL",
    subject: "Change Wallet Number",
    message: "Please allow 1-time wallet reset.",
    status: "OPEN",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Regular user cannot delete support tickets
  await assertFails(aliceDb.collection("support_tickets").doc("tkt_1").delete());
  await assertFails(bobDb.collection("support_tickets").doc("tkt_1").delete());

  // Verified OwnerAdmin CAN delete support tickets
  await assertSucceeds(admin.collection("support_tickets").doc("tkt_1").delete());

  // Admin disables Support Center in app_config/payment_settings
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").set({
    bkashNumber: "01711112222",
    nagadNumber: "01811112222",
    vipBronzePriceBdt: 150,
    vipSilverPriceBdt: 350,
    vipGoldPriceBdt: 750,
    vipDiamondPriceBdt: 1500,
    latestAppVersion: "2.1.0",
    supportEnabled: false,
    updatedAt: ts,
  }));

  // User creating new support ticket fails while supportEnabled == false
  await assertFails(aliceDb.collection("support_tickets").doc("tkt_blocked").set({
    userId: ALICE_UID,
    fullName: "Alice Rahman",
    username: "alice_bd",
    category: "WALLET_WITHDRAWAL",
    subject: "Blocked ticket",
    message: "Should be denied while disabled.",
    status: "OPEN",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Admin re-enables Support Center
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").update({
    supportEnabled: true,
    updatedAt: pastDate(200),
  }));

  // User creating ticket succeeds again
  await assertSucceeds(aliceDb.collection("support_tickets").doc("tkt_allowed").set({
    userId: ALICE_UID,
    fullName: "Alice Rahman",
    username: "alice_bd",
    category: "WALLET_WITHDRAWAL",
    subject: "Allowed ticket",
    message: "Should succeed when enabled.",
    status: "OPEN",
    createdAt: ts,
    updatedAt: ts,
  }));
});

test("Withdrawal atomically deducts availableBalanceBdt in both users and leaderboard_public, blocks spoofing, supports self-heal sync & admin removal", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const admin = ownerDb();
  const ts = pastDate(500);

  // Setup Alice with 500 BDT earned and 500 BDT available
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("leaderboard_public").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 0,
    earningsBdt: 10,
    availableBalanceBdt: 10,
    updatedAt: ts,
  }));
  // Admin credits Alice to 500 BDT (leaving leaderboard slightly drifted to verify self-heal too)
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    availableBalanceBdt: 500,
    totalEarningsBdt: 500,
    completedTasksCount: 3,
    updatedAt: pastDate(400),
  }));

  // Spoofed leaderboard_public update that does NOT match users/{uid} fails
  await assertFails(aliceDb.collection("leaderboard_public").doc(ALICE_UID).update({
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 3,
    earningsBdt: 9999,
    availableBalanceBdt: 9999,
    updatedAt: pastDate(350),
  }));

  // Self-heal leaderboard_public sync matching existing users/{uid} succeeds
  await assertSucceeds(aliceDb.collection("leaderboard_public").doc(ALICE_UID).update({
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 3,
    earningsBdt: 500,
    availableBalanceBdt: 500,
    updatedAt: pastDate(300),
  }));

  // Atomic withdrawal of 300 BDT (+10 fee = 310 deducted) updates BOTH users and leaderboard_public
  const wdrBatch = aliceDb.batch();
  wdrBatch.set(aliceDb.collection("withdrawals").doc("w_live_1"), {
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 300,
    feeBdt: 10,
    totalDeductedBdt: 310,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  });
  wdrBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    availableBalanceBdt: 190,
    pendingBalanceBdt: 310,
    updatedAt: pastDate(200),
  });
  wdrBatch.update(aliceDb.collection("leaderboard_public").doc(ALICE_UID), {
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 3,
    earningsBdt: 500,
    availableBalanceBdt: 190,
    updatedAt: pastDate(200),
  });
  wdrBatch.set(aliceDb.collection("transactions").doc("txn_w_live_1"), {
    userId: ALICE_UID,
    titleBn: "বিকাশ পার্সোনাল উত্তোলন (৳300 + ৳১০ ফি)",
    titleEn: "bKash Personal Withdrawal (৳300 + ৳10 Fee)",
    type: "WITHDRAWAL",
    amountBdt: -310,
    status: "PENDING",
    referenceId: "w_live_1",
    createdAt: ts,
  });
  await assertSucceeds(wdrBatch.commit());

  // Regular users cannot delete withdrawal queue records
  await assertFails(aliceDb.collection("withdrawals").doc("w_live_1").delete());
  await assertFails(bobDb.collection("withdrawals").doc("w_live_1").delete());

  // Admin marks Payment Success and then removes it from the queue
  await assertSucceeds(admin.collection("withdrawals").doc("w_live_1").update({
    status: "COMPLETED",
    adminNote: "Payment Success",
    updatedAt: pastDate(100),
  }));
  await assertSucceeds(admin.collection("withdrawals").doc("w_live_1").delete());
});

test("Tiered bKash-style withdrawal fees enforce exact brackets and allow unlimited amounts >= 300", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(500);

  const validBrackets = [
    [500, 10],
    [800, 10],
    [801, 20],
    [1000, 20],
    [1001, 25],
    [1300, 25],
    [1301, 30],
    [1800, 30],
    [1801, 40],
    [2000, 40],
    [150000, 3000], // Unlimited above 300
  ];

  for (let i = 0; i < validBrackets.length; i++) {
    const [amt, fee] = validBrackets[i];
    await assertSucceeds(aliceDb.collection("withdrawals").doc(`w_tier_${i}`).set({
      userId: ALICE_UID,
      username: "alice_bd",
      method: "BKASH_PERSONAL",
      accountNumber: "01712345678",
      amountBdt: amt,
      feeBdt: fee,
      totalDeductedBdt: amt + fee,
      status: "PENDING",
      createdAt: ts,
      updatedAt: ts,
    }));
  }

  // Wrong fee for 1001 BDT (should be 25, attempting 10 or 20 fails)
  await assertFails(aliceDb.collection("withdrawals").doc("w_bad_tier_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 1001,
    feeBdt: 20,
    totalDeductedBdt: 1021,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Wrong fee for 1500 BDT (should be 30, attempting 10 fails)
  await assertFails(aliceDb.collection("withdrawals").doc("w_bad_tier_2").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 1500,
    feeBdt: 10,
    totalDeductedBdt: 1510,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));
});

test("Instant task credit atomically pays user on completion and blocks non-instant or inflated reward forgery", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(500);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("leaderboard_public").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 0,
    earningsBdt: 10,
    availableBalanceBdt: 10,
    updatedAt: ts,
  }));

  await assertSucceeds(admin.collection("tasks").doc("t_instant").set({
    titleBn: "ফাস্ট ইনকাম টাস্ক",
    titleEn: "Fast Income Task",
    category: "SURVEY",
    rewardBdt: 75,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Details",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Instructions",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: true,
    active: true,
    instantCredit: true,
    verificationCode: "BD77",
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertSucceeds(admin.collection("tasks").doc("t_manual").set({
    titleBn: "ম্যানুয়াল টাস্ক",
    titleEn: "Manual Task",
    category: "WEBSITE",
    rewardBdt: 60,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Details",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Instructions",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: false,
    active: true,
    instantCredit: false,
    verificationCode: "",
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Valid atomic instant-credit completion succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("task_proofs").doc(`t_instant_${ALICE_UID}`), {
    taskId: "t_instant",
    taskTitle: "Fast Income Task",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 75,
    proofNote: "BD77",
    status: "APPROVED",
    createdAt: ts,
    updatedAt: ts,
  });
  batch.update(aliceDb.collection("tasks").doc("t_instant"), {
    availableSlots: 9,
    updatedAt: pastDate(300),
  });
  batch.update(aliceDb.collection("users").doc(ALICE_UID), {
    availableBalanceBdt: 85,
    totalEarningsBdt: 85,
    dailyEarningsBdt: 75,
    completedTasksCount: 1,
    lastCompletedTaskId: "t_instant",
    updatedAt: pastDate(300),
  });
  batch.update(aliceDb.collection("leaderboard_public").doc(ALICE_UID), {
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 1,
    earningsBdt: 85,
    availableBalanceBdt: 85,
    updatedAt: pastDate(300),
  });
  batch.set(aliceDb.collection("transactions").doc("txn_instant_1"), {
    userId: ALICE_UID,
    titleBn: "টাস্ক পুরস্কার: ফাস্ট ইনকাম টাস্ক",
    titleEn: "Task Reward: Fast Income Task",
    type: "TASK_EARNING",
    amountBdt: 75,
    status: "COMPLETED",
    referenceId: `t_instant_${ALICE_UID}`,
    createdAt: ts,
  });
  await assertSucceeds(batch.commit());

  // Cannot forge instant credit on a manual review task (instantCredit: false)
  const badBatch = aliceDb.batch();
  badBatch.set(aliceDb.collection("task_proofs").doc(`t_manual_${ALICE_UID}`), {
    taskId: "t_manual",
    taskTitle: "Manual Task",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 60,
    proofNote: "fake",
    status: "APPROVED",
    createdAt: ts,
    updatedAt: ts,
  });
  badBatch.update(aliceDb.collection("tasks").doc("t_manual"), {
    availableSlots: 9,
    updatedAt: pastDate(200),
  });
  badBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    availableBalanceBdt: 145,
    totalEarningsBdt: 145,
    dailyEarningsBdt: 135,
    completedTasksCount: 2,
    lastCompletedTaskId: "t_manual",
    updatedAt: pastDate(200),
  });
  await assertFails(badBatch.commit());
});

test("VIP tiers grant small progressive task percentage bonuses (+2%/+4%/+7%/+10%) and block reward spoofing", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(500);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(validUserPayload(BOB_UID, "01812345678", "device_bob_vip")));
  await assertSucceeds(aliceDb.collection("leaderboard_public").doc(ALICE_UID).set({
    userId: ALICE_UID,
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "REGULAR",
    tasksCompleted: 0,
    earningsBdt: 10,
    availableBalanceBdt: 10,
    updatedAt: ts,
  }));

  // Upgrade Alice to GOLD (+7% bonus)
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    vipTier: "GOLD",
    updatedAt: pastDate(450),
  }));

  await assertSucceeds(admin.collection("tasks").doc("t_vip_100").set({
    titleBn: "ভিআইপি টেস্ট টাস্ক",
    titleEn: "VIP Test Task",
    category: "SURVEY",
    rewardBdt: 100,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Details",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Instructions",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: true,
    active: true,
    instantCredit: true,
    verificationCode: "",
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Alice (GOLD +7%) completes 100 BDT task -> earns 107 BDT atomically
  const goldBatch = aliceDb.batch();
  goldBatch.set(aliceDb.collection("task_proofs").doc(`t_vip_100_${ALICE_UID}`), {
    taskId: "t_vip_100",
    taskTitle: "VIP Test Task",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 107,
    proofNote: "Completed accurately",
    status: "APPROVED",
    createdAt: ts,
    updatedAt: ts,
  });
  goldBatch.update(aliceDb.collection("tasks").doc("t_vip_100"), {
    availableSlots: 9,
    updatedAt: pastDate(300),
  });
  goldBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    availableBalanceBdt: 117,
    totalEarningsBdt: 117,
    dailyEarningsBdt: 107,
    completedTasksCount: 1,
    lastCompletedTaskId: "t_vip_100",
    updatedAt: pastDate(300),
  });
  goldBatch.update(aliceDb.collection("leaderboard_public").doc(ALICE_UID), {
    displayName: "Alice Rahman",
    username: "alice_bd",
    vipTier: "GOLD",
    tasksCompleted: 1,
    earningsBdt: 117,
    availableBalanceBdt: 117,
    updatedAt: pastDate(300),
  });
  goldBatch.set(aliceDb.collection("transactions").doc("txn_gold_100"), {
    userId: ALICE_UID,
    titleBn: "ভিআইপি টেস্ট টাস্ক (+7% VIP)",
    titleEn: "VIP Test Task (+7% VIP)",
    type: "TASK_EARNING",
    amountBdt: 107,
    status: "COMPLETED",
    referenceId: `t_vip_100_${ALICE_UID}`,
    createdAt: ts,
  });
  await assertSucceeds(goldBatch.commit());

  // REGULAR user (Bob) CANNOT claim DIAMOND +10% (110 BDT) on 100 BDT task!
  const spoofBatch = bobDb.batch();
  spoofBatch.set(bobDb.collection("task_proofs").doc(`t_vip_100_${BOB_UID}`), {
    taskId: "t_vip_100",
    taskTitle: "VIP Test Task",
    userId: BOB_UID,
    username: "bob_bd",
    rewardBdt: 110,
    proofNote: "Trying to spoof Diamond VIP bonus",
    status: "APPROVED",
    createdAt: ts,
    updatedAt: ts,
  });
  spoofBatch.update(bobDb.collection("tasks").doc("t_vip_100"), {
    availableSlots: 8,
    updatedAt: pastDate(200),
  });
  spoofBatch.update(bobDb.collection("users").doc(BOB_UID), {
    availableBalanceBdt: 120,
    totalEarningsBdt: 120,
    dailyEarningsBdt: 110,
    completedTasksCount: 1,
    lastCompletedTaskId: "t_vip_100",
    updatedAt: pastDate(200),
  });
  await assertFails(spoofBatch.commit());
});

test("Maintenance mode (appStopped with countdown) blocks regular user actions and resumes when expired or turned off", async () => {
  const admin = ownerDb();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(500);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertSucceeds(admin.collection("users").doc(ALICE_UID).update({
    availableBalanceBdt: 500,
    totalEarningsBdt: 500,
    updatedAt: pastDate(400),
  }));
  await assertSucceeds(admin.collection("tasks").doc("t_maint_check").set({
    titleBn: "মেইনটেন্যান্স টেস্ট",
    titleEn: "Maintenance Test Task",
    category: "SURVEY",
    rewardBdt: 25,
    descriptionBn: "বিস্তারিত",
    descriptionEn: "Details",
    instructionsBn: "নির্দেশনা",
    instructionsEn: "Instructions",
    taskLink: "https://example.com",
    deadlineText: "2026-12-31",
    totalSlots: 10,
    availableSlots: 10,
    featured: false,
    active: true,
    instantCredit: false,
    verificationCode: "",
    createdBy: OWNER_UID,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Admin activates maintenance stop for +30 minutes
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").set({
    bkashNumber: "01711111111",
    nagadNumber: "01811111111",
    vipBronzePriceBdt: 150,
    vipSilverPriceBdt: 350,
    vipGoldPriceBdt: 750,
    vipDiamondPriceBdt: 1500,
    latestAppVersion: "1.0.0",
    appStopped: true,
    stopMessageBn: "অ্যাপসের আপডেটের কাজ চলমান।",
    stopEndEpochMillis: Date.now() + 30 * 60_000,
    supportEnabled: true,
    updatedAt: ts,
  }));

  // Regular user cannot submit tasks, withdrawals, VIP upgrades, or support tickets while stopped
  await assertFails(aliceDb.collection("task_proofs").doc(`t_maint_check_${ALICE_UID}`).set({
    taskId: "t_maint_check",
    taskTitle: "Maintenance Test Task",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 25,
    proofNote: "Tried during maintenance",
    status: "PENDING_REVIEW",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertFails(aliceDb.collection("withdrawals").doc("wdr_maint_1").set({
    userId: ALICE_UID,
    username: "alice_bd",
    method: "BKASH_PERSONAL",
    accountNumber: "01712345678",
    amountBdt: 300,
    feeBdt: 10,
    totalDeductedBdt: 310,
    status: "PENDING",
    createdAt: ts,
    updatedAt: ts,
  }));

  await assertFails(aliceDb.collection("support_tickets").doc("sup_maint_1").set({
    userId: ALICE_UID,
    fullName: "Alice Rahman",
    username: "alice_bd",
    category: "TASK_ISSUE",
    subject: "Help during update",
    message: "Trying to send a ticket while stopped",
    status: "OPEN",
    createdAt: ts,
    updatedAt: ts,
  }));

  // Admin turns maintenance off -> regular user actions work again
  await assertSucceeds(admin.collection("app_config").doc("payment_settings").update({
    appStopped: false,
    stopEndEpochMillis: 0,
    updatedAt: pastDate(100),
  }));

  await assertSucceeds(aliceDb.collection("task_proofs").doc(`t_maint_check_${ALICE_UID}`).set({
    taskId: "t_maint_check",
    taskTitle: "Maintenance Test Task",
    userId: ALICE_UID,
    username: "alice_bd",
    rewardBdt: 25,
    proofNote: "Completed after resume",
    status: "PENDING_REVIEW",
    createdAt: ts,
    updatedAt: ts,
  }));
});



