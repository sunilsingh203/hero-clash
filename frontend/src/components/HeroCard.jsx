import { STATS } from '../constants.js';

/**
 * One hero card. Pass `card = null` to render it face-down. When `pickable` is set, each
 * stat row becomes a button that calls `onPick(statKey)`.
 */
export default function HeroCard({ card, highlight, pickable, onPick, size = 'md' }) {
  if (!card) {
    return (
      <div className={`hero-card facedown ${size}`}>
        <div className="facedown-crest">HC</div>
      </div>
    );
  }

  return (
    <div className={`hero-card ${size}`}>
      <div className="hero-card__name">{card.name}</div>
      <div
        className="hero-card__art"
        style={card.imageUrl ? { backgroundImage: `url(${card.imageUrl})` } : undefined}
      >
        {!card.imageUrl && <span>{initials(card.name)}</span>}
      </div>
      <div className="hero-card__stats">
        {STATS.map((stat) => {
          const value = card[stat.field] ?? 0;
          const active = highlight === stat.key;
          const Row = pickable ? 'button' : 'div';
          return (
            <Row
              key={stat.key}
              className={`stat-row${active ? ' is-highlight' : ''}`}
              onClick={pickable ? () => onPick(stat.key) : undefined}
              type={pickable ? 'button' : undefined}
            >
              <span className="stat-row__label">{stat.label}</span>
              <span className="stat-row__bar">
                <span className="stat-row__fill" style={{ width: `${value}%` }} />
              </span>
              <span className="stat-row__value">{value}</span>
            </Row>
          );
        })}
      </div>
    </div>
  );
}

function initials(name) {
  return name
    .split(/\s+/)
    .slice(0, 2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
}
