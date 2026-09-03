import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { STATS } from '../constants.js';
import HeroCard from './HeroCard.jsx';
import Chat from './Chat.jsx';

const statLabel = (key) => STATS.find((s) => s.key === key)?.label ?? key;

export default function Battle({ session, view, error, onPick, onReveal, onLeave, chat }) {
  const [hand, setHand] = useState(null);

  useEffect(() => {
    let alive = true;
    api
      .getHand(session.code, session.playerId)
      .then((h) => alive && setHand(h))
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [session.code, session.playerId, view]);

  const round = view.round ?? {};
  const me = session.playerId;
  const isActive = view.activePlayerId === me;
  const isContender = (round.contenders ?? []).includes(me);
  const hasResponded = (round.responded ?? []).includes(me);
  const opponents = view.players.filter((p) => p.playerId !== me);
  const nameOf = (id) => view.players.find((p) => p.playerId === id)?.displayName ?? id;
  const reveals = round.reveals ?? {};
  const flipShown = Object.keys(reveals).length > 0;

  return (
    <div className="battle">
      <header className="battle__bar">
        <button className="link-btn" onClick={onLeave}>
          ← leave
        </button>
        <span>
          Round {view.roundNumber} / {view.roundCap}
        </span>
        {round.showdownDepth > 0 && <span className="badge badge--hot">SHOWDOWN ×{round.showdownDepth}</span>}
      </header>

      <section className="opponents">
        {opponents.map((p) => {
          const responded = (round.responded ?? []).includes(p.playerId);
          const oppCard = flipShown ? lastCard(reveals[p.playerId]) : null;
          return (
            <div
              key={p.playerId}
              className={`opponent${p.eliminated ? ' is-out' : ''}${
                view.activePlayerId === p.playerId ? ' is-active' : ''
              }`}
            >
              <div className="opponent__name">
                {p.displayName}
                {view.activePlayerId === p.playerId && <span className="badge">picking</span>}
              </div>
              <HeroCard card={oppCard} size="sm" highlight={round.stat} />
              <div className="opponent__meta">
                <span>{p.deckCount} cards</span>
                {round.phase === 'REVEALING' && !flipShown && (
                  <span className={responded ? 'ok' : 'muted'}>
                    {responded ? '✓ revealed' : '…'}
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </section>

      <section className="stage">
        <StatusLine
          view={view}
          round={round}
          isActive={isActive}
          isContender={isContender}
          hasResponded={hasResponded}
          nameOf={nameOf}
        />
        {view.resolution && (
          <div className="resolution">
            <strong>{nameOf(view.resolution.roundWinnerId)}</strong> takes the pot on{' '}
            {statLabel(view.resolution.stat)}
            <div className="resolution__values">
              {Object.entries(view.resolution.values).map(([id, v]) => (
                <span key={id}>
                  {nameOf(id)} <b>{v}</b>
                </span>
              ))}
            </div>
          </div>
        )}
        {error && <div className="alert">{error}</div>}
      </section>

      <section className="myhand">
        <div className="myhand__label">
          Your hand · {hand?.deckCount ?? view.players.find((p) => p.playerId === me)?.deckCount ?? 0} cards
        </div>
        <HeroCard
          card={hand?.topCard}
          size="lg"
          highlight={round.stat}
          pickable={isActive && round.phase === 'PICKING'}
          onPick={onPick}
        />
        {isContender && round.phase === 'REVEALING' && !hasResponded && (
          <button className="btn btn--primary" onClick={onReveal}>
            Reveal card
          </button>
        )}
        {isContender && round.phase === 'REVEALING' && hasResponded && !flipShown && (
          <p className="hint">Card locked in — waiting for the others…</p>
        )}
      </section>

      {chat && <Chat {...chat} />}

      {view.phase === 'FINISHED' && (
        <div className="overlay">
          <div className="overlay__box">
            <h2>
              {view.matchWinnerId === me ? 'You win! 🏆' : `${nameOf(view.matchWinnerId)} wins`}
            </h2>
            <button className="btn btn--primary" onClick={onLeave}>
              Back to home
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function StatusLine({ view, round, isActive, isContender, hasResponded, nameOf }) {
  if (view.phase === 'FINISHED') return <p className="status">Match over.</p>;
  if (round.phase === 'PICKING') {
    return (
      <p className="status">
        {isActive
          ? 'Your turn — pick a stat from your top card.'
          : `Waiting for ${nameOf(view.activePlayerId)} to pick a stat…`}
      </p>
    );
  }
  if (round.phase === 'REVEALING') {
    return (
      <p className="status">
        {nameOf(view.activePlayerId)} chose <b>{statLabel(round.stat)}</b>.{' '}
        {isContender
          ? hasResponded
            ? 'Waiting on the other players…'
            : 'Reveal your card!'
          : 'Spectating this round.'}
      </p>
    );
  }
  return <p className="status">Resolving…</p>;
}

function lastCard(cards) {
  return Array.isArray(cards) && cards.length ? cards[cards.length - 1] : null;
}
