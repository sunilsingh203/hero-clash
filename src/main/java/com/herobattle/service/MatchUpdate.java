package com.herobattle.service;

import com.herobattle.game.GameState;
import com.herobattle.game.TurnResult;

/**
 * Result of a single match action: the new {@link GameState} plus, for a reveal, the
 * {@link TurnResult} describing what that flip decided (the engine has already advanced to
 * the next round by the time the state is broadcast, so the outcome must be carried
 * alongside it).
 */
public record MatchUpdate(GameState state, TurnResult result) {

    static MatchUpdate of(GameState state) {
        return new MatchUpdate(state, null);
    }
}
