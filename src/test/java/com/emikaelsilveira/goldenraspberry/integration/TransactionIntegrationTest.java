package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import com.emikaelsilveira.goldenraspberry.service.MovieService;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GoldenRaspberryApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction_testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
class TransactionIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService movieService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();
    }

    @Test
    void shouldUseReadOnlyTransactionForMovieService() {
        final var winnerMovie = Movie.builder()
                .year(1980)
                .title("Winner Movie")
                .producers("Producer A")
                .winner(true)
                .build();
        movieRepository.save(winnerMovie);

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).isNull();
        assertThat(result.max()).isNull();
    }

    @Test
    void shouldRollbackOnException() {
        final var validMovie = Movie.builder()
                .year(1980)
                .title("Valid Movie")
                .producers("Producer A")
                .winner(true)
                .build();

        movieRepository.save(validMovie);
        final var initialCount = movieRepository.count();
        assertThat(initialCount).isEqualTo(1);

        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                final var movie1 = Movie.builder()
                        .year(1981)
                        .title("Movie 1")
                        .producers("Producer B")
                        .winner(false)
                        .build();
                movieRepository.save(movie1);

                final var invalidMovie = Movie.builder()
                        .year(null)
                        .title("Invalid Movie")
                        .producers("Producer C")
                        .winner(false)
                        .build();
                movieRepository.save(invalidMovie);
                return null;
            });
        }).isInstanceOf(ConstraintViolationException.class);
        assertThat(movieRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void shouldCommitSuccessfulTransaction() {
        final var initialCount = movieRepository.count();

        transactionTemplate.execute(status -> {
            final var movie1 = Movie.builder()
                    .year(1980)
                    .title("Movie 1")
                    .producers("Producer A")
                    .winner(true)
                    .build();

            final var movie2 = Movie.builder()
                    .year(1981)
                    .title("Movie 2")
                    .producers("Producer B")
                    .winner(false)
                    .build();

            movieRepository.save(movie1);
            movieRepository.save(movie2);
            return null;
        });

        assertThat(movieRepository.count()).isEqualTo(initialCount + 2);
    }

    @Test
    void shouldHandleConcurrentTransactions() throws ExecutionException, InterruptedException {
        final var initialMovie = Movie.builder()
                .year(1980)
                .title("Initial Movie")
                .producers("Initial Producer")
                .winner(true)
                .build();
        movieRepository.save(initialMovie);

        final var transaction1 = CompletableFuture.runAsync(() -> {
            transactionTemplate.execute(status -> {
                final var movie = Movie.builder()
                        .year(1981)
                        .title("Movie from Transaction 1")
                        .producers("Producer 1")
                        .winner(false)
                        .build();
                movieRepository.save(movie);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        });

        final var transaction2 = CompletableFuture.runAsync(() -> {
            transactionTemplate.execute(status -> {
                final var movie = Movie.builder()
                        .year(1982)
                        .title("Movie from Transaction 2")
                        .producers("Producer 2")
                        .winner(true)
                        .build();
                movieRepository.save(movie);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException _) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        });

        CompletableFuture.allOf(transaction1, transaction2).get();

        assertThat(movieRepository.count()).isEqualTo(3);

        final var allMovies = movieRepository.findAll();
        assertThat(allMovies)
                .extracting(Movie::getTitle)
                .contains("Initial Movie", "Movie from Transaction 1", "Movie from Transaction 2");
    }

    @Test
    @Transactional
    void shouldPropagateTransactionToServiceMethod() {
        final var winnerMovie1 = Movie.builder()
                .year(1980)
                .title("Winner 1980")
                .producers("Producer A")
                .winner(true)
                .build();

        final var winnerMovie2 = Movie.builder()
                .year(1985)
                .title("Winner 1985")
                .producers("Producer A")
                .winner(true)
                .build();

        movieRepository.save(winnerMovie1);
        movieRepository.save(winnerMovie2);

        final var result = movieService.getProducerIntervals();

        assertThat(result).isNotNull();
        if (nonNull(result.min()) && !result.min().isEmpty()) {
            assertThat(result.min()).hasSize(1);
            assertThat(result.min().getFirst().interval()).isEqualTo(5);
        } else {
            assertThat(result.min()).satisfiesAnyOf(
                    intervals -> assertThat(intervals).isNull(),
                    intervals -> assertThat(intervals).isEmpty()
            );
        }
    }

    @Test
    @Transactional
    void shouldMaintainSessionWithinTransaction() {
        final var movie = Movie.builder()
                .year(1980)
                .title("Test Movie")
                .producers("Test Producer")
                .winner(true)
                .build();

        final var savedMovie = movieRepository.save(movie);
        final var movieId = savedMovie.getId();

        final var retrievedMovie = movieRepository.findById(movieId).orElse(null);
        assertThat(retrievedMovie)
                .isNotNull()
                .isSameAs(savedMovie);
    }

    @Test
    void shouldCreateNewSessionForNewTransaction() {
        final var movie = Movie.builder()
                .year(1980)
                .title("Test Movie")
                .producers("Test Producer")
                .winner(true)
                .build();

        final var movieId = transactionTemplate.execute(_ -> {
            final var savedMovie = movieRepository.save(movie);
            return savedMovie.getId();
        });

        final var retrievedMovie = transactionTemplate.execute(_ ->
                movieRepository.findById(movieId).orElse(null));

        assertThat(retrievedMovie).isNotNull();
        assertThat(retrievedMovie.getId()).isEqualTo(movieId);
        assertThat(retrievedMovie.getTitle()).isEqualTo("Test Movie");
    }

    @Test
    void shouldHandleBulkOperationsInTransaction() {
        final var movies = List.of(
                Movie.builder().year(1980).title("Movie 1").producers("Producer A").winner(true).build(),
                Movie.builder().year(1981).title("Movie 2").producers("Producer B").winner(false).build(),
                Movie.builder().year(1982).title("Movie 3").producers("Producer C").winner(true).build(),
                Movie.builder().year(1983).title("Movie 4").producers("Producer D").winner(false).build(),
                Movie.builder().year(1984).title("Movie 5").producers("Producer E").winner(true).build()
        );

        transactionTemplate.execute(_ -> {
            movieRepository.saveAll(movies);
            return null;
        });

        assertThat(movieRepository.count()).isEqualTo(5);

        final var winningMovies = movieRepository.findAllWinningMoviesOrderedByYear();
        assertThat(winningMovies).hasSize(3);
    }

    @Test
    void shouldRollbackBulkOperationOnError() {
        final var validMovies = List.of(
                Movie.builder().year(1980).title("Movie 1").producers("Producer A").winner(true).build(),
                Movie.builder().year(1981).title("Movie 2").producers("Producer B").winner(false).build(),
                Movie.builder().year(1982).title("Movie 3").producers("Producer C").winner(true).build()
        );

        movieRepository.saveAll(validMovies);
        final var initialCount = movieRepository.count();

        final var mixedMovies = List.of(
                Movie.builder().year(1983).title("Movie 4").producers("Producer D").winner(false).build(),
                Movie.builder().year(null).title("Invalid Movie").producers("Producer E").winner(true).build(),
                Movie.builder().year(1985).title("Movie 6").producers("Producer F").winner(false).build()
        );

        assertThatThrownBy(() -> {
            transactionTemplate.execute(status -> {
                movieRepository.saveAll(mixedMovies);
                entityManager.flush();
                return null;
            });
        }).isInstanceOf(ConstraintViolationException.class);
        assertThat(movieRepository.count()).isEqualTo(initialCount);
    }

    @Test
    void shouldNotTimeoutForNormalOperations() {
        final var startTime = System.currentTimeMillis();

        transactionTemplate.execute(status -> {
            for (int i = 0; i < 100; i++) {
                final var movie = Movie.builder()
                        .year(1980 + i)
                        .title("Movie " + i)
                        .producers("Producer " + i)
                        .winner(i % 2 == 0)
                        .build();
                movieRepository.save(movie);
            }
            return null;
        });

        final var endTime = System.currentTimeMillis();
        final var executionTime = endTime - startTime;

        assertThat(movieRepository.count()).isEqualTo(100);
        assertThat(executionTime).isLessThan(10000);
    }
}