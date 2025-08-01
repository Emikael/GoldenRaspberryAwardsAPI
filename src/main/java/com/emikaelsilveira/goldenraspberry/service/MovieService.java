package com.emikaelsilveira.goldenraspberry.service;

import com.emikaelsilveira.goldenraspberry.dto.ProducerInterval;
import com.emikaelsilveira.goldenraspberry.dto.ProducerIntervalDto;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;

    public ProducerIntervalDto getProducerIntervals() {
        log.info("Calculating producer award intervals");
        final var winningMovies = movieRepository.findAllWinningMoviesOrderedByYear();

        if (winningMovies.isEmpty()) {
            log.warn("No winning movies found in database");
            return ProducerIntervalDto.builder()
                    .max(emptyList())
                    .min(emptyList())
                    .build();
        }

        final var producerWins = extractProducerWins(winningMovies);
        final var allIntervals = calculateProducerIntervals(producerWins);

        if (allIntervals.isEmpty()) {
            log.info("No producers found with multiple wins");
            return ProducerIntervalDto.builder().build();
        }
        return groupIntervalsByMinMax(allIntervals);
    }

    private Map<String, List<Integer>> extractProducerWins(final List<Movie> winningMovies) {
        final var producerWins = new HashMap<String, List<Integer>>();

        for (Movie movie : winningMovies) {
            final var producersField = movie.getProducers();
            if (isNull(producersField) || producersField.trim().isEmpty()) {
                continue;
            }

            final var producers = parseProducerNames(producersField);
            for (String producer : producers) {
                producerWins.computeIfAbsent(producer, key -> new ArrayList<>()).add(movie.getYear());
            }
        }
        producerWins.values().forEach(Collections::sort);

        log.info("Extracted {} unique producers from winning movies", producerWins.size());
        return producerWins;
    }

    private List<String> parseProducerNames(final String producersField) {
        if (isNull(producersField) || producersField.trim().isEmpty()) {
            return emptyList();
        }

        final var parts = producersField.split("[,&]|\\band\\b");

        final var producers = new ArrayList<String>();
        for (String part : parts) {
            String cleaned = cleanProducerName(part);
            if (!cleaned.isEmpty()) {
                producers.add(cleaned);
            }
        }
        return producers;
    }

    private String cleanProducerName(final String rawName) {
        if (isNull(rawName)) {
            return EMPTY;
        }

        final var cleanedName = rawName.trim()
                .replaceAll("^(Producer|Produced by|Executive Producer):?\\s*", "")
                .replaceAll("\\s*\\(.*\\)$", "") // Remove parenthetical information
                .replaceAll("\\s+", " ") // Normalize whitespace
                .trim();

        if (cleanedName.length() < 2 || !cleanedName.matches(".*[a-zA-Z].*")) {
            return EMPTY;
        }
        return cleanedName;
    }

    private List<ProducerInterval> calculateProducerIntervals(final Map<String, List<Integer>> producerWins) {
        final var intervals = new ArrayList<ProducerInterval>();

        for (Map.Entry<String, List<Integer>> entry : producerWins.entrySet()) {
            final var producer = entry.getKey();
            final var years = entry.getValue();

            if (years.size() < 2) {
                continue;
            }

            for (int i = 1; i < years.size(); i++) {
                final var previousWin = years.get(i - 1);
                final var followingWin = years.get(i);
                final var interval = followingWin - previousWin;

                intervals.add(ProducerInterval.builder()
                        .producer(producer)
                        .interval(interval)
                        .previousWin(previousWin)
                        .followingWin(followingWin)
                        .build()
                );
            }
        }

        log.info("Calculated {} producer intervals", intervals.size());
        return intervals;
    }

    private ProducerIntervalDto groupIntervalsByMinMax(final List<ProducerInterval> allIntervals) {
        if (allIntervals.isEmpty()) {
            return ProducerIntervalDto.builder().build();
        }

        final var minInterval = allIntervals.stream()
                .mapToInt(ProducerInterval::interval)
                .min()
                .orElse(0);

        final var maxInterval = allIntervals.stream()
                .mapToInt(ProducerInterval::interval)
                .max()
                .orElse(0);

        final var minIntervals = allIntervals.stream()
                .filter(interval -> interval.interval() == minInterval)
                .toList();

        final var maxIntervals = allIntervals.stream()
                .filter(interval -> interval.interval() == maxInterval)
                .toList();

        log.info("Found {} producers with minimum interval ({} years) and {} producers with maximum interval ({} years)",
                minIntervals.size(), minInterval, maxIntervals.size(), maxInterval);

        return ProducerIntervalDto.builder()
                .min(minIntervals)
                .max(maxIntervals)
                .build();
    }
}
