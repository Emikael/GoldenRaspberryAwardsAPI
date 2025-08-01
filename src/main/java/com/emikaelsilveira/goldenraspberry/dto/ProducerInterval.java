package com.emikaelsilveira.goldenraspberry.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ProducerInterval(
        @JsonProperty("producer") String producer,
        @JsonProperty("interval") Integer interval,
        @JsonProperty("previousWin") Integer previousWin,
        @JsonProperty("followingWin") Integer followingWin
) {}