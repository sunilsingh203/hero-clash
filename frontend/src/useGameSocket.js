import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Opens one STOMP-over-SockJS connection for a room and keeps the latest GameView.
 * Returns the connection state, the last view, the last error, and action senders.
 */
export function useGameSocket(roomCode) {
  const [connected, setConnected] = useState(false);
  const [view, setView] = useState(null);
  const [error, setError] = useState(null);
  const clientRef = useRef(null);

  useEffect(() => {
    if (!roomCode) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 2000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/rooms.${roomCode}`, (msg) => {
          setError(null);
          setView(JSON.parse(msg.body));
        });
        client.subscribe(`/topic/rooms.${roomCode}.errors`, (msg) => {
          setError(JSON.parse(msg.body).message);
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;
    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [roomCode]);

  const send = (suffix, body) => {
    const client = clientRef.current;
    if (!client || !client.connected) return;
    client.publish({
      destination: `/app/rooms/${roomCode}/${suffix}`,
      body: JSON.stringify(body ?? {}),
    });
  };

  return {
    connected,
    view,
    error,
    start: () => send('start'),
    pick: (playerId, stat) => send('pick', { playerId, stat }),
    reveal: (playerId) => send('reveal', { playerId }),
  };
}
