import { useCallback, useEffect, useState } from 'react';
import Home from './components/Home.jsx';
import Lobby from './components/Lobby.jsx';
import Battle from './components/Battle.jsx';
import { useGameSocket } from './useGameSocket.js';

const STORAGE_KEY = 'heroclash.session';

function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) || null;
  } catch {
    return null;
  }
}

export default function App() {
  const [session, setSession] = useState(loadSession);
  const { connected, view, error, messages, start, pick, reveal, sendChat } = useGameSocket(
    session?.code,
  );

  useEffect(() => {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  }, [session]);

  const leave = useCallback(() => setSession(null), []);

  const chat = session
    ? { messages, me: session.playerId, onSend: (text) => sendChat(session.playerId, text) }
    : null;

  let screen;
  if (!session) {
    screen = <Home onJoined={setSession} />;
  } else if (!view) {
    screen = (
      <Lobby
        session={session}
        connected={connected}
        onStart={start}
        onLeave={leave}
        chat={chat}
      />
    );
  } else {
    screen = (
      <Battle
        session={session}
        view={view}
        error={error}
        onPick={(stat) => pick(session.playerId, stat)}
        onReveal={() => reveal(session.playerId)}
        onLeave={leave}
        chat={chat}
      />
    );
  }

  return <div className="app">{screen}</div>;
}
