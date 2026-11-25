const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { initializeApp, deleteApp } = require('firebase/app');
const { getAuth, connectAuthEmulator, signInAnonymously, updateProfile } = require('firebase/auth');
const { getFunctions, connectFunctionsEmulator, httpsCallable } = require('firebase/functions');
const { collection, doc, getDoc, getDocs, setDoc, query, orderBy, limit, onSnapshot } = require('firebase/firestore');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { initializeApp: initializeAdmin, deleteApp: deleteAdmin } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getAuth: getAdminAuth } = require('firebase-admin/auth');
const projectId = 'demo-vocablens';
const apps = [];
let environment, admin;

before(async () => {
  environment = await initializeTestEnvironment({ projectId, firestore: {
    host: '127.0.0.1', port: 8080,
    rules: fs.readFileSync(path.join(__dirname, '../../../firestore.rules'), 'utf8')
  } });
  // Every client gets a fresh UID. Never wipe a developer's existing emulator data.
  admin = initializeAdmin({ projectId }, 'ranking-tests');
});

after(async () => {
  if (admin) {
    const uids = apps.map(app => getAuth(app).currentUser?.uid).filter(Boolean);
    for (const uid of uids) {
      await getFirestore(admin).recursiveDelete(getFirestore(admin).doc(`xpAccounts/${uid}`));
      await getFirestore(admin).doc(`leaderboard/${uid}`).delete();
      await getAdminAuth(admin).deleteUser(uid);
    }
  }
  await Promise.all(apps.map(deleteApp));
  await environment?.cleanup();
  if (admin) await deleteAdmin(admin);
});

async function client(signedIn = true) {
  const app = initializeApp({ projectId, apiKey: 'demo-key', authDomain: `${projectId}.firebaseapp.com` }, `client-${apps.length}`);
  apps.push(app);
  const auth = getAuth(app);
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  if (signedIn) {
    await signInAnonymously(auth);
    await updateProfile(auth.currentUser, { displayName: 'Người học thử' });
  }
  const functions = getFunctions(app, 'asia-southeast1');
  connectFunctionsEmulator(functions, '127.0.0.1', 5001);
  return { uid: auth.currentUser?.uid, submit: httpsCallable(functions, 'submitFlashcardSession') };
}

function session(id, offset = 0) {
  const at = Date.now() - 20_000 + offset;
  return { sessionId: id, startedAt: at, endedAt: at + 1000, activeDurationMs: 1000,
    reviews: [{ wordId: 'book', rating: 'AGAIN', reviewedAt: at + 500 }, { wordId: 'phone', rating: 'HARD', reviewedAt: at + 1000 }] };
}

test('callable rejects anonymous HTTP requests and mismatched queue owner', async () => {
  const guest = await client(false);
  await assert.rejects(guest.submit({ ownerId: 'A', session: session('guest') }), { code: 'functions/unauthenticated' });
  const user = await client();
  await assert.rejects(user.submit({ ownerId: 'other', session: session('wrong-owner') }), { code: 'functions/permission-denied' });
});

test('concurrent retries award once; changed payload and overlap are rejected; accounts stay isolated', async () => {
  const user = await client();
  const payload = session('same-session');
  const results = await Promise.all(Array.from({ length: 4 }, () => user.submit({ ownerId: user.uid, session: payload })));
  assert.equal(results.filter(it => !it.data.alreadyAwarded).length, 1);
  for (const result of results) {
    assert.equal(result.data.earnedXp, 10);
    assert.equal(result.data.totalXp, 10);
  }
  assert.equal((await getFirestore(admin).doc(`leaderboard/${user.uid}`).get()).get('totalXp'), 10);
  await assert.rejects(user.submit({ ownerId: user.uid, session: { ...payload, reviews: payload.reviews.slice(0, 1) } }),
    { code: 'functions/already-exists' });
  await assert.rejects(user.submit({ ownerId: user.uid, session: { ...payload, sessionId: 'overlap' } }),
    { code: 'functions/failed-precondition' });
  const other = await client();
  await other.submit({ ownerId: other.uid, session: payload });
  assert.equal((await getFirestore(admin).doc(`leaderboard/${other.uid}`).get()).get('totalXp'), 10);

  // Offline submissions may arrive out of order as long as their intervals do not overlap.
  await user.submit({ ownerId: user.uid, session: session('later', 5000) });
  await user.submit({ ownerId: user.uid, session: session('earlier', -5000) });
  assert.equal((await getFirestore(admin).doc(`leaderboard/${user.uid}`).get()).get('totalXp'), 30);
});

