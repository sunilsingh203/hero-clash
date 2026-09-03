import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { api } from './api.js';

/**
 * Opens one STOMP-over-SockJS connection for a room and keeps the latest GameView plus the
 * running chat log. Returns the connection state, the last view, the last error, the chat
 * messages, and action senders.
 */
export function useGameSocket(roomCode) {
  const [connected, setConnected] = useState(false);
  const [view, setView] = useState(null);
  const [error, setError] = useState(null);
  const [messages, setMessages] = useState([]);
  const clientRef = useRef(null);

  const mergeMessages = (incoming) =>
    setMessages((prev) => {
      const seen = new Set(prev.map((m) => m.id));
      const added = (Array.isArray(incoming) ? incoming : [incoming]).filter((m) => !seen.has(m.id));
      if (added.length === 0) return prev;
      return [...prev, ...added].sort((a, b) => a.sentAt - b.sentAt);
    });

  useEffect(() => {
    if (!roomCode) return undefined;
    setMessages([]);

    // Seed the chat backlog for a client that just (re)connected.
    api.getChat(roomCode).then(mergeMessages).catch(() => {});

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
        client.subscribe(`/topic/rooms.${roomCode}.chat`, (msg) => {
          mergeMessages(JSON.parse(msg.body));
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
    messages,
    start: () => send('start'),
    pick: (playerId, stat) => send('pick', { playerId, stat }),
    reveal: (playerId) => send('reveal', { playerId }),
    attack: (playerId, targetId) => send('battle/attack', { playerId, targetId }),
    startBattle: () => send('battle/start'),
    sendChat: (playerId, text) => send('chat', { playerId, text }),
  };
}
