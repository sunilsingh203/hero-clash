package com.herobattle.game;

import com.herobattle.model.Card;

/**
 * Lightweight, serializable snapshot of a {@link Card} used inside a running game so the
 * engine never needs to touch the database mid-match. Stat accessors coerce {@code null}
 * to 0.
 */
public record GameCard(
        long id,
        String name,
        String imageUrl,
        Integer intelligence,
        Integer strength,
        Integer speed,
        Integer durability,
        Integer power,
        Integer combat) {

    public static GameCard from(Card c) {
        return new GameCard(c.getId(), c.getName(), c.getImageUrl(),
                c.getIntelligence(), c.getStrength(), c.getSpeed(),
                c.getDurability(), c.getPower(), c.getCombat());
    }

}
