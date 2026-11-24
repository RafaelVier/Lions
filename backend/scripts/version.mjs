import { readFileSync, writeFileSync } from 'node:fs';

const buildFile = new URL('../build.gradle.kts', import.meta.url);
const declaration = /^version\s*=\s*"([^"\r\n]+)"[^\S\r\n]*$/gm;
const versionPattern = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-[A-Za-z0-9]+([.-][A-Za-z0-9]+)*)?$/;

try {
  const source = readFileSync(buildFile, 'utf8');
  const matches = [...source.matchAll(declaration)];
  if (matches.length !== 1) {
    throw new Error('Expected one version = "..." declaration in build.gradle.kts.');
  }
  const current = matches[0][1];
  const requested = process.argv[2];
  const version = requested || current;
  if (process.argv.length > 3 || !versionPattern.test(version) || version.length > 128) {
    throw new Error('Use a version such as 1.0.0 or 1.1.0-SNAPSHOT (maximum 128 characters).');
  }
  if (requested && requested !== current) {
    const match = matches[0];
    // Replace only the value, preserving the remaining file contents and line endings.
    const start = match.index + match[0].indexOf('"') + 1;
    writeFileSync(buildFile, source.slice(0, start) + version + source.slice(start + current.length));
    console.error(`Backend version: ${current} -> ${version}`);
  }
  console.log(version);
} catch (error) {
  console.error(error.message);
  process.exitCode = 1;
}
