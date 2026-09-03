import { useEffect, useRef, useState } from 'react';

const MAX_LENGTH = 280;

const timeOf = (ms) =>
  new Date(ms).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

export default function Chat({ messages, me, onSend }) {
  const [text, setText] = useState('');
  const logRef = useRef(null);

  useEffect(() => {
    const el = logRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [messages]);

  const submit = (e) => {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed.slice(0, MAX_LENGTH));
    setText('');
  };

  return (
    <section className="chat">
      <div className="chat__log" ref={logRef}>
        {messages.length === 0 && <p className="chat__empty">No messages yet. Say hi 👋</p>}
        {messages.map((m) => (
          <div key={m.id} className={`chat__msg${m.playerId === me ? ' is-me' : ''}`}>
            <span className="chat__author">{m.displayName}</span>
            <span className="chat__time">{timeOf(m.sentAt)}</span>
            <div className="chat__text">{m.text}</div>
          </div>
        ))}
      </div>
      <form className="chat__form" onSubmit={submit}>
        <input
          value={text}
          maxLength={MAX_LENGTH}
          placeholder="Message the room…"
          onChange={(e) => setText(e.target.value)}
        />
        <button className="btn" type="submit" disabled={!text.trim()}>
          Send
        </button>
      </form>
    </section>
  );
}
