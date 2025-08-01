package com.emikaelsilveira.goldenraspberry.service;

import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.exception.CSVReaderException;
import com.emikaelsilveira.goldenraspberry.exception.H2DatabaseException;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.emikaelsilveira.goldenraspberry.utilities.Utils.normalizeFalse;
import static com.emikaelsilveira.goldenraspberry.utilities.Utils.normalizeTrue;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CsvReaderService implements ApplicationRunner {

    private static final String CSV_FILE_PATH = "movielist.csv";
    private final MovieRepository movieRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting CSV data loading process...");

        try {
            final var movies = loadMoviesFromCsv();
            saveMoviesToDatabase(movies);
            log.info("Successfully loaded {} movies from CSV file", movies.size());
        } catch (Exception e) {
            log.error("Error loading CSV data: {}", e.getMessage(), e);
            throw new CSVReaderException("Failed to load CSV data", e);
        }
    }

    private List<Movie> loadMoviesFromCsv() throws IOException, CsvException {
        final var movies = new ArrayList<Movie>();
        final var resource = new ClassPathResource(CSV_FILE_PATH);

        if (!resource.exists()) {
            log.warn("CSV file {} not found in classpath. Creating empty dataset.", CSV_FILE_PATH);
            return movies;
        }

        try (final var csvReader = new CSVReaderBuilder(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                .withCSVParser(new com.opencsv.CSVParserBuilder().withSeparator(';').build())
                .build()) {

            final var lines = csvReader.readAll();

            if (lines.isEmpty()) {
                log.warn("CSV file {} is empty", CSV_FILE_PATH);
                return movies;
            }

            final var skipHeader = isHeaderRow(lines.get(0));
            final var startIndex = skipHeader ? 1 : 0;

            for (var i = startIndex; i < lines.size(); i++) {
                final var line = lines.get(i);
                try {
                    final var movie = parseMovieFromCsvRecord(line, i + 1);
                    if (nonNull(movie)) {
                        movies.add(movie);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing CSV record at line {}: {}. Skipping record.", i + 1, e.getMessage());
                }
            }
        }
        return movies;
    }

    private boolean isHeaderRow(final String[] firstRow) {
        if (firstRow.length >= 2) {
            final var firstColumn = firstRow[0].trim().toLowerCase();
            return firstColumn.contains("year");
        }
        return false;
    }

    private Movie parseMovieFromCsvRecord(final String[] line, final int lineNumber) {
        if (line.length < 5) {
            log.warn("CSV record at line {} has insufficient columns (expected 5, got {})",
                    lineNumber, line.length);
            return null;
        }

        try {
            final var year = parseYear(line[0].trim());
            final var title = line[1].trim();
            var studios = line[2].trim();
            final var producers = line[3].trim();
            final var winner = parseWinner(line[4].trim());

            if (isNull(year) || title.isEmpty() || producers.isEmpty() || isNull(winner)) {
                log.warn("CSV record at line {} has invalid or missing required fields", lineNumber);
                return null;
            }

            if (studios.isEmpty()) {
                studios = null;
            }

            return Movie.builder()
                    .studios(studios)
                    .title(title)
                    .winner(winner)
                    .producers(producers)
                    .year(year)
                    .build();

        } catch (Exception e) {
            log.warn("Error parsing CSV record at line {}: {}", lineNumber, e.getMessage());
            return null;
        }
    }

    private Integer parseYear(final String year) {
        if (isNull(year) || year.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(year);
        } catch (NumberFormatException _) {
            log.warn("Invalid year format: {}", year);
            return null;
        }
    }

    private Boolean parseWinner(final String winner) {
        if (isNull(winner) || winner.isEmpty()) {
            return false;
        }

        final var normalized = winner.trim().toLowerCase();

        if (normalizeTrue(normalized)) {
            return true;
        }

        if (normalizeFalse(normalized)) {
            return false;
        }

        return !normalized.isEmpty();
    }

    private void saveMoviesToDatabase(List<Movie> movies) {
        if (movies.isEmpty()) {
            log.info("No movies to save to database");
            return;
        }

        try {
            final var existingCount = movieRepository.count();
            if (existingCount > 0) {
                log.info("Clearing {} existing movies from database", existingCount);
                movieRepository.deleteAll();
            }

            movieRepository.saveAll(movies);
            log.info("Successfully saved {} movies to database", movies.size());

            long winnerCount = movies.stream().filter(Movie::getWinner).count();
            log.info("Database statistics: {} total movies, {} winners", movies.size(), winnerCount);
        } catch (Exception e) {
            log.error("Error saving movies to database: {}", e.getMessage(), e);
            throw new H2DatabaseException("Failed to save movies to database", e);
        }
    }
}
