package com.herobattle.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full mutable state of one match. Serialized to Redis between operations so any backend
 * instance can advance any game.
 */
public class GameState {

    public enum Phase { RUNNING, FINISHED }

    private String roomCode;
    private Phase phase = Phase.RUNNING;
    private int roundNumber = 1;
    private int roundCap = 25;
    private String activePlayerId;

    /** Seating order; also the tie-break order for showdown pot assignment. */
    private List<String> playerOrder = new ArrayList<>();

    /** Player id → their deck. Index 0 / head = top card (next to be played). */
    private Map<String, Deque<GameCard>> decks = new LinkedHashMap<>();

    private Set<String> eliminated = new LinkedHashSet<>();

    private Round currentRound = new Round();

    private String matchWinnerId;

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
        return roundNumber;
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

    public String getActivePlayerId() {
        return activePlayerId;
    }

    public void setActivePlayerId(String activePlayerId) {
        this.activePlayerId = activePlayerId;
    }

    public List<String> getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(List<String> playerOrder) {
        this.playerOrder = playerOrder;
    }

    public Map<String, Deque<GameCard>> getDecks() {
        return decks;
    }

    public void setDecks(Map<String, Deque<GameCard>> decks) {
        this.decks = decks;
    }

    public Set<String> getEliminated() {
        return eliminated;
    }

    public void setEliminated(Set<String> eliminated) {
        this.eliminated = eliminated;
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(Round currentRound) {
        this.currentRound = currentRound;
    }

    public String getMatchWinnerId() {
        return matchWinnerId;
    }

    public void setMatchWinnerId(String matchWinnerId) {
        this.matchWinnerId = matchWinnerId;
    }

    // ---- convenience (ignored for JSON; derived) ----

    public int deckSize(String playerId) {
        Deque<GameCard> d = decks.get(playerId);
        return d == null ? 0 : d.size();
    }

    public List<String> activePlayers() {
        List<String> alive = new ArrayList<>();
        for (String p : playerOrder) {
            if (!eliminated.contains(p)) {
                alive.add(p);
            }
        }
        return alive;
    }

    public Deque<GameCard> deckOf(String playerId) {
        return decks.computeIfAbsent(playerId, k -> new ArrayDeque<>());
    }
}
