package com.herobattle.battle;

/**
 * What one attack did. Carried on {@link BattleState#getLastAttack()} so the broadcast can
 * describe the blow even though the state has already advanced to the next turn.
 *
 * @param attackerId    who attacked
 * @param targetId      who was hit
 * @param damage        HP actually removed (after mitigation, clamped to the target's HP)
 * @param targetHpLeft  target's HP after the hit
 * @param targetDown    true if this hit eliminated the target
 * @param matchOver     true if this hit ended the match
 * @param matchWinnerId overall winner (only when {@code matchOver})
 */
public record AttackResult(
        String attackerId,
        String targetId,
        int damage,
        int targetHpLeft,
        boolean targetDown,
        boolean matchOver,
        String matchWinnerId) {
}
