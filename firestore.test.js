const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_firebase_uid";
const BOB_UID = "bob_firebase_uid";
const ALICE_ACCT = "acct_alice_01";
const BOB_ACCT = "acct_bob_02";
const THREAD_ID = "acct_alice_01__acct_bob_02";
const ALICE_PHONE_HASH = "a".repeat(64);
const BOB_PHONE_HASH = "b".repeat(64);
const ALICE_TOKEN_HASH = "1".repeat(64);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  const [host, portStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host,
      port: parseInt(portStr, 10),
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

async function seedUsers(aliceDb, bobDb) {
  const now = new Date(Date.now() - 2000);
  await aliceDb.collection("users").doc(ALICE_ACCT).set({
    accountId: ALICE_ACCT,
    ownerFirebaseUid: ALICE_UID,
    authorizedFirebaseUids: [ALICE_UID],
    displayName: "Alice Reader",
    bio: "Night reader",
    secretTheme: "INSTAGRAM",
    online: true,
    lastActive: now,
    createdAt: now,
    updatedAt: now,
  });
  await bobDb.collection("users").doc(BOB_ACCT).set({
    accountId: BOB_ACCT,
    ownerFirebaseUid: BOB_UID,
    authorizedFirebaseUids: [BOB_UID],
    displayName: "Bob Reader",
    bio: "Mystery fan",
    secretTheme: "EMERALD",
    online: true,
    lastActive: now,
    createdAt: now,
    updatedAt: now,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("phone_claims").doc(ALICE_PHONE_HASH).get());
  await assertFails(unauthDb.collection("nfc_links").doc(ALICE_TOKEN_HASH).get());
});

test("Private PII & NFC link listing is denied and cross-user private read is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await seedUsers(aliceDb, bobDb);
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_ACCT).collection("private").doc("account").set({
      accountId: ALICE_ACCT,
      phoneE164: "+15551112222",
      phoneVerified: true,
      personalLinkToken: "secret_random_token_123456789",
      activeTokenHash: ALICE_TOKEN_HASH,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_ACCT).collection("private").doc("account").get()
  );
  await assertFails(aliceDb.collection("phone_claims").get());
  await assertFails(aliceDb.collection("nfc_links").get());
});

test("Phone claims enforce one account per phone number", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("phone_claims").doc(ALICE_PHONE_HASH).set({
      phoneHash: ALICE_PHONE_HASH,
      accountId: ALICE_ACCT,
      ownerFirebaseUid: ALICE_UID,
      passwordSalt: "random_salt_1234",
      passwordHash: "a1b2c3d4".repeat(8),
      phoneVerified: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    bobDb.collection("phone_claims").doc(ALICE_PHONE_HASH).set({
      phoneHash: ALICE_PHONE_HASH,
      accountId: BOB_ACCT,
      ownerFirebaseUid: BOB_UID,
      passwordSalt: "other_salt_1234",
      passwordHash: "e5f6a1b2".repeat(8),
      phoneVerified: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Backend blocking prevents blocked users from sending messages or initiating calls", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await seedUsers(aliceDb, bobDb);
  const now = new Date(Date.now() - 1000);

  // Alice creates conversation and sends first message before blocking
  await assertSucceeds(
    aliceDb.collection("conversations").doc(THREAD_ID).set({
      memberAccountIds: [ALICE_ACCT, BOB_ACCT],
      memberFirebaseUids: [ALICE_UID, BOB_UID],
      lastMessageText: "Hello!",
      lastSenderAccountId: ALICE_ACCT,
      lastMessageAt: now,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("conversations").doc(THREAD_ID).collection("messages").doc("m1").set({
      threadId: THREAD_ID,
      senderAccountId: ALICE_ACCT,
      recipientAccountId: BOB_ACCT,
      senderFirebaseUid: ALICE_UID,
      memberFirebaseUids: [ALICE_UID, BOB_UID],
      text: "Hello!",
      seen: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Recipient Bob can mark seen=true
  await assertSucceeds(
    bobDb.collection("conversations").doc(THREAD_ID).collection("messages").doc("m1").update({
      seen: true,
      seenAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Alice blocks Bob
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_ACCT).collection("blocked").doc(BOB_ACCT).set({
      blockedAccountId: BOB_ACCT,
      blockedDisplayName: "Bob Reader",
      createdAt: now,
    })
  );

  // Bob CANNOT send message to Alice anymore
  await assertFails(
    bobDb.collection("conversations").doc(THREAD_ID).collection("messages").doc("m2").set({
      threadId: THREAD_ID,
      senderAccountId: BOB_ACCT,
      recipientAccountId: ALICE_ACCT,
      senderFirebaseUid: BOB_UID,
      memberFirebaseUids: [ALICE_UID, BOB_UID],
      text: "Bypass?",
      seen: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob CANNOT initiate a call to Alice anymore
  await assertFails(
    bobDb.collection("conversations").doc(THREAD_ID).collection("calls").doc("c1").set({
      threadId: THREAD_ID,
      senderAccountId: BOB_ACCT,
      recipientAccountId: ALICE_ACCT,
      senderFirebaseUid: BOB_UID,
      memberFirebaseUids: [ALICE_UID, BOB_UID],
      callType: "AUDIO",
      status: "RINGING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice unblocks Bob -> Bob can message again
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_ACCT).collection("blocked").doc(BOB_ACCT).delete()
  );
  await assertSucceeds(
    bobDb.collection("conversations").doc(THREAD_ID).collection("messages").doc("m3").set({
      threadId: THREAD_ID,
      senderAccountId: BOB_ACCT,
      recipientAccountId: ALICE_ACCT,
      senderFirebaseUid: BOB_UID,
      memberFirebaseUids: [ALICE_UID, BOB_UID],
      text: "Back again",
      seen: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});
