import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { shellFixture } from '../test-support.mjs';

const script = fileURLToPath(new URL('./deploy.sh', import.meta.url));
const initScript = fileURLToPath(new URL('./init-app-db.sh', import.meta.url));

function database(t, { existing = '', fail = '', volume = 'yes' } = {}) {
  const fixture = shellFixture(t);
  for (const name of ['.env.database', 'compose.yaml', 'init-app-db.sh']) fixture.write(`server/${name}`, '');
  fixture.write('server/backup.sh', readFileSync(new URL('./backup.sh', import.meta.url), 'utf8'));
  fixture.stub('docker', `
printf '%s\\n' "$*" >> "$TEST_LOG"
if [[ "$TEST_FAIL" != none && "$*" == *" $TEST_FAIL "* ]]; then exit 1; fi
if [[ "$*" == *' ps -a -q postgres' ]]; then printf '%s' "$TEST_EXISTING"; fi
if [[ "$*" == 'volume inspect '* && "$TEST_VOLUME" == no ]]; then exit 1; fi
if [[ "$*" == *'exec pg_dump '* ]]; then printf 'PGDMP-test-data'; fi
`);
  const result = fixture.run(script, [fixture.path('server')], {
    TEST_LOG: fixture.path('calls'), TEST_EXISTING: existing, TEST_FAIL: fail || 'none', TEST_VOLUME: volume,
  });
  return { ...result, fixture, calls: fixture.read('calls') };
}

test('first deployment creates persistent volume, waits for health and checks application login', (t) => {
  const result = database(t, { volume: 'no' });
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.calls, /volume create lions-postgres-data/);
  assert.match(result.calls, /up -d --no-deps --wait --wait-timeout 180 postgres/);
  assert.match(result.calls, /psql -h 127.0.0.1 -U "\$APP_DB_USER"/);
  assert.doesNotMatch(result.calls, /pg_dump/);
});

test('updating an existing database backs up before replacing its container', (t) => {
  const result = database(t, { existing: 'container-id' });
  assert.equal(result.status, 0, result.stderr);
  assert.ok(result.calls.indexOf('pg_dump') < result.calls.indexOf(' up -d'));
  const files = result.fixture.files('server/backups');
  assert.equal(files.length, 1);
  assert.equal(result.fixture.read(`server/backups/${files[0]}`), 'PGDMP-test-data');
});

test('failed backup blocks database replacement and removes its incomplete dump', (t) => {
  const result = database(t, { existing: 'container-id', fail: 'exec' });
  assert.notEqual(result.status, 0);
  assert.doesNotMatch(result.calls, / up -d/);
  assert.deepEqual(result.fixture.files('server/backups'), []);
});

test('failed image pull leaves the running database untouched', (t) => {
  const result = database(t, { existing: 'container-id', fail: 'pull' });
  assert.notEqual(result.status, 0);
  assert.doesNotMatch(result.calls, /pg_dump| up -d/);
});

test('failed application login fails deployment even after pg_isready succeeds', (t) => {
  const result = database(t, { fail: 'exec' });
  assert.notEqual(result.status, 0);
  assert.match(result.calls, / up -d/);
});

for (const env of [{ APP_DB_USER: 'postgres' }, { APP_DB_NAME: 'template1' }, { APP_DB_USER: 'bad;role' }]) {
  test(`initialization rejects reserved or unsafe names: ${JSON.stringify(env)}`, (t) => {
    const fixture = shellFixture(t);
    fixture.stub('psql', `touch "$TEST_CALLED"`);
    const result = fixture.run(initScript, [], {
      APP_DB_NAME: 'lionsdb', APP_DB_USER: 'lions', APP_DB_PASSWORD: 'test',
      TEST_CALLED: fixture.path('psql-called'), ...env,
    });
    assert.notEqual(result.status, 0);
    assert.deepEqual(fixture.files('psql-called'), []);
  });
}
