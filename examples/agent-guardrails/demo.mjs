import { guardedRunner } from './guard.mjs';

const invoke = guardedRunner('audit', {
  read_file: request => `Synthetic content for ${request.path}`,
  write_file: () => 'This handler must not run for audit',
});
console.log(await invoke({ tool: 'read_file', path: 'docs/deploy-notes.md' }));
console.log(await invoke({ tool: 'write_file', path: 'work/draft.md' }));
