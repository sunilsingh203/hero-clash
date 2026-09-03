import HeroCard from './HeroCard.jsx';
import Chat from './Chat.jsx';

/**
 * Battle Mode v2 screen. Each player has one champion with an HP pool; on your turn you
 * click an opponent's card to attack it. Turn order and damage are decided server-side.
 */
export default function BattleArena({ session, view, error, onAttack, onLeave, chat }) {
  const me = session.playerId;
  const myTurn = view.activePlayerId === me && view.phase === 'RUNNING';
  const nameOf = (id) => view.combatants.find((c) => c.playerId === id)?.displayName ?? id;

  const mine = view.combatants.find((c) => c.playerId === me);
  const opponents = view.combatants.filter((c) => c.playerId !== me);
  const atk = view.lastAttack;

  return (
    <div className="battle arena">
      <header className="battle__bar">
        <button className="link-btn" onClick={onLeave}>
          ← leave
        </button>
        <span>
          Round {view.roundNumber} / {view.roundCap}
        </span>
        <span className="badge">Battle v2</span>
      </header>

      <section className="opponents">
        {opponents.map((c) => (
          <Fighter
            key={c.playerId}
            c={c}
            active={view.activePlayerId === c.playerId}
            targetable={myTurn && !c.eliminated}
            onAttack={() => onAttack(c.playerId)}
          />
        ))}
      </section>

      <section className="stage">
        <p className="status">
          {view.phase === 'FINISHED'
            ? 'Match over.'
            : myTurn
              ? 'Your turn — click an opponent to attack.'
              : `Waiting for ${nameOf(view.activePlayerId)} to attack…`}
        </p>
        {atk && (
          <div className="resolution">
            <strong>{nameOf(atk.attackerId)}</strong> hit <strong>{nameOf(atk.targetId)}</strong> for{' '}
            <b>{atk.damage}</b>
            {atk.targetDown ? ' — down!' : ` (${atk.targetHpLeft} HP left)`}
          </div>
        )}
        {error && <div className="alert">{error}</div>}
      </section>

      <section className="myhand">
        <div className="myhand__label">Your champion</div>
        {mine && <Fighter c={mine} active={myTurn} big />}
      </section>

      {chat && <Chat {...chat} />}

      {view.phase === 'FINISHED' && (
        <div className="overlay">
          <div className="overlay__box">
            <h2>{view.matchWinnerId === me ? 'You win! 🏆' : `${nameOf(view.matchWinnerId)} wins`}</h2>
            <button className="btn btn--primary" onClick={onLeave}>
              Back to home
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function Fighter({ c, active, targetable, big, onAttack }) {
  const pct = Math.max(0, Math.round((c.currentHp / c.maxHp) * 100));
  const Wrap = targetable ? 'button' : 'div';
  return (
    <Wrap
      type={targetable ? 'button' : undefined}
      onClick={targetable ? onAttack : undefined}
      className={`fighter${big ? ' fighter--big' : ''}${c.eliminated ? ' is-out' : ''}${
        active ? ' is-active' : ''
      }${targetable ? ' is-targetable' : ''}`}
    >
      <div className="fighter__name">
        {c.displayName}
        {active && <span className="badge">turn</span>}
        {c.eliminated && <span className="badge badge--hot">down</span>}
      </div>
      <HeroCard card={c.card} size={big ? 'lg' : 'sm'} />
      <div className="hpbar">
        <span className="hpbar__fill" style={{ width: `${pct}%` }} />
      </div>
      <div className="fighter__hp">
        {c.currentHp} / {c.maxHp} HP
      </div>
      {targetable && <div className="fighter__cta">Attack</div>}
    </Wrap>
  );
}
