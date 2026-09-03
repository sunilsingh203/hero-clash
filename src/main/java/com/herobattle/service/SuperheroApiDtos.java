package com.herobattle.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Minimal DTOs mirroring the shape of the akabab/superhero-api dataset
 * (https://akabab.github.io/superhero-api/api/all.json). Only the fields Hero Clash
 * needs are mapped; everything else is ignored.
 */
final class SuperheroApiDtos {

    private SuperheroApiDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Hero(long id, String name, PowerStats powerstats, Biography biography, Images images) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PowerStats(Integer intelligence, Integer strength, Integer speed,
                      Integer durability, Integer power, Integer combat) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Biography(String alignment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Images(String xs, String sm, String md, String lg) {
    }
}
