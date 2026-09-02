import { useState } from 'react';
import { api } from '../api.js';

export default function Home({ onJoined }) {
  const [name, setName] = useState('');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const join = async (roomCode) => {
    setBusy(true);
    setError(null);
    try {
      const res = await api.joinRoom(roomCode, name.trim());
      onJoined({ code: res.room.code, playerId: String(res.you.id), displayName: res.you.displayName });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  };

  const createAndJoin = async () => {
    setBusy(true);
    setError(null);
    try {
      const room = await api.createRoom();
      await join(room.code);
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  };

  const nameOk = name.trim().length > 0;

  return (
    <div className="panel home">
      <h1 className="logo">
        HERO<span>CLASH</span>
      </h1>
      <p className="tagline">Stat-battle card duels. Highest number takes the pot.</p>

      <label className="field">
        <span>Your name</span>
        <input
          value={name}
          maxLength={30}
          placeholder="e.g. Nova"
          onChange={(e) => setName(e.target.value)}
        />
      </label>

      <div className="home__actions">
        <button className="btn btn--primary" disabled={!nameOk || busy} onClick={createAndJoin}>
          Create a room
        </button>

        <div className="or">or</div>

        <div className="join-row">
          <input
            value={code}
            placeholder="ROOM CODE"
            maxLength={6}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
          />
          <button
            className="btn"
            disabled={!nameOk || busy || code.length < 4}
            onClick={() => join(code)}
          >
            Join
          </button>
        </div>
      </div>

      {error && <div className="alert">{error}</div>}
    </div>
  );
}
