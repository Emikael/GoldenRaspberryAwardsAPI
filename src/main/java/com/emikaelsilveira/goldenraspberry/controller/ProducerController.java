package com.emikaelsilveira.goldenraspberry.controller;

import com.emikaelsilveira.goldenraspberry.dto.ProducerIntervalDto;
import com.emikaelsilveira.goldenraspberry.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/producers")
public class ProducerController {

    private final MovieService movieService;

    @GetMapping("/intervals")
    public ResponseEntity<ProducerIntervalDto> getProducerIntervals() {
        final var intervals = movieService.getProducerIntervals();
        return new ResponseEntity<>(intervals, HttpStatus.OK);
    }
}
