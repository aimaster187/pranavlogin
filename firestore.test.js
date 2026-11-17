import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
function loadMod(name) {
  try {
    return require(name);
  } catch {
    try {
      return require(`/usr/local/lib/node_modules/${name}`);
    } catch {
      return require(`/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/${name}`);
    }
  }
}

const { initializeTestEnvironment, assertFails, assertSucceeds } = loadMod("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc, updateDoc, serverTimestamp } = loadMod("firebase/firestore");

const PROJECT_ID = "demo-pranav-rules";
const OWNER_UID = "owner_pranav_uid";
const OTHER_UID = "intruder_uid";

const rules = fs.readFileSync("firestore.rules", "utf8");

let testEnv;

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: { rules }
  });
});

test.after(async () => {
  if (testEnv) await testEnv.cleanup();
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
});

const OWNER_CLAIMS = { email: "ai.master.bot.official@gmail.com", email_verified: true };
const OTHER_CLAIMS = { email: "random.user@gmail.com", email_verified: true };

test("Root owner can create workspace, app, and valid vaulted bot metadata", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, OWNER_CLAIMS).firestore();

  await assertSucceeds(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/workspace/primary`), {
      ownerUid: OWNER_UID,
      ownerEmail: "ai.master.bot.official@gmail.com",
      masterMaintenance: false,
      maintenanceMessage: "OK",
      emergencyLockdown: false,
      domainHost: "pranavsmm.shop",
      environment: "production",
      enabledModules: { core_dashboard_telemetry: true },
      updatedAt: serverTimestamp()
    })
  );

  await assertSucceeds(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/apps/app_1`), {
      ownerUid: OWNER_UID,
      appCode: "app_1",
      packageName: "com.pranav.demo",
      displayName: "Demo Managed App",
      masterEnabled: true,
      forceUpdateEnabled: true,
      laterEnabled: false,
      rolloutPercent: 50,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );

  await assertSucceeds(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/bots/bot_1`), {
      ownerUid: OWNER_UID,
      botUsername: "pranav_user_bot",
      botDisplayName: "PRANAV User Bot",
      roleType: "USER_BOT",
      mode: "POLLING",
      commandsCount: 14,
      tokenSecretRef: "vault://bots/bot_1",
      healthStatus: "ACTIVE",
      enabled: true,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );
});

test("Unauthenticated and non-owner users (even under their own UID) are denied access", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, OWNER_CLAIMS).firestore();
  await setDoc(doc(ownerDb, `users/${OWNER_UID}/apps/app_1`), {
    ownerUid: OWNER_UID,
    appCode: "app_1",
    packageName: "com.pranav.demo",
    displayName: "Demo",
    masterEnabled: true,
    forceUpdateEnabled: false,
    laterEnabled: true,
    rolloutPercent: 100,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  });

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(getDoc(doc(anonDb, `users/${OWNER_UID}/apps/app_1`)));

  const otherDb = testEnv.authenticatedContext(OTHER_UID, OTHER_CLAIMS).firestore();
  await assertFails(getDoc(doc(otherDb, `users/${OWNER_UID}/apps/app_1`)));
  await assertFails(
    setDoc(doc(otherDb, `users/${OTHER_UID}/apps/app_hack`), {
      ownerUid: OTHER_UID,
      appCode: "hack",
      packageName: "com.evil",
      displayName: "Evil",
      masterEnabled: true,
      forceUpdateEnabled: false,
      laterEnabled: true,
      rolloutPercent: 100,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );
});

test("Rules reject raw botToken, invalid forceUpdate+later, and createdAt mutation", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, OWNER_CLAIMS).firestore();

  // Reject raw botToken field
  await assertFails(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/bots/bot_bad`), {
      ownerUid: OWNER_UID,
      botUsername: "bad_bot",
      botToken: "123456:SECRET_RAW_TOKEN",
      tokenSecretRef: "vault://bots/bad",
      roleType: "USER_BOT",
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );

  // Reject forceUpdateEnabled=true with laterEnabled=true
  await assertFails(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/apps/app_bad_update`), {
      ownerUid: OWNER_UID,
      appCode: "app_bad",
      packageName: "com.pranav.bad",
      displayName: "Bad",
      masterEnabled: true,
      forceUpdateEnabled: true,
      laterEnabled: true,
      rolloutPercent: 100,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );

  // Create valid app then reject mutating createdAt
  await assertSucceeds(
    setDoc(doc(ownerDb, `users/${OWNER_UID}/apps/app_2`), {
      ownerUid: OWNER_UID,
      appCode: "app_2",
      packageName: "com.pranav.two",
      displayName: "App Two",
      masterEnabled: true,
      forceUpdateEnabled: false,
      laterEnabled: true,
      rolloutPercent: 100,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );

  await assertFails(
    updateDoc(doc(ownerDb, `users/${OWNER_UID}/apps/app_2`), {
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );
});
