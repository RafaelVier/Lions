import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, existsSync, rmSync, readdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';

export const bashPath = (path) => process.platform === 'win32'
  ? path.replaceAll('\\', '/').replace(/^([A-Za-z]):/, (_, drive) => `/${drive.toLowerCase()}`)
  : path;

export function shellFixture(t) {
  const root = mkdtempSync(join(tmpdir(), 'lions-infra-'));
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const write = (path, text) => {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    writeFileSync(join(root, path), text, { mode: 0o755 });
  };
  mkdirSync(join(root, 'bin'));
  return {
    root,
    path: (path) => bashPath(join(root, path)),
    write,
    stub: (name, body) => write(`bin/${name}`, `#!/usr/bin/env bash\nset -euo pipefail\n${body}\n`),
    read: (path) => existsSync(join(root, path)) ? readFileSync(join(root, path), 'utf8') : '',
    files: (path) => existsSync(join(root, path)) ? readdirSync(join(root, path)) : [],
    run: (script, args = [], env = {}) => {
      const result = spawnSync(process.env.BASH_BINARY || 'bash',
        ['-c', 'export PATH="$1:$PATH"; shift; exec bash "$@"', 'test', bashPath(join(root, 'bin')), bashPath(script), ...args],
        { cwd: root, encoding: 'utf8', env: { ...process.env, ...env } });
      if (result.error) throw result.error;
      return result;
    },
  };
}
