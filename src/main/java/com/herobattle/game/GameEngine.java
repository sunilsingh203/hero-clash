package com.herobattle.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * Pure rules engine for "Hero Clash". Holds no state of its own — every method takes a
 * {@link GameState}, mutates it in place, and returns. See {@code CLAUDE.md} §"Game Rules".
 */
@Component
public class GameEngine {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;

    private final Random random;

    public GameEngine() {
        this(new Random());
    }

    /** Test seam: inject a seeded {@link Random} for deterministic deals. */
    public GameEngine(Random random) {
        this.random = random;
    }

    // ------------------------------------------------------------------ start

    public GameState startMatch(String roomCode, List<String> playerIds,
                                List<GameCard> pool, int roundCap) {
        if (playerIds.size() < MIN_PLAYERS || playerIds.size() > MAX_PLAYERS) {
            throw new GameException("A match needs 2–4 players, got " + playerIds.size());
        }
        if (pool.size() < playerIds.size()) {
            throw new GameException("Not enough cards to deal");
        }
        GameState s = new GameState();
        s.setRoomCode(roomCode);
        s.setRoundCap(roundCap);
        s.setPlayerOrder(new ArrayList<>(playerIds));
        s.setActivePlayerId(playerIds.get(0));

        List<GameCard> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, random);
        for (String p : playerIds) {
            s.getDecks().put(p, new java.util.ArrayDeque<>());
        }
        int i = 0;
        for (GameCard card : shuffled) {
            String owner = playerIds.get(i % playerIds.size());
            s.deckOf(owner).addLast(card);
            i++;
        }
        // keep hands equal: trim to the smallest hand size
        int min = playerIds.stream().mapToInt(s::deckSize).min().orElse(0);
        for (String p : playerIds) {
            while (s.deckSize(p) > min) {
                s.deckOf(p).removeLast();
            }
        }
        s.setCurrentRound(freshRound());
        return s;
    }

    // ------------------------------------------------------------------ pick

    public void pickCategory(GameState s, String playerId, Stat stat) {
        requireRunning(s);
        Round r = s.getCurrentRound();
        if (r.getPhase() != Round.Phase.PICKING) {
            throw new GameException("Not in the pick phase");
        }
        if (!playerId.equals(s.getActivePlayerId())) {
            throw new GameException("Only the active player picks the category");
        }
        if (stat == null) {
            throw new GameException("Stat is required");
        }
        r.setStat(stat);
        r.setContenders(new ArrayList<>(s.activePlayers()));
        r.getPlays().clear();
        for (String p : r.getContenders()) {
            GameCard top = s.deckOf(p).pollFirst();
            if (top == null) {
                throw new GameException("Contender " + p + " has no cards");
            }
            List<GameCard> committed = new ArrayList<>();
            committed.add(top);
            r.getPlays().put(p, committed);
        }
        r.getResponded().clear();
        r.getLastRevealValues().clear();
        r.setShowdownDepth(0);
        r.setPhase(Round.Phase.REVEALING);
    }

    // ------------------------------------------------------------------ reveal

    public TurnResult submitReveal(GameState s, String playerId) {
        requireRunning(s);
        Round r = s.getCurrentRound();
        if (r.getPhase() != Round.Phase.REVEALING) {
            throw new GameException("Not in the reveal phase");
        }
        if (!r.getContenders().contains(playerId)) {
            throw new GameException("Player " + playerId + " is not a contender this round");
        }
        r.getResponded().add(playerId);
        if (!r.getResponded().containsAll(r.getContenders())) {
            return TurnResult.waiting();
        }
        return resolveFlip(s, r);
    }

    private TurnResult resolveFlip(GameState s, Round r) {
        Stat stat = r.getStat();
        Map<String, Integer> values = new LinkedHashMap<>();
        int best = Integer.MIN_VALUE;
        for (String p : r.getContenders()) {
            List<GameCard> committed = r.getPlays().get(p);
            int v = stat.valueOf(committed.get(committed.size() - 1));
            values.put(p, v);
            best = Math.max(best, v);
        }
        r.setLastRevealValues(values);

        final int top = best;
        List<String> leaders = r.getContenders().stream()
                .filter(p -> values.get(p) == top)
                .toList();

        if (leaders.size() == 1) {
            return award(s, r, leaders.get(0), values);
        }

        // tie → showdown among the leaders
        List<String> nextContenders = new ArrayList<>();
        for (String p : leaders) {
            GameCard next = s.deckOf(p).pollFirst();
            if (next != null) {
                r.getPlays().get(p).add(next);
                nextContenders.add(p);
            }
        }
        if (nextContenders.size() == 1) {
            return award(s, r, nextContenders.get(0), values);
        }
        if (nextContenders.isEmpty()) {
            // nobody could keep flipping: earliest-seated tied player takes the pot
            String fallback = orderedFirst(s, leaders);
            return award(s, r, fallback, values);
        }
        r.setContenders(nextContenders);
        r.getResponded().clear();
        r.setShowdownDepth(r.getShowdownDepth() + 1);
        return new TurnResult(true, values, true, false, null, false, null, List.of(), r.getStat());
    }

    private TurnResult award(GameState s, Round r, String winnerId, Map<String, Integer> values) {
        List<GameCard> pot = new ArrayList<>();
        // winner's own cards first, then the rest in seating order — deterministic
        r.getPlays().getOrDefault(winnerId, List.of()).forEach(pot::add);
        for (String p : s.getPlayerOrder()) {
            if (!p.equals(winnerId) && r.getPlays().containsKey(p)) {
                pot.addAll(r.getPlays().get(p));
            }
        }
        pot.forEach(s.deckOf(winnerId)::addLast);
        r.setWinnerId(winnerId);
        r.setPhase(Round.Phase.RESOLVED);
        s.setActivePlayerId(winnerId);

        List<String> newlyEliminated = new ArrayList<>();
        for (String p : s.getPlayerOrder()) {
            if (!s.getEliminated().contains(p) && s.deckSize(p) == 0) {
                s.getEliminated().add(p);
                newlyEliminated.add(p);
            }
        }

        List<String> alive = s.activePlayers();
        boolean matchOver = false;
        String matchWinner = null;
        if (alive.size() <= 1) {
            matchOver = true;
            matchWinner = alive.isEmpty() ? winnerId : alive.get(0);
        } else if (s.getRoundNumber() >= s.getRoundCap()) {
            matchOver = true;
            matchWinner = alive.stream()
                    .max(Comparator.<String>comparingInt(s::deckSize)
                            .thenComparing(p -> -s.getPlayerOrder().indexOf(p)))
                    .orElse(winnerId);
        }

        if (matchOver) {
            s.setPhase(GameState.Phase.FINISHED);
            s.setMatchWinnerId(matchWinner);
        } else {
            s.setRoundNumber(s.getRoundNumber() + 1);
            s.setCurrentRound(freshRound());
        }
        return new TurnResult(true, values, false, true, winnerId,
                matchOver, matchWinner, newlyEliminated, r.getStat());
    }

    // ------------------------------------------------------------------ helpers

    private Round freshRound() {
        Round r = new Round();
        r.setPhase(Round.Phase.PICKING);
        return r;
    }

    private void requireRunning(GameState s) {
        if (s.getPhase() != GameState.Phase.RUNNING) {
            throw new GameException("Match is not running");
        }
    }

    private String orderedFirst(GameState s, List<String> ids) {
        return ids.stream()
                .min(Comparator.comparingInt(p -> s.getPlayerOrder().indexOf(p)))
                .orElseThrow();
    }
}
