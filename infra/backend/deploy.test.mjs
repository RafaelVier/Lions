import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const image = `example/backend@sha256:${'a'.repeat(64)}`;
const previous = `BACKEND_IMAGE=example/backend@sha256:${'b'.repeat(64)}\n`;
const script = fileURLToPath(new URL('./deploy.sh', import.meta.url));
const shell = process.env.BASH_BINARY || 'bash';
const bashPath = (path) => process.platform === 'win32'
  ? path.replaceAll('\\', '/').replace(/^([A-Za-z]):/, (_, drive) => `/${drive.toLowerCase()}`)
  : path;

function deploy(t, { fail = '', ref = image, config = true } = {}) {
  const root = mkdtempSync(join(tmpdir(), 'lions-deploy-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const bin = join(root, 'bin');
  const directory = join(root, 'server');
  mkdirSync(bin);
  mkdirSync(directory);
  writeFileSync(join(directory, 'compose.yaml'), 'services: {}\n');
  if (config) writeFileSync(join(directory, '.env.prod'), '');
  writeFileSync(join(directory, '.release.env'), previous);
  const log = join(root, 'docker.log');
  writeFileSync(join(bin, 'docker'), `#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$DOCKER_TEST_LOG"
[[ "$*" != *" $DOCKER_TEST_FAIL "* ]]
`, { mode: 0o755 });
  const result = spawnSync(shell, ['-c', 'export PATH="$1:$PATH"; exec bash "$2" "$3" "$4"',
    'test', bashPath(bin), bashPath(script), bashPath(directory), ref], {
    encoding: 'utf8',
    env: { ...process.env, DOCKER_TEST_LOG: bashPath(log), DOCKER_TEST_FAIL: fail || 'never-match' },
  });
  if (result.error) throw result.error;
  return {
    ...result,
    calls: existsSync(log) ? readFileSync(log, 'utf8').trim().split('\n') : [],
    release: readFileSync(join(directory, '.release.env'), 'utf8'),
  };
}

test('deploy pulls before updating only backend, waits for health and records the digest', (t) => {
  const result = deploy(t);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.calls.length, 3);
  assert.match(result.calls[0], /config --quiet$/);
  assert.match(result.calls[1], /pull backend$/);
  assert.match(result.calls[2], /up -d --no-deps --wait --wait-timeout 240 backend$/);
  assert.equal(result.release, `BACKEND_IMAGE=${image}\n`);
});

for (const failure of ['config', 'pull', 'up']) {
  test(`${failure} failure stops deployment and preserves the last healthy digest`, (t) => {
    const result = deploy(t, { fail: failure });
    assert.notEqual(result.status, 0);
    assert.equal(result.calls.length, ['config', 'pull', 'up'].indexOf(failure) + 1);
    assert.equal(result.release, previous);
  });
}

test('invalid image is rejected before calling Docker', (t) => {
  const result = deploy(t, { ref: 'example/backend:latest' });
  assert.notEqual(result.status, 0);
  assert.equal(result.calls.length, 0);
  assert.equal(result.release, previous);
});

test('missing server configuration is rejected before calling Docker', (t) => {
  const result = deploy(t, { config: false });
  assert.notEqual(result.status, 0);
  assert.equal(result.calls.length, 0);
  assert.equal(result.release, previous);
});