test('invalid payload leaves both receipts and leaderboard unchanged', async () => {
  const user = await client();
  const payload = session('invalid');
  payload.reviews.push(payload.reviews[0]);
  await assert.rejects(user.submit({ ownerId: user.uid, session: payload }), { code: 'functions/invalid-argument' });
  assert.equal((await getFirestore(admin).doc(`leaderboard/${user.uid}`).get()).exists, false);
  assert.equal((await getFirestore(admin).collection(`xpAccounts/${user.uid}/sessions`).get()).size, 0);
});

test('Android protobuf Long timestamps award XP and retry matches ordinary JSON numbers', async () => {
  const user = await client();
  const payload = session('android-long');
  const long = value => ({ '@type': 'type.googleapis.com/google.protobuf.Int64Value', value: String(value) });
  const android = { ...payload,
    startedAt: long(payload.startedAt), endedAt: long(payload.endedAt), activeDurationMs: long(payload.activeDurationMs),
    reviews: payload.reviews.map(review => ({ ...review, reviewedAt: long(review.reviewedAt) })) };
  const first = await user.submit({ ownerId: user.uid, session: android });
  assert.deepEqual(first.data, { earnedXp: 10, totalXp: 10, alreadyAwarded: false });
  const retry = await user.submit({ ownerId: user.uid, session: payload });
  assert.deepEqual(retry.data, { earnedXp: 10, totalXp: 10, alreadyAwarded: true });
});

test('Unicode reviews and reordered retries preserve the Python callable contract', async () => {
  const user = await client();
  const payload = session('unicode');
  payload.reviews[0].wordId = 'điện thoại';
  payload.reviews[1].wordId = '漢字';
  await user.submit({ ownerId: user.uid, session: payload });
  const retry = await user.submit({ ownerId: user.uid, session: { ...payload, reviews: [...payload.reviews].reverse() } });
  assert.deepEqual(retry.data, { earnedXp: 10, totalXp: 10, alreadyAwarded: true });
  assert.equal((await getFirestore(admin).doc(`leaderboard/${user.uid}`).get()).get('displayName'), 'Người học thử');
});

test('rules allow bounded signed-in ranking reads and deny score edits, private receipts and unauthenticated reads', async () => {
  const signed = environment.authenticatedContext('reader').firestore();
  const guest = environment.unauthenticatedContext().firestore();
  await assertSucceeds(getDocs(query(collection(signed, 'leaderboard'), orderBy('totalXp', 'desc'), orderBy('userId'), limit(50))));
  await assertFails(getDocs(collection(signed, 'leaderboard')));
  await assertFails(getDocs(query(collection(signed, 'leaderboard'), limit(51))));
  await assertFails(getDoc(doc(guest, 'leaderboard/reader')));
  await assertFails(setDoc(doc(signed, 'leaderboard/reader'), { totalXp: 999999 }));
  await assertFails(getDoc(doc(signed, 'xpAccounts/reader/sessions/secret')));
});

test('a realtime ranking listener receives an awarded score', async () => {
  const user = await client();
  const db = environment.authenticatedContext(user.uid).firestore();
  let stop;
  const updated = new Promise((resolve, reject) => {
    const timeout = setTimeout(() => { stop?.(); reject(new Error('Ranking listener timed out')); }, 15000);
    stop = onSnapshot(doc(db, 'leaderboard', user.uid), snapshot => {
      if (snapshot.exists() && snapshot.data().totalXp === 10) {
        clearTimeout(timeout); stop(); resolve();
      }
    }, error => { clearTimeout(timeout); reject(error); });
  });
  await user.submit({ ownerId: user.uid, session: session('live') });
  await updated;
});
