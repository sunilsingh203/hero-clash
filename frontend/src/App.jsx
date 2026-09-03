import { useCallback, useEffect, useState } from 'react';
import Home from './components/Home.jsx';
import Lobby from './components/Lobby.jsx';
import Battle from './components/Battle.jsx';
import BattleArena from './components/BattleArena.jsx';
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
  const { connected, view, error, messages, start, startBattle, pick, reveal, attack, sendChat } =
    useGameSocket(session?.code);

  useEffect(() => {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    else localStorage.removeItem(STORAGE_KEY);
  }, [session]);

  const leave = useCallback(() => setSession(null), []);

  const isBattle = session?.mode === 'BATTLE';
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
        onStart={isBattle ? startBattle : start}
        onLeave={leave}
        chat={chat}
      />
    );
  } else if (view.kind === 'BATTLE') {
    screen = (
      <BattleArena
        session={session}
        view={view}
        error={error}
        onAttack={(targetId) => attack(session.playerId, targetId)}
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
