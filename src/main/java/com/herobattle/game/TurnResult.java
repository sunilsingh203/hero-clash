package com.herobattle.game;

import java.util.List;
import java.util.Map;

/**
 * Outcome of a {@link GameEngine#submitReveal} call.
 *
 * @param allRevealed    true once every contender has confirmed this flip
 * @param revealValues   stat value each contender showed on this flip (only when {@code allRevealed})
 * @param showdown       true if the flip tied and another showdown flip is now required
 * @param roundResolved  true if the round is finished and the pot has been awarded
 * @param roundWinnerId  winner of the pot (only when {@code roundResolved})
 * @param matchOver      true if this round ended the match
 * @param matchWinnerId  overall winner (only when {@code matchOver})
 * @param eliminated     players knocked out by this round's resolution
 */
public record TurnResult(
        boolean allRevealed,
        Map<String, Integer> revealValues,
        boolean showdown,
        boolean roundResolved,
        String roundWinnerId,
        boolean matchOver,
        String matchWinnerId,
        List<String> eliminated) {

    static TurnResult waiting() {
        return new TurnResult(false, Map.of(), false, false, null, false, null, List.of());
    }
}
