import test from 'node:test';
import assert from 'node:assert/strict';
import { evaluate, guardedRunner } from './guard.mjs';

test('all defined roles permit a normal read', () => {
  for (const role of ['audit', 'reproduce', 'fix', 'judge']) {
    assert.equal(evaluate(role, { tool: 'read_file', path: 'src/main.js' }).allowed, true);
  }
});
test('search is permitted without granting shell execution', () => {
  assert.equal(evaluate('audit', { tool: 'search_files', path: 'src' }).allowed, true);
  assert.equal(evaluate('audit', { tool: 'shell', path: 'src' }).allowed, false);
});
test('false positive regression: reading deployment notes is allowed', () => {
  assert.equal(evaluate('audit', { tool: 'read_file', path: 'docs/deploy-notes.md' }).allowed, true);
});
test('false positive regression: reading a delete-named file is allowed', () => {
  assert.equal(evaluate('audit', { tool: 'read_file', path: 'src/delete-handler.js' }).allowed, true);
});
test('false negative regression: benign filename does not permit mutation', () => {
  assert.equal(evaluate('audit', { tool: 'write_file', path: 'work/readme.md' }).allowed, false);
});
test('fix role can write only within its synthetic workspace', () => {
  assert.equal(evaluate('fix', { tool: 'write_file', path: 'work/result.txt' }).allowed, true);
  assert.equal(evaluate('fix', { tool: 'write_file', path: 'src/main.js' }).allowed, false);
});
test('other roles cannot write even in workspace', () => {
  for (const role of ['audit', 'reproduce', 'judge']) {
    assert.equal(evaluate(role, { tool: 'write_file', path: 'work/result.txt' }).allowed, false);
  }
});
test('deletion and remote push are denied to every role', () => {
  for (const role of ['audit', 'reproduce', 'fix', 'judge']) {
    for (const tool of ['delete_file', 'push_repository']) {
      assert.equal(evaluate(role, { tool, path: 'work/result.txt' }).allowed, false);
    }
  }
});
test('unknown role and unknown tool fail closed', () => {
  assert.equal(evaluate('other', { tool: 'read_file', path: 'src' }).allowed, false);
  assert.equal(evaluate('fix', { tool: 'unrecognized', path: 'work/a' }).allowed, false);
});
test('path traversal is denied, including within work prefix', () => {
  for (const path of ['../a', 'work/../a', 'work/../../a']) {
    assert.equal(evaluate('fix', { tool: 'write_file', path }).allowed, false);
  }
});
test('absolute paths and alternative separators are denied', () => {
  for (const path of ['/tmp/a', 'X:/a', 'work\\a', '//host/a']) {
    assert.equal(evaluate('fix', { tool: 'write_file', path }).allowed, false);
  }
});
test('empty segments, control characters, and absent paths are denied', () => {
  for (const path of ['', 'work//a', 'work/./a', 'work/a\0', undefined]) {
    assert.equal(evaluate('fix', { tool: 'write_file', path }).allowed, false);
  }
});
test('malformed requests fail closed', () => {
  for (const request of [null, undefined, [], 'read']) {
    assert.equal(evaluate('audit', request).allowed, false);
  }
});
test('request role cannot promote trusted audit role', async () => {
  let calls = 0;
  const invoke = guardedRunner('audit', { write_file: () => calls++ });
  const result = await invoke({ role: 'fix', tool: 'write_file', path: 'work/a' });
  assert.equal(result.allowed, false);
  assert.equal(calls, 0);
});
test('denied request never reaches supplied handler', async () => {
  let calls = 0;
  const invoke = guardedRunner('fix', { delete_file: () => calls++ });
  assert.equal((await invoke({ tool: 'delete_file', path: 'work/a' })).allowed, false);
  assert.equal(calls, 0);
});
test('allowed request reaches handler once', async () => {
  let calls = 0;
  const invoke = guardedRunner('audit', { read_file: () => { calls++; return 'synthetic'; } });
  const result = await invoke({ tool: 'read_file', path: 'src/a' });
  assert.equal(result.result, 'synthetic');
  assert.equal(calls, 1);
});
test('missing handler fails closed', async () => {
  const invoke = guardedRunner('fix', {});
  assert.equal((await invoke({ tool: 'write_file', path: 'work/a' })).allowed, false);
});
test('handler errors are not converted to successful validation', async () => {
  const invoke = guardedRunner('audit', { read_file: () => { throw new Error('synthetic bug'); } });
  await assert.rejects(invoke({ tool: 'read_file', path: 'src/a' }), /synthetic bug/);
});
test('accessor-based request cannot change tool after validation', async () => {
  let reads = 0;
  let calls = 0;
  const request = { path: 'work/a', get tool() { reads++; return reads === 1 ? 'read_file' : 'write_file'; } };
  const invoke = guardedRunner('audit', { read_file: () => calls++, write_file: () => calls++ });
  assert.equal((await invoke(request)).allowed, false);
  assert.equal(reads, 0);
  assert.equal(calls, 0);
});
test('handler receives the immutable validated request, not mutable caller data', async () => {
  const request = { tool: 'read_file', path: 'src/a' };
  const invoke = guardedRunner('audit', { read_file: checked => {
    request.tool = 'write_file';
    assert.equal(checked.tool, 'read_file');
    assert.equal(Object.isFrozen(checked), true);
    return checked.path;
  } });
  assert.equal((await invoke(request)).result, 'src/a');
});
