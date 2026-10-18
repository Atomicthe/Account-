const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: cannot read another user's account", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("accounts").doc("bob_wallet").set({
      id: "bob_wallet",
      userId: BOB_UID,
      name: "Bob Cash",
      type: "CASH",
      balance: 1000,
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("accounts").doc("bob_wallet").get());
});

test("Authenticated user: can create and read their own account", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accountRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1");
  
  await assertSucceeds(accountRef.set({
    id: "acc_1",
    userId: ALICE_UID,
    name: "牛皮紙皮夾",
    type: "CASH",
    balance: 5000,
    createdAt: new Date(),
  }));

  await assertSucceeds(accountRef.get());
});

test("Authenticated user: can create their own transaction", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");

  await assertSucceeds(txRef.set({
    id: "tx_1",
    userId: ALICE_UID,
    type: "EXPENSE",
    amount: 150,
    accountId: "acc_1",
    category: "餐飲",
    date: "2026-10-17",
    timestampMs: Date.now(),
    note: "復古老派咖啡",
    merchant: "黑膠咖啡館",
    tags: "#日常",
    stamp: "PAID",
    createdAt: new Date(),
  }));
});
