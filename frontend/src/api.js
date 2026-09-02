// Thin REST client for room lifecycle + game reads. Gameplay actions go over STOMP.

async function request(path, options) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      detail = (await res.json()).detail || detail;
    } catch {
      /* non-JSON body */
    }
    throw new Error(detail);
  }
  return res.status === 204 ? null : res.json();
}

export const api = {
  createRoom: () => request('/api/rooms', { method: 'POST' }),
  joinRoom: (code, displayName) =>
    request(`/api/rooms/${code}/players`, {
      method: 'POST',
      body: JSON.stringify({ displayName }),
    }),
  getRoom: (code) => request(`/api/rooms/${code}`),
  getGame: (code) => request(`/api/rooms/${code}/game`),
  getHand: (code, playerId) => request(`/api/rooms/${code}/players/${playerId}/hand`),
};
