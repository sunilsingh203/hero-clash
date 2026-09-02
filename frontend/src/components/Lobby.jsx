import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function Lobby({ session, connected, onStart, onLeave }) {
  const [room, setRoom] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    const poll = async () => {
      try {
        const r = await api.getRoom(session.code);
        if (alive) setRoom(r);
      } catch (e) {
        if (alive) setError(e.message);
      }
    };
    poll();
    const id = setInterval(poll, 2000);
    return () => {
      alive = false;
      clearInterval(id);
    };
  }, [session.code]);

  const players = room?.players ?? [];
  const canStart = connected && players.length >= 2;

  return (
    <div className="panel lobby">
      <button className="link-btn" onClick={onLeave}>
        ← leave
      </button>
      <h2>Lobby</h2>
      <div className="roomcode">
        <span>Room code</span>
        <strong>{session.code}</strong>
        <button
          className="link-btn"
          onClick={() => navigator.clipboard?.writeText(session.code)}
        >
          copy
        </button>
      </div>

      <ul className="players">
        {players.map((p) => (
          <li key={p.id} className={String(p.id) === session.playerId ? 'is-you' : ''}>
            <span className="dot" />
            {p.displayName}
            {String(p.id) === session.playerId && <em> (you)</em>}
          </li>
        ))}
        {Array.from({ length: Math.max(0, 4 - players.length) }).map((_, i) => (
          <li key={`empty-${i}`} className="empty">
            waiting for player…
          </li>
        ))}
      </ul>

      <button className="btn btn--primary" disabled={!canStart} onClick={onStart}>
        {players.length < 2 ? 'Need 2+ players' : 'Start match'}
      </button>
      <p className="hint">
        {connected ? 'Connected. Anyone in the room can start.' : 'Connecting…'}
      </p>
      {error && <div className="alert">{error}</div>}
    </div>
  );
}
