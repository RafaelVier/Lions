import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { copyFileSync, mkdtempSync, mkdirSync, readFileSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';

function fixture(t, source) {
  const root = mkdtempSync(join(tmpdir(), 'lions-version-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  mkdirSync(join(root, 'scripts'));
  copyFileSync(new URL('./version.mjs', import.meta.url), join(root, 'scripts/version.mjs'));
  const buildFile = join(root, 'build.gradle.kts');
  writeFileSync(buildFile, source);
  return {
    read: () => readFileSync(buildFile, 'utf8'),
    run: (...args) => spawnSync(process.execPath, [join(root, 'scripts/version.mjs'), ...args], { encoding: 'utf8' }),
  };
}

test('reads the current version without changing the build file', (t) => {
  const source = 'group = "org.lions"\nversion = "0.0.1-SNAPSHOT"\n';
  const project = fixture(t, source);
  const result = project.run();
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stdout.trim(), '0.0.1-SNAPSHOT');
  assert.equal(project.read(), source);
});

test('updates only the version and preserves CRLF and Unicode', (t) => {
  const source = 'version = "0.0.1-SNAPSHOT"\r\ndescription = "Gestão"\r\n';
  const project = fixture(t, source);
  const result = project.run('1.2.3-rc.1');
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stdout.trim(), '1.2.3-rc.1');
  assert.equal(project.read(), source.replace('0.0.1-SNAPSHOT', '1.2.3-rc.1'));
});

test('rejects malformed or unsafe versions without modifying the file', (t) => {
  const source = 'version = "0.0.1-SNAPSHOT"\n';
  const project = fixture(t, source);
  for (const version of ['1.0', '01.0.0', '1.0.0; echo injected', '1.0.0\nother=bad', '1.0.0-' + 'x'.repeat(128)]) {
    assert.notEqual(project.run(version).status, 0, version);
    assert.equal(project.read(), source);
  }
});

test('rejects missing or ambiguous version declarations', (t) => {
  for (const source of ['', 'version = "1.0.0"\nversion = "2.0.0"\n']) {
    const project = fixture(t, source);
    assert.notEqual(project.run('3.0.0').status, 0);
    assert.equal(project.read(), source);
  }
});
