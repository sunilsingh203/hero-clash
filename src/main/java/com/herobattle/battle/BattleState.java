package com.herobattle.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full mutable state of one Battle Mode v2 match. Serialized to Redis between operations so
 * any backend instance can advance any battle (mirrors {@code GameState} for classic mode).
 */
public class BattleState {

    public enum Phase { RUNNING, FINISHED }

    private String roomCode;
    private Phase phase = Phase.RUNNING;

    /** 1-based; a round is one full pass through {@link #turnOrder} (see {@code roundCap}). */
    private int roundNumber = 1;
    private int roundCap = 25;

    /** Total attacks resolved; drives {@link #getRoundNumber()} and the round cap. */
    private int turnsTaken = 0;

    /** Player ids in initiative order (highest power+combat+speed first). Fixed for the match. */
    private List<String> turnOrder = new ArrayList<>();

    /** Index into {@link #turnOrder} of the player whose turn it is. */
    private int turnIndex = 0;

    /** Player id → champion. Insertion order is the room's original seating order. */
    private Map<String, Combatant> combatants = new LinkedHashMap<>();

    private Set<String> eliminated = new LinkedHashSet<>();

    private String matchWinnerId;

    /** What the most recent attack did, for the broadcast. Null before the first attack. */
    private AttackResult lastAttack;

    // ---- derived helpers (ignored by Jackson) ----

    public String activePlayerId() {
        return turnOrder.isEmpty() ? null : turnOrder.get(turnIndex);
    }

    public List<String> alivePlayers() {
        List<String> alive = new ArrayList<>();
        for (String id : turnOrder) {
            if (!eliminated.contains(id)) {
                alive.add(id);
            }
        }
        return alive;
    }

    public Combatant combatantOf(String playerId) {
        return combatants.get(playerId);
    }

    /** Advance {@link #turnIndex} to the next living player, counting the turn just taken. */
    public void advanceTurn() {
        turnsTaken++;
        for (int step = 0; step < turnOrder.size(); step++) {
            turnIndex = (turnIndex + 1) % turnOrder.size();
            if (!eliminated.contains(turnOrder.get(turnIndex))) {
                return;
            }
        }
    }

    // ---- accessors ----

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getRoundNumber() {
        return turnOrder.isEmpty() ? roundNumber : (turnsTaken / turnOrder.size()) + 1;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getRoundCap() {
        return roundCap;
    }

    public void setRoundCap(int roundCap) {
        this.roundCap = roundCap;
    }

    public int getTurnsTaken() {
        return turnsTaken;
    }

    public void setTurnsTaken(int turnsTaken) {
        this.turnsTaken = turnsTaken;
    }

    public List<String> getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(List<String> turnOrder) {
        this.turnOrder = turnOrder;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public void setTurnIndex(int turnIndex) {
        this.turnIndex = turnIndex;
    }

    public Map<String, Combatant> getCombatants() {
        return combatants;
    }

    public void setCombatants(Map<String, Combatant> combatants) {
        this.combatants = combatants;
    }

    public Set<String> getEliminated() {
        return eliminated;
    }

    public void setEliminated(Set<String> eliminated) {
        this.eliminated = eliminated;
    }

    public String getMatchWinnerId() {
        return matchWinnerId;
    }

    public void setMatchWinnerId(String matchWinnerId) {
        this.matchWinnerId = matchWinnerId;
    }

    public AttackResult getLastAttack() {
        return lastAttack;
    }

    public void setLastAttack(AttackResult lastAttack) {
        this.lastAttack = lastAttack;
    }
}
