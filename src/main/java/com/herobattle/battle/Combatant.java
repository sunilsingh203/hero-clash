package com.herobattle.battle;

import com.herobattle.game.GameCard;

/**
 * One player's champion in Battle Mode v2. HP is derived from the hero's durability and
 * strength at deal time; {@link #initiative()} (power + combat + speed) fixes turn order.
 */
public class Combatant {

    private String playerId;
    private GameCard card;
    private int maxHp;
    private int currentHp;

    public Combatant() {
    }

    public Combatant(String playerId, GameCard card) {
        this.playerId = playerId;
        this.card = card;
        this.maxHp = Math.max(1, nz(card.durability()) + nz(card.strength()));
        this.currentHp = this.maxHp;
    }

    /** Damage a straight-up attack from this combatant deals before the target's mitigation. */
    public int attackPower() {
        return (nz(card.strength()) + nz(card.power())) / 2;
    }

    /** Flat damage reduction this combatant applies when defending. */
    public int mitigation() {
        return nz(card.durability()) / 4;
    }

    /** Turn-order score: higher goes first. */
    public int initiative() {
        return nz(card.power()) + nz(card.combat()) + nz(card.speed());
    }

    public boolean alive() {
        return currentHp > 0;
    }

    /** Applies {@code amount} damage, clamping at 0. Returns the damage actually dealt. */
    public int takeHit(int amount) {
        int dealt = Math.min(amount, currentHp);
        currentHp -= dealt;
        return dealt;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    // ---- accessors (also used by Jackson) ----

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public GameCard getCard() {
        return card;
    }

    public void setCard(GameCard card) {
        this.card = card;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }
}
