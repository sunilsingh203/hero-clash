package com.herobattle.game;

import java.util.function.Function;

/** The six comparable powerstats a round can be played on. */
public enum Stat {
    INTELLIGENCE(GameCard::intelligence),
    STRENGTH(GameCard::strength),
    SPEED(GameCard::speed),
    DURABILITY(GameCard::durability),
    POWER(GameCard::power),
    COMBAT(GameCard::combat);

    private final Function<GameCard, Integer> accessor;

    Stat(Function<GameCard, Integer> accessor) {
        this.accessor = accessor;
    }

    /** Value of this stat for the given card; a missing (null) stat counts as 0. */
    public int valueOf(GameCard card) {
        Integer v = accessor.apply(card);
        return v == null ? 0 : v;
    }
}
