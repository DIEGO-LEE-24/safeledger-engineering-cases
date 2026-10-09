// Independent policy example. Never executes a shell or changes files.
const reads = ['read_file', 'search_files'];
const roles = Object.freeze({
  audit: Object.freeze([...reads]),
  reproduce: Object.freeze([...reads]),
  fix: Object.freeze([...reads, 'write_file']),
  judge: Object.freeze([...reads]),
});

function validRelativePath(value) {
  return typeof value === 'string' && value.length > 0
    && !value.startsWith('/') && !/[\\:\x00-\x1f\x7f]/.test(value)
    && value.split('/').every(part => part !== '' && part !== '.' && part !== '..');
}

function requestSnapshot(request) {
  if (!request || typeof request !== 'object' || Array.isArray(request)) return null;
  const prototype = Object.getPrototypeOf(request);
  if (prototype !== Object.prototype && prototype !== null) return null;
  const fields = Object.getOwnPropertyDescriptors(request);
  if (!fields.tool || !fields.path || !('value' in fields.tool) || !('value' in fields.path)) return null;
  return Object.freeze({ tool: fields.tool.value, path: fields.path.value });
}

export function evaluate(role, request) {
  const allowed = typeof role === 'string' && Object.hasOwn(roles, role) ? roles[role] : null;
  if (!allowed) return { allowed: false, reason: 'unknown-role' };
  const snapshot = requestSnapshot(request);
  if (!snapshot) return { allowed: false, reason: 'invalid-request' };
  if (!allowed.includes(snapshot.tool)) return { allowed: false, reason: 'tool-denied' };
  if (!validRelativePath(snapshot.path)) return { allowed: false, reason: 'invalid-path' };
  if (snapshot.tool === 'write_file' && !snapshot.path.startsWith('work/')) {
    return { allowed: false, reason: 'write-outside-workspace' };
  }
  return { allowed: true, reason: 'permitted' };
}

// The host supplies the role and handlers; a request cannot promote its role.
export function guardedRunner(role, handlers) {
  const trustedHandlers = { ...handlers };
  return async function invoke(request) {
    const snapshot = requestSnapshot(request);
    const decision = evaluate(role, snapshot);
    if (!decision.allowed) return decision;
    if (!Object.hasOwn(trustedHandlers, snapshot.tool)
        || typeof trustedHandlers[snapshot.tool] !== 'function') {
      return { allowed: false, reason: 'missing-handler' };
    }
    return { ...decision, result: await trustedHandlers[snapshot.tool](snapshot) };
  };
}
