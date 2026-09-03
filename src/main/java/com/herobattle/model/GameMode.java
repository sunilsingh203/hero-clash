package com.herobattle.model;

/** How a room's match plays out. Chosen at room creation, fixed for the room's life. */
public enum GameMode {

    /** Top-trumps: pick a stat, highest value takes the pot. See {@code CLAUDE.md} §"Game Rules". */
    CLASSIC,

    /** Battle Mode v2: one champion hero each, HP = durability + strength, attack on your turn. */
    BATTLE
}
