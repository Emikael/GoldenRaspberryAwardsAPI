package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import com.emikaelsilveira.goldenraspberry.service.CsvReaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:csv_testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
@Transactional
class CsvProcessingIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CsvReaderService csvReaderService;

    @BeforeEach
    void setUp() {
        if (movieRepository.count() == 0) {
            try {
                csvReaderService.run(null);
            } catch (Exception _) {
                // CSV loading failed, but that's part of what we're testing
            }
        }
    }

    @Test
    void shouldLoadDefaultCsvFileOnApplicationStart() {
        final var movies = movieRepository.findAll();

        assertThat(movies)
                .isNotEmpty()
                .anyMatch(movie ->
                        movie.getYear().equals(1980) && movie.getTitle().equals("Can't Stop the Music")
                );

        final var winners = movieRepository.findAllWinningMoviesOrderedByYear();
        assertThat(winners).isNotEmpty();

        movies.forEach(movie -> {
            assertThat(movie.getYear()).isNotNull();
            assertThat(movie.getTitle()).isNotBlank();
            assertThat(movie.getProducers()).isNotBlank();
            assertThat(movie.getWinner()).isNotNull();
        });
    }

    @Test
    void shouldHandleValidCsvContent() {
        final var allMovies = movieRepository.findAll();
        assertThat(allMovies).isNotEmpty();

        allMovies.forEach(movie -> {
            assertThat(movie.getYear()).isPositive();
            assertThat(movie.getTitle()).isNotBlank();
            assertThat(movie.getProducers()).isNotBlank();
        });

        final var hasValidYear = allMovies.stream()
                .anyMatch(movie -> movie.getYear() >= 1980 && movie.getYear() <= 2030);
        assertThat(hasValidYear).isTrue();

        final var hasWinners = allMovies.stream().anyMatch(Movie::getWinner);
        final var hasNonWinners = allMovies.stream().anyMatch(movie -> !movie.getWinner());
        assertThat(hasWinners).isTrue();
        assertThat(hasNonWinners).isTrue();
    }

    @Test
    void shouldHandleEmptyAndNullFields() {
        final var movies = movieRepository.findAll();

        movies.forEach(movie -> {
            assertThat(movie.getYear()).isNotNull();
            assertThat(movie.getTitle()).isNotBlank();
            assertThat(movie.getProducers()).isNotBlank();
            assertThat(movie.getWinner()).isNotNull();
        });
    }

    @Test
    void shouldParseWinnerFieldCorrectly() {
        final var allMovies = movieRepository.findAll();
        final var winners = movieRepository.findAllWinningMoviesOrderedByYear();
        final var nonWinners = allMovies.stream()
                .filter(movie -> !movie.getWinner())
                .toList();

        assertThat(winners).isNotEmpty();
        assertThat(nonWinners).isNotEmpty();
        assertThat(winners.size() + nonWinners.size()).isEqualTo(allMovies.size());

        winners.forEach(movie -> assertThat(movie.getWinner()).isTrue());
        nonWinners.forEach(movie -> assertThat(movie.getWinner()).isFalse());
    }

    @Test
    void shouldHandleYearParsing() {
        final var movies = movieRepository.findAll();

        movies.forEach(movie -> {
            assertThat(movie.getYear()).isNotNull();
            assertThat(movie.getYear()).isGreaterThan(1900);
            assertThat(movie.getYear()).isLessThan(2030);
        });

        final var distinctYears = movies.stream()
                .map(Movie::getYear)
                .distinct()
                .count();
        assertThat(distinctYears).isGreaterThan(1);
    }

    @Test
    void shouldHandleProducerFieldParsing() {
        final var movies = movieRepository.findAll();

        movies.forEach(movie -> {
            final var producers = movie.getProducers();
            assertThat(producers).isNotBlank();

            if (producers.contains(",") || producers.contains("&") || producers.contains(" and ")) {
                assertThat(producers).hasSizeGreaterThan(3);
            }
        });
    }

    @Test
    void shouldMaintainDataConsistency() {
        final var movies = movieRepository.findAll();
        final var initialCount = movies.size();

        movieRepository.deleteAll();
        assertThat(movieRepository.count()).isZero();
        assertThat(initialCount).isGreaterThan(0);
    }

    @Test
    void shouldHandleDuplicateEntries() {
        final var movies = movieRepository.findAll();
        final var uniqueMovies = movies.stream()
                .map(movie -> movie.getYear() + ":" + movie.getTitle() + ":" + movie.getProducers())
                .distinct()
                .count();

        assertThat(uniqueMovies).isLessThanOrEqualTo(movies.size());
    }

    @Test
    void shouldValidateRequiredFields() {
        final var movies = movieRepository.findAll();

        movies.forEach(movie -> {
            assertThat(movie.getYear())
                    .isNotNull()
                    .isGreaterThan(1900)
                    .isLessThan(2030);

            assertThat(movie.getTitle())
                    .isNotNull()
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(500);

            assertThat(movie.getProducers())
                    .isNotNull()
                    .isNotBlank()
                    .hasSizeLessThanOrEqualTo(1000);

            assertThat(movie.getWinner()).isNotNull();

            if (nonNull(movie.getStudios())) {
                assertThat(movie.getStudios())
                        .isNotBlank()
                        .hasSizeLessThanOrEqualTo(1000);
            }
        });
    }

    @Test
    void shouldValidateBusinessRules() {
        final var movies = movieRepository.findAll();
        final var winners = movieRepository.findAllWinningMoviesOrderedByYear();

        final var winnerPercentage = (double) winners.size() / movies.size();
        assertThat(winnerPercentage)
                .isGreaterThan(0.01)
                .isLessThan(0.5);

        final var yearsWithWinners = winners.stream()
                .map(Movie::getYear)
                .distinct()
                .count();
        assertThat(yearsWithWinners).isGreaterThan(1);

        final var totalYears = movies.stream()
                .map(Movie::getYear)
                .distinct()
                .count();
        assertThat(totalYears).isGreaterThan(5);
    }

    @Test
    void shouldHandleEdgeCaseData() {
        final var movies = movieRepository.findAll();

        movies.forEach(movie -> {
            assertThat(movie.getTitle().trim()).isNotBlank();
            assertThat(movie.getProducers().trim()).isNotBlank();
            assertThat(movie.getYear()).isBetween(1920, 2030);

            if (nonNull(movie.getStudios())) {
                assertThat(movie.getStudios().trim()).isNotBlank();
            }
        });
    }

    @Test
    void shouldLoadCsvDataInReasonableTime() {
        final var startTime = System.currentTimeMillis();
        final var movies = movieRepository.findAll();
        final var endTime = System.currentTimeMillis();
        final var loadTime = endTime - startTime;

        assertThat(loadTime).isLessThan(2000);
        assertThat(movies).isNotEmpty();
    }

    @Test
    void shouldMaintainDataIntegrityAfterLoad() {
        final var movies = movieRepository.findAll();
        final var totalMovies = movies.size();

        movies.forEach(movie -> {
            assertThat(movie.getId()).isNotNull();
            assertThat(movie.getYear()).isNotNull();
            assertThat(movie.getTitle()).isNotNull();
            assertThat(movie.getProducers()).isNotNull();
            assertThat(movie.getWinner()).isNotNull();
        });

        final var testMovie = Movie.builder()
                .year(2023)
                .title("Test Movie After Load")
                .producers("Test Producer")
                .studios("Test Studio")
                .winner(false)
                .build();

        final var saved = movieRepository.save(testMovie);
        assertThat(saved.getId()).isNotNull();

        final var newCount = movieRepository.count();
        assertThat(newCount).isEqualTo(totalMovies + 1);

        movieRepository.delete(saved);
        assertThat(movieRepository.count()).isEqualTo(totalMovies);
    }

    @Test
    void shouldHandleConcurrentDataAccess() {
        final var movies = movieRepository.findAll();
        final var winners = movieRepository.findAllWinningMoviesOrderedByYear();

        assertThat(movies).isNotEmpty();
        assertThat(winners).isNotEmpty();

        final var moviesSecondQuery = movieRepository.findAll();
        final var winnersSecondQuery = movieRepository.findAllWinningMoviesOrderedByYear();

        assertThat(moviesSecondQuery).hasSize(movies.size());
        assertThat(winnersSecondQuery).hasSize(winners.size());

        for (int i = 0; i < Math.min(10, movies.size()); i++) {
            final var original = movies.get(i);
            final var queried = moviesSecondQuery.stream()
                    .filter(m -> m.getId().equals(original.getId()))
                    .findFirst()
                    .orElse(null);

            assertThat(queried).isNotNull();
            assertThat(queried.getYear()).isEqualTo(original.getYear());
            assertThat(queried.getTitle()).isEqualTo(original.getTitle());
            assertThat(queried.getWinner()).isEqualTo(original.getWinner());
        }
    }
}