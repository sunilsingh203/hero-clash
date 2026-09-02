package com.herobattle.controller.dto;

import com.herobattle.game.GameCard;
import com.herobattle.game.GameState;
import com.herobattle.game.Round;
import com.herobattle.game.TurnResult;
import java.util.List;
import java.util.Map;

/** Payloads for the game (STOMP broadcasts + game REST reads). */
public final class GameDtos {

    private GameDtos() {
    }

    /** STOMP action: active player picks a stat. */
    public record PickMessage(String playerId, String stat) {
    }

    /** STOMP action: a contender confirms their flip. */
    public record RevealMessage(String playerId) {
    }

    public record PlayerState(String playerId, String displayName, int deckCount, boolean eliminated) {
    }

    /**
     * Public view of the current round. Card faces in {@code reveals} are only populated
     * once a flip has resolved — before that the server holds them.
     */
    public record RoundView(
            String phase,
            String stat,
            List<String> contenders,
            List<String> responded,
            int showdownDepth,
            Map<String, Integer> revealValues,
            Map<String, List<GameCard>> reveals,
            String winnerId) {

        static RoundView from(Round r) {
            if (r == null) {
                return null;
            }
            boolean flipResolved = !r.getLastRevealValues().isEmpty();
            return new RoundView(
                    r.getPhase().name(),
                    r.getStat() == null ? null : r.getStat().name(),
                    List.copyOf(r.getContenders()),
                    List.copyOf(r.getResponded()),
                    r.getShowdownDepth(),
                    Map.copyOf(r.getLastRevealValues()),
                    flipResolved ? Map.copyOf(r.getPlays()) : Map.of(),
                    r.getWinnerId());
        }
    }

    /**
     * What the previous flip decided. Carried separately because by broadcast time the
     * engine has already opened the next round.
     */
    public record ResolutionView(
            String stat,
            Map<String, Integer> values,
            String roundWinnerId,
            boolean matchOver,
            String matchWinnerId,
            List<String> eliminated) {

        static ResolutionView from(TurnResult r) {
            if (r == null || !r.roundResolved()) {
                return null;
            }
            return new ResolutionView(
                    r.stat() == null ? null : r.stat().name(),
                    Map.copyOf(r.revealValues()),
                    r.roundWinnerId(),
                    r.matchOver(),
                    r.matchWinnerId(),
                    List.copyOf(r.eliminated()));
        }
    }

    /** Everything a spectator/player may see about a match without seeing hidden hands. */
    public record GameView(
            String roomCode,
            String phase,
            int roundNumber,
            int roundCap,
            String activePlayerId,
            String matchWinnerId,
            List<PlayerState> players,
            RoundView round,
            ResolutionView resolution) {

        public static GameView from(GameState s, Map<String, String> names) {
            return from(s, names, null);
        }

        public static GameView from(GameState s, Map<String, String> names, TurnResult result) {
            List<PlayerState> players = s.getPlayerOrder().stream()
                    .map(id -> new PlayerState(
                            id,
                            names.getOrDefault(id, id),
                            s.deckSize(id),
                            s.getEliminated().contains(id)))
                    .toList();
            return new GameView(
                    s.getRoomCode(),
                    s.getPhase().name(),
                    s.getRoundNumber(),
                    s.getRoundCap(),
                    s.getActivePlayerId(),
                    s.getMatchWinnerId(),
                    players,
                    RoundView.from(s.getCurrentRound()),
                    ResolutionView.from(result));
        }
    }

    /** A single player's private hand. */
    public record HandView(String playerId, int deckCount, GameCard topCard, List<GameCard> cards) {

        public static HandView from(GameState s, String playerId) {
            List<GameCard> cards = List.copyOf(s.deckOf(playerId));
            GameCard top = cards.isEmpty() ? null : cards.get(0);
            return new HandView(playerId, cards.size(), top, cards);
        }
    }

    /** STOMP error event pushed to {@code /topic/rooms.{code}.errors}. */
    public record GameError(String message) {
    }
}
