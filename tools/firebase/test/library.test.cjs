const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, deleteDoc, serverTimestamp, collection, getDocs } = require('firebase/firestore');
let environment;
const uid = `library-${Date.now()}`;
const word = () => ({ wordId: 'fish', english: 'fish', phonetic: '/fɪʃ/', meaning: 'cá',
  partOfSpeech: 'noun', exampleSentence: null, category: 'Objects', createdAt: 1, savedAt: 1,
  deleted: false, serverUpdatedAt: serverTimestamp() });
before(async () => {
  environment = await initializeTestEnvironment({ projectId: 'demo-vocablens', firestore: {
    host: '127.0.0.1', port: 8080, rules: fs.readFileSync(path.join(__dirname, '../../../firestore.rules'), 'utf8')
  }});
});
after(async () => {
  if (environment) {
    await environment.withSecurityRulesDisabled(async context => {
      const db = context.firestore();
      for (const [kind, id] of [['vocabulary','fish'], ['collections','animals'], ['items','membership']]) {
        await deleteDoc(doc(db, 'users', uid, kind, id));
      }
    });
    await environment.cleanup();
  }
});
test('library is private to owner; validates words, collections and memberships; keeps deletion tombstones', async () => {
  const own = environment.authenticatedContext(uid).firestore();
  const other = environment.authenticatedContext('other-user').firestore();
  const guest = environment.unauthenticatedContext().firestore();
  const ref = db => doc(db, 'users', uid, 'vocabulary', 'fish');
  await assertSucceeds(setDoc(ref(own), word()));
  await assertSucceeds(getDoc(ref(own)));
  await assertFails(getDoc(ref(other)));
  await assertFails(getDoc(ref(guest)));
  await assertFails(getDocs(collection(other, 'users', uid, 'vocabulary')));
  await assertFails(setDoc(ref(other), word()));
  await assertFails(setDoc(ref(own), { ...word(), meaning: '' }));
  await assertFails(setDoc(ref(own), { ...word(), wordId: 'different-id' }));
  await assertFails(setDoc(ref(own), { ...word(), totalXp: 1000 }));
  await assertFails(deleteDoc(ref(own)));
  const group = doc(own, 'users', uid, 'collections', 'animals');
  await assertSucceeds(setDoc(group, { collectionId: 'animals', name: 'Animals', description: null,
    createdAt: 1, updatedAt: 1, deleted: false, serverUpdatedAt: serverTimestamp() }));
  const item = doc(own, 'users', uid, 'items', 'membership');
  await assertSucceeds(setDoc(item, { collectionId: 'animals', wordId: 'fish', addedAt: 1,
    deleted: false, serverUpdatedAt: serverTimestamp() }));
  await assertFails(setDoc(group, { collectionId: 'animals', name: 'x'.repeat(121), description: null,
    createdAt: 1, updatedAt: 1, deleted: false, serverUpdatedAt: serverTimestamp() }));
  await assertSucceeds(setDoc(ref(own), { wordId: 'fish', deleted: true, serverUpdatedAt: serverTimestamp() }));
  assert.equal((await getDoc(ref(own))).data().deleted, true);
  await assertSucceeds(setDoc(item, { collectionId: 'animals', wordId: 'fish', deleted: true, serverUpdatedAt: serverTimestamp() }));
  await assertSucceeds(setDoc(group, { collectionId: 'animals', deleted: true, serverUpdatedAt: serverTimestamp() }));
  await assertFails(setDoc(doc(own, 'leaderboard', uid), { totalXp: 1000 }));
});
