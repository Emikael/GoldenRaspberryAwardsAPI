package com.emikaelsilveira.goldenraspberry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ProducerIntervalDto(
        @JsonProperty("min") List<ProducerInterval> min,
        @JsonProperty("max") List<ProducerInterval> max
) {}
