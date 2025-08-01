package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.emikaelsilveira.goldenraspberry.dto.ProducerInterval;
import com.emikaelsilveira.goldenraspberry.dto.ProducerIntervalDto;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import com.emikaelsilveira.goldenraspberry.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:business_testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
@Transactional
class BusinessLogicIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();
    }

    @Test
    void shouldCalculateSimpleProducerInterval() {
        createWinningMovie(1980, "Movie A", "John Producer");
        createWinningMovie(1985, "Movie B", "John Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).hasSize(1);
        assertThat(result.max()).hasSize(1);

        final var interval = result.min().getFirst();
        assertThat(interval.producer()).contains("John");
        assertThat(interval.interval()).isEqualTo(5);
        assertThat(interval.previousWin()).isEqualTo(1980);
        assertThat(interval.followingWin()).isEqualTo(1985);
    }

    @Test
    void shouldCalculateMultipleIntervalsForSameProducer() {
        createWinningMovie(1980, "Movie A", "Prolific Producer");
        createWinningMovie(1983, "Movie B", "Prolific Producer");
        createWinningMovie(1990, "Movie C", "Prolific Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();

        final var allIntervals = getAllIntervals(result);
        assertThat(allIntervals).hasSize(2);
        assertThat(allIntervals)
                .extracting(ProducerInterval::interval)
                .containsExactlyInAnyOrder(3, 7);
    }

    @Test
    void shouldFindMinimumAndMaximumIntervals() {
        createWinningMovie(1980, "Movie A", "Fast Producer");
        createWinningMovie(1981, "Movie B", "Fast Producer");

        createWinningMovie(1980, "Movie C", "Slow Producer");
        createWinningMovie(1990, "Movie D", "Slow Producer");

        createWinningMovie(1985, "Movie E", "Medium Producer");
        createWinningMovie(1990, "Movie F", "Medium Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).hasSize(1);
        assertThat(result.max()).hasSize(1);

        final var minInterval = result.min().getFirst();
        assertThat(minInterval.interval()).isEqualTo(1);

        final var maxInterval = result.max().getFirst();
        assertThat(maxInterval.interval()).isEqualTo(10);
    }

    @Test
    void shouldIgnoreNonWinningMovies() {
        createWinningMovie(1980, "Winner A", "Mixed Producer");
        createNonWinningMovie(1982, "Loser B", "Mixed Producer");
        createWinningMovie(1985, "Winner C", "Mixed Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).hasSize(1);

        final var interval = result.min().getFirst();
        assertThat(interval.interval()).isEqualTo(5);
        assertThat(interval.previousWin()).isEqualTo(1980);
        assertThat(interval.followingWin()).isEqualTo(1985);
    }

    @Test
    void shouldHandleCompleteBusinessWorkflow() {
        setupRealisticDataset();

        final var allMovies = movieRepository.findAll();
        final var winners = movieRepository.findAllWinningMoviesOrderedByYear();
        final var intervals = movieService.getProducerIntervals();

        assertThat(allMovies).hasSizeGreaterThan(10);
        assertThat(winners).hasSizeGreaterThan(3);
        assertThat(intervals).isNotNull();

        final var winnerCount = allMovies.stream().filter(Movie::getWinner).count();
        assertThat(winnerCount).isEqualTo(winners.size());

        final var allIntervals = getAllIntervals(intervals);
        if (!allIntervals.isEmpty()) {
            allIntervals.forEach(interval -> {
                assertThat(interval.interval()).isPositive();
                assertThat(interval.previousWin()).isLessThan(interval.followingWin());
                assertThat(interval.producer()).isNotEmpty();
            });
        }
    }

    @Test
    void shouldHandleEmptyDatasetGracefully() {
        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).isEmpty();
        assertThat(result.max()).isEmpty();
    }

    @Test
    void shouldHandleSingleWinnerPerProducer() {
        createWinningMovie(1980, "Winner A", "Producer A");
        createWinningMovie(1981, "Winner B", "Producer B");
        createWinningMovie(1982, "Winner C", "Producer C");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();

        if (nonNull(result.min())) {
            assertThat(result.min()).isEmpty();
        }
        if (nonNull(result.max())) {
            assertThat(result.max()).isEmpty();
        }
    }

    @Test
    void shouldHandleMultipleProducersOnSameMovie() {
        createWinningMovie(1980, "Collaborative Movie A", "Producer A, Producer B");
        createWinningMovie(1985, "Solo Movie A", "Producer A");
        createWinningMovie(1983, "Solo Movie B", "Producer B");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();

        final var allIntervals = getAllIntervals(result);
        if (!allIntervals.isEmpty()) {
            assertThat(allIntervals).hasSizeGreaterThanOrEqualTo(1);

            allIntervals.forEach(interval -> {
                assertThat(interval.interval()).isPositive();
                assertThat(interval.producer()).isNotEmpty();
                assertThat(interval.previousWin()).isLessThan(interval.followingWin());
            });
        }
    }

    @Test
    void shouldHandleLargeDatasetEfficiently() {
        setupLargeDataset(100);

        final var startTime = System.currentTimeMillis();

        final var result = movieService.getProducerIntervals();

        final var endTime = System.currentTimeMillis();
        final var processingTime = endTime - startTime;

        assertThat(processingTime).isLessThan(5000);
        assertThat(result).isNotNull();

        final var allIntervals = getAllIntervals(result);
        allIntervals.forEach(interval -> {
            assertThat(interval.interval()).isPositive();
            assertThat(interval.previousWin()).isLessThan(interval.followingWin());
            assertThat(interval.producer()).isNotEmpty();
        });
    }

    @Test
    void shouldValidateIntervalCalculationAccuracy() {
        createWinningMovie(1980, "Movie 1", "Test Producer");
        createWinningMovie(1985, "Movie 2", "Test Producer");
        createWinningMovie(1990, "Movie 3", "Test Producer");
        createWinningMovie(1992, "Movie 4", "Test Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();

        final var allIntervals = getAllIntervals(result);
        assertThat(allIntervals).hasSize(3);

        final var intervals = allIntervals.stream()
                .map(ProducerInterval::interval)
                .sorted()
                .toList();

        assertThat(intervals).containsExactly(2, 5, 5);

        assertThat(allIntervals)
                .anyMatch(i -> i.previousWin() == 1980 && i.followingWin() == 1985)
                .anyMatch(i -> i.previousWin() == 1985 && i.followingWin() == 1990)
                .anyMatch(i -> i.previousWin() == 1990 && i.followingWin() == 1992);
    }

    @Test
    void shouldHandleEdgeCaseYears() {
        createWinningMovie(1920, "Very Old Movie", "Vintage Producer");
        createWinningMovie(2023, "Modern Movie", "Vintage Producer");

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.max()).hasSize(1);

        final var interval = result.max().getFirst();
        assertThat(interval.interval()).isEqualTo(103);
    }

    @Test
    void shouldMaintainDataConsistencyThroughoutProcess() {
        setupRealisticDataset();

        final var initialMovieCount = movieRepository.count();
        final var initialWinnerCount = movieRepository.findAllWinningMoviesOrderedByYear().size();

        final var result1 = movieService.getProducerIntervals();
        final var result2 = movieService.getProducerIntervals();
        final var result3 = movieService.getProducerIntervals();

        assertThat(movieRepository.count()).isEqualTo(initialMovieCount);
        assertThat(movieRepository.findAllWinningMoviesOrderedByYear()).hasSize(initialWinnerCount);

        assertThat(result1.min()).hasSize(result2.min().size());
        assertThat(result1.max()).hasSize(result2.max().size());
        assertThat(result2.min()).hasSize(result3.min().size());
        assertThat(result2.max()).hasSize(result3.max().size());
    }

    private List<ProducerInterval> getAllIntervals(ProducerIntervalDto result) {
        final var allIntervals = new ArrayList<ProducerInterval>();
        if (result.min() != null) {
            allIntervals.addAll(result.min());
        }
        if (result.max() != null) {
            allIntervals.addAll(result.max());
        }
        return allIntervals;
    }

    private void createWinningMovie(int year, String title, String producers) {
        final var movie = Movie.builder()
                .year(year)
                .title(title)
                .producers(producers)
                .studios("Test Studio")
                .winner(true)
                .build();
        movieRepository.save(movie);
    }

    private void createNonWinningMovie(int year, String title, String producers) {
        final var movie = Movie.builder()
                .year(year)
                .title(title)
                .producers(producers)
                .studios("Test Studio")
                .winner(false)
                .build();
        movieRepository.save(movie);
    }

    private void setupRealisticDataset() {
        createWinningMovie(1980, "Disaster Epic", "Allan Carr");
        createWinningMovie(1984, "Musical Flop", "Allan Carr");
        createWinningMovie(1990, "Action Disaster", "Allan Carr");

        createWinningMovie(1989, "Action Movie", "Joel Silver");
        createWinningMovie(1990, "Action Sequel", "Joel Silver");

        createWinningMovie(1982, "Horror Movie", "Sean S. Cunningham");
        createWinningMovie(1995, "Horror Revival", "Sean S. Cunningham");

        createWinningMovie(1985, "One-Hit Wonder", "Single Producer");
        createWinningMovie(1987, "Another One-Hit", "Another Single Producer");

        createNonWinningMovie(1981, "Almost Winner", "Close Producer");
        createNonWinningMovie(1986, "Second Attempt", "Close Producer");

        createWinningMovie(1988, "Big Budget Flop", "Producer A, Producer B");
        createWinningMovie(1993, "Producer A Solo", "Producer A");
        createWinningMovie(1994, "Producer B Solo", "Producer B");
    }

    private void setupLargeDataset(int movieCount) {
        final String[] producerNames = {
                "Producer Alpha", "Producer Beta", "Producer Gamma", "Producer Delta",
                "Producer Echo", "Producer Foxtrot", "Producer Golf", "Producer Hotel"
        };

        for (int i = 0; i < movieCount; i++) {
            final var year = 1980 + (i % 40);
            final var title = "Movie " + (i + 1);
            final var producer = producerNames[i % producerNames.length];
            final var isWinner = (i % 3 == 0);

            final var movie = Movie.builder()
                    .year(year)
                    .title(title)
                    .producers(producer)
                    .studios("Studio " + ((i % 5) + 1))
                    .winner(isWinner)
                    .build();
            movieRepository.save(movie);
        }
    }
}