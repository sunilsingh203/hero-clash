package com.herobattle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import com.herobattle.model.Card;
import com.herobattle.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CardSeedServiceTest {

    private static final String URL = "https://example.test/all.json";

    private CardRepository cardRepository;
    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private CardSeedService service() {
        return new CardSeedService(cardRepository, builder, true, URL);
    }

    @Test
    void mapsDatasetFieldsOntoCards() {
        String json = """
                [
                  {
                    "id": 1,
                    "name": "A-Bomb",
                    "powerstats": {"intelligence": 38, "strength": 100, "speed": 17,
                                   "durability": 80, "power": 24, "combat": 64},
                    "biography": {"alignment": "good"},
                    "images": {"sm": "s.jpg", "md": "m.jpg", "lg": "l.jpg"}
                  }
                ]
                """;
        server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        int seeded = service().seed();

        assertThat(seeded).isEqualTo(1);
        ArgumentCaptor<List<Card>> captor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(cardRepository).saveAll(captor.capture());
        Card card = captor.getValue().get(0);
        assertThat(card.getName()).isEqualTo("A-Bomb");
        assertThat(card.getAlignment()).isEqualTo("good");
        assertThat(card.getImageUrl()).isEqualTo("m.jpg");
        assertThat(card.getIntelligence()).isEqualTo(38);
        assertThat(card.getStrength()).isEqualTo(100);
        assertThat(card.getSpeed()).isEqualTo(17);
        assertThat(card.getDurability()).isEqualTo(80);
        assertThat(card.getPower()).isEqualTo(24);
        assertThat(card.getCombat()).isEqualTo(64);
        server.verify();
    }

    @Test
    void toleratesMissingPowerstatsAndImages() {
        String json = """
                [ {"id": 70, "name": "Mystery", "biography": {"alignment": null}} ]
                """;
        server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        int seeded = service().seed();

        assertThat(seeded).isEqualTo(1);
    }

    @Test
    void skipsSeedingWhenTableAlreadyPopulated() {
        when(cardRepository.count()).thenReturn(731L);

        service().seedOnStartup();

        org.mockito.Mockito.verify(cardRepository, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
