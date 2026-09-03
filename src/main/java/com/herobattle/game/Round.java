package com.herobattle.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable state for the round currently in play.
 *
 * <p>Lifecycle: {@code PICKING} (waiting for the active player to choose a stat) →
 * {@code REVEALING} (each contender's top card has been pulled into {@link #plays};
 * waiting for every contender to confirm the flip) → {@code RESOLVED}.
 */
public class Round {

    public enum Phase { PICKING, REVEALING, RESOLVED }

    private Phase phase = Phase.PICKING;
    private Stat stat;

    /** Player id → cards that player has committed this round (grows during showdowns). */
    private Map<String, List<GameCard>> plays = new LinkedHashMap<>();

    /** Contenders still competing for the pot (shrinks as players bust out of a showdown). */
    private List<String> contenders = new ArrayList<>();

    /** Contenders who have confirmed their reveal for the current flip. */
    private Set<String> responded = new LinkedHashSet<>();

    /** Number of flips resolved so far (0 = first reveal, >0 = showdown rounds). */
    private int showdownDepth = 0;

    /** Stat value each contender showed on the most recent flip (for the reveal broadcast). */
    private Map<String, Integer> lastRevealValues = new LinkedHashMap<>();

    private String winnerId;

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }

    public Map<String, List<GameCard>> getPlays() {
        return plays;
    }

    public void setPlays(Map<String, List<GameCard>> plays) {
        this.plays = plays;
    }

    public List<String> getContenders() {
        return contenders;
    }

    public void setContenders(List<String> contenders) {
        this.contenders = contenders;
    }

    public Set<String> getResponded() {
        return responded;
    }

    public void setResponded(Set<String> responded) {
        this.responded = responded;
    }

    public int getShowdownDepth() {
        return showdownDepth;
    }

    public void setShowdownDepth(int showdownDepth) {
        this.showdownDepth = showdownDepth;
    }

    public Map<String, Integer> getLastRevealValues() {
        return lastRevealValues;
    }

    public void setLastRevealValues(Map<String, Integer> lastRevealValues) {
        this.lastRevealValues = lastRevealValues;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    /** Every card committed this round, across all contenders and showdown flips. */
    public List<GameCard> pot() {
        List<GameCard> pot = new ArrayList<>();
        plays.values().forEach(pot::addAll);
        return pot;
    }
}
