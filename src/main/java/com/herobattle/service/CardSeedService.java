package com.herobattle.service;

import java.util.List;
import java.util.Objects;

import com.herobattle.model.Card;
import com.herobattle.repository.CardRepository;
import com.herobattle.service.SuperheroApiDtos.Hero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Seeds the {@code card} table once from the akabab/superhero-api static dataset.
 *
 * <p>Runs on application startup. It is a no-op when the table already holds cards, so
 * restarts are cheap and idempotent. Disable entirely with
 * {@code heroclash.seed.enabled=false} (used by tests).
 */
@Service
public class CardSeedService {

    private static final Logger log = LoggerFactory.getLogger(CardSeedService.class);

    private final CardRepository cardRepository;
    private final RestClient restClient;
    private final boolean enabled;
    private final String datasetUrl;

    public CardSeedService(CardRepository cardRepository,
                           RestClient.Builder restClientBuilder,
                           @Value("${heroclash.seed.enabled:true}") boolean enabled,
                           @Value("${heroclash.seed.url:https://akabab.github.io/superhero-api/api/all.json}")
                           String datasetUrl) {
        this.cardRepository = cardRepository;
        this.restClient = restClientBuilder.build();
        this.enabled = enabled;
        this.datasetUrl = datasetUrl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedOnStartup() {
        if (!enabled) {
            log.info("Card seeding disabled (heroclash.seed.enabled=false)");
            return;
        }
        long existing = cardRepository.count();
        if (existing > 0) {
            log.info("Card table already seeded ({} cards) — skipping", existing);
            return;
        }
        seed();
    }

    /** Fetches the dataset and persists it. Package-visible for tests. */
    int seed() {
        log.info("Seeding cards from {}", datasetUrl);
        List<Hero> heroes = restClient.get()
                .uri(datasetUrl)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (heroes == null || heroes.isEmpty()) {
            log.warn("Superhero dataset returned no entries — nothing seeded");
            return 0;
        }
        List<Card> cards = heroes.stream()
                .filter(Objects::nonNull)
                .map(CardSeedService::toCard)
                .toList();
        cardRepository.saveAll(cards);
        log.info("Seeded {} cards", cards.size());
        return cards.size();
    }

    static Card toCard(Hero hero) {
        Card card = new Card();
        card.setName(hero.name());
        if (hero.biography() != null) {
            card.setAlignment(hero.biography().alignment());
        }
        card.setImageUrl(pickImage(hero));
        SuperheroApiDtos.PowerStats stats = hero.powerstats();
        if (stats != null) {
            card.setIntelligence(stats.intelligence());
            card.setStrength(stats.strength());
            card.setSpeed(stats.speed());
            card.setDurability(stats.durability());
            card.setPower(stats.power());
            card.setCombat(stats.combat());
        }
        return card;
    }

    private static String pickImage(Hero hero) {
        if (hero.images() == null) {
            return null;
        }
        SuperheroApiDtos.Images i = hero.images();
        if (i.md() != null) {
            return i.md();
        }
        if (i.sm() != null) {
            return i.sm();
        }
        return i.lg() != null ? i.lg() : i.xs();
    }
}
