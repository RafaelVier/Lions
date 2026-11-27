import { test } from 'node:test';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';
import { shellFixture } from '../test-support.mjs';

const script = fileURLToPath(new URL('./deploy-via-ssh.sh', import.meta.url));

function deploy(t, service, overrides = {}) {
  const fixture = shellFixture(t);
  fixture.write('infra/backend/deploy.sh', '# backend script\n');
  fixture.write('infra/database/deploy.sh', '# database script\n');
  fixture.write('runner/placeholder', '');
  fixture.stub('ssh-keygen', 'exit 0');
  fixture.stub('scp', `printf 'scp %s\\n' "$*" >> "$TEST_LOG"`);
  fixture.stub('ssh', `
printf 'ssh %s\\n' "$*" >> "$TEST_LOG"
if [[ "$*" == *'bash -s'* ]]; then
  cat > /dev/null
  if [[ "$TEST_FAIL" == yes ]]; then exit 1; fi
fi
`);
  const result = fixture.run(script, [service], {
    SSH_HOST: 'example.test', SSH_USER: 'lions', SSH_PRIVATE_KEY: 'test-only-key',
    SSH_KNOWN_HOSTS: 'test-only-host', SSH_PORT: '22', RUNNER_TEMP: fixture.path('runner'),
    DEPLOY_PATH: '/opt/lions/backend', DATABASE_DEPLOY_PATH: '/opt/lions/database',
    BACKEND_IMAGE: `example/backend@sha256:${'a'.repeat(64)}`,
    TEST_LOG: fixture.path('calls'), TEST_FAIL: 'no', ...overrides,
  });
  return { ...result, fixture, calls: fixture.read('calls') };
}

test('backend deploy requires a successful database backup before running migrations', (t) => {
  const result = deploy(t, 'backend');
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.calls, /backup\.sh' '\/opt\/lions\/database' && bash -s/);
  assert.match(result.calls, /StrictHostKeyChecking=yes/);
  assert.match(result.calls, /BatchMode=yes/);
  assert.deepEqual(result.fixture.files('runner'), ['placeholder']);
});

test('database deploy uploads its initialization and backup scripts', (t) => {
  const result = deploy(t, 'database');
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.calls, /scp .*infra\/database\/init-app-db\.sh infra\/database\/backup\.sh/);
  assert.match(result.calls, /test -f '\/opt\/lions\/database\/\.env\.database'/);
  assert.doesNotMatch(result.calls, /@sha256:/);
});

test('remote failure fails the job and cleans temporary credentials', (t) => {
  const result = deploy(t, 'backend', { TEST_FAIL: 'yes' });
  assert.notEqual(result.status, 0);
  assert.deepEqual(result.fixture.files('runner'), ['placeholder']);
});

for (const env of [{ SSH_HOST: 'host;command' }, { DATABASE_DEPLOY_PATH: '/opt/../backend' },
  { DATABASE_DEPLOY_PATH: '/opt/lions/backend' }, { SSH_PORT: '0' }, { BACKEND_IMAGE: 'repo:latest' }]) {
  test(`rejects unsafe configuration before SSH: ${JSON.stringify(env)}`, (t) => {
    const result = deploy(t, 'backend', env);
    assert.notEqual(result.status, 0);
    assert.equal(result.calls, '');
  });
}
