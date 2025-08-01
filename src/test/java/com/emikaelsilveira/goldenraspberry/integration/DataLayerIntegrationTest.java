package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GoldenRaspberryApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalayer_testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
@Transactional
class DataLayerIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EntityManager entityManager;

    private Movie validMovie;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();
        validMovie = Movie.builder()
                .year(1980)
                .title("Can't Stop the Music")
                .studios("Associated Film Distribution")
                .producers("Allan Carr")
                .winner(true)
                .build();
    }

    @Test
    void shouldCreateMovieTableWithCorrectColumns() {
        final var savedMovie = movieRepository.save(validMovie);

        assertThat(savedMovie.getId()).isNotNull();
        assertThat(savedMovie.getYear()).isEqualTo(1980);
        assertThat(savedMovie.getTitle()).isEqualTo("Can't Stop the Music");
        assertThat(savedMovie.getStudios()).isEqualTo("Associated Film Distribution");
        assertThat(savedMovie.getProducers()).isEqualTo("Allan Carr");
        assertThat(savedMovie.getWinner()).isTrue();
    }

    @Test
    void shouldEnforceNotNullConstraintOnYear() {
        final var movieWithNullYear = Movie.builder()
                .title("Test Movie")
                .producers("Test Producer")
                .winner(false)
                .build();

        assertThatThrownBy(() -> {
            movieRepository.save(movieWithNullYear);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldEnforceNotNullConstraintOnTitle() {
        final var movieWithNullTitle = Movie.builder()
                .year(1980)
                .producers("Test Producer")
                .winner(false)
                .build();

        assertThatThrownBy(() -> {
            movieRepository.save(movieWithNullTitle);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldEnforceNotNullConstraintOnProducers() {
        final var movieWithNullProducers = Movie.builder()
                .year(1980)
                .title("Test Movie")
                .winner(false)
                .build();

        assertThatThrownBy(() -> {
            movieRepository.save(movieWithNullProducers);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldAllowNullStudios() {
        final var movieWithNullStudios = Movie.builder()
                .year(1980)
                .title("Test Movie")
                .producers("Test Producer")
                .winner(false)
                .build();

        final var savedMovie = movieRepository.save(movieWithNullStudios);
        assertThat(savedMovie.getStudios()).isNull();
    }

    @Test
    void shouldEnforceStringLengthLimits() {
        final var longTitle = "A".repeat(501);
        final var movieWithLongTitle = Movie.builder()
                .year(1980)
                .title(longTitle)
                .producers("Test Producer")
                .winner(false)
                .build();

        assertThatThrownBy(() -> {
            movieRepository.save(movieWithLongTitle);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldHandleMaximumAllowedStringLengths() {
        final var maxTitle = "A".repeat(500);
        final var maxStudios = "B".repeat(1000);
        final var maxProducers = "C".repeat(1000);

        final var movieWithMaxLengths = Movie.builder()
                .year(1980)
                .title(maxTitle)
                .studios(maxStudios)
                .producers(maxProducers)
                .winner(false)
                .build();

        final var savedMovie = movieRepository.save(movieWithMaxLengths);
        assertThat(savedMovie.getTitle()).hasSize(500);
        assertThat(savedMovie.getStudios()).hasSize(1000);
        assertThat(savedMovie.getProducers()).hasSize(1000);
    }

    @Test
    void shouldGenerateSequentialIds() {
        final var movie1 = movieRepository.save(Movie.builder()
                .year(1980)
                .title("Movie 1")
                .producers("Producer 1")
                .winner(false)
                .build()
        );
        final var movie2 = movieRepository.save(Movie.builder()
                .year(1981)
                .title("Movie 2")
                .producers("Producer 2")
                .winner(false)
                .build()
        );

        assertThat(movie1.getId()).isNotNull();
        assertThat(movie2.getId()).isNotNull();
        assertThat(movie2.getId()).isGreaterThan(movie1.getId());
    }

    @Test
    void shouldFindAllWinningMoviesOrderedByYear() {
        final var movie1980 = Movie.builder()
                .year(1980)
                .title("Winner 1980")
                .producers("Producer A")
                .winner(true)
                .build();
        final var movie1982 = Movie.builder()
                .year(1982)
                .title("Winner 1982")
                .producers("Producer B")
                .winner(true)
                .build();
        final var movie1981 = Movie.builder()
                .year(1981)
                .title("Winner 1981")
                .producers("Producer C")
                .winner(true)
                .build();
        final var nonWinner = Movie.builder()
                .year(1979)
                .title("Non Winner")
                .producers("Producer D")
                .winner(false)
                .build();

        movieRepository.saveAll(List.of(movie1980, movie1982, movie1981, nonWinner));

        final var winningMovies = movieRepository.findAllWinningMoviesOrderedByYear();

        assertThat(winningMovies).hasSize(3);
        assertThat(winningMovies.get(0).getYear()).isEqualTo(1980);
        assertThat(winningMovies.get(1).getYear()).isEqualTo(1981);
        assertThat(winningMovies.get(2).getYear()).isEqualTo(1982);
        assertThat(winningMovies).allMatch(Movie::getWinner);
    }

    @Test
    void shouldReturnEmptyListWhenNoWinningMovies() {
        final var nonWinner1 = Movie.builder()
                .year(1980)
                .title("Non Winner 1")
                .producers("Producer A")
                .winner(false)
                .build();
        final var nonWinner2 = Movie.builder()
                .year(1981)
                .title("Non Winner 2")
                .producers("Producer B")
                .winner(false)
                .build();

        movieRepository.saveAll(List.of(nonWinner1, nonWinner2));
        final var winningMovies = movieRepository.findAllWinningMoviesOrderedByYear();

        assertThat(winningMovies).isEmpty();
    }

    @Test
    void shouldFindMovieById() {
        final var savedMovie = movieRepository.save(validMovie);
        final var foundMovie = movieRepository.findById(savedMovie.getId()).orElse(null);

        assertThat(foundMovie).isNotNull();
        assertThat(foundMovie.getTitle()).isEqualTo(validMovie.getTitle());
        assertThat(foundMovie.getYear()).isEqualTo(validMovie.getYear());
    }

    @Test
    void shouldReturnEmptyOptionalForNonExistentId() {
        assertThat(movieRepository.findById(999L)).isEmpty();
    }

    @Test
    void shouldCountMoviesCorrectly() {
        movieRepository.saveAll(List.of(
                Movie.builder().year(1980).title("Movie 1").producers("P1").winner(true).build(),
                Movie.builder().year(1981).title("Movie 2").producers("P2").winner(false).build(),
                Movie.builder().year(1982).title("Movie 3").producers("P3").winner(true).build()
        ));

        assertThat(movieRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldDeleteMovieById() {
        final var savedMovie = movieRepository.save(validMovie);
        final var movieId = savedMovie.getId();

        movieRepository.deleteById(movieId);

        assertThat(movieRepository.findById(movieId)).isEmpty();
        assertThat(movieRepository.count()).isZero();
    }

    @Test
    void shouldUpdateExistingMovie() {
        final var savedMovie = movieRepository.save(validMovie);

        savedMovie.setTitle("Updated Title");
        savedMovie.setWinner(false);
        final var updatedMovie = movieRepository.save(savedMovie);

        assertThat(updatedMovie.getId()).isEqualTo(savedMovie.getId());
        assertThat(updatedMovie.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedMovie.getWinner()).isFalse();
    }

    @Test
    void shouldHandleDuplicateMovieEntries() {
        final var movie1 = Movie.builder()
                .year(1980)
                .title("Same Movie")
                .producers("Producer A")
                .winner(true)
                .build();
        final var movie2 = Movie.builder()
                .year(1980)
                .title("Same Movie")
                .producers("Producer A")
                .winner(true)
                .build();

        movieRepository.save(movie1);
        final var savedMovie2 = movieRepository.save(movie2);

        assertThat(savedMovie2.getId()).isNotNull();
        assertThat(movieRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldHandleSpecialCharactersInFields() {
        final var movieWithSpecialChars = Movie.builder()
                .year(1980)
                .title("Movie with Special Characters: !@#$%^&*()_+{}|:<>?[]\\;',./")
                .studios("Studio with Unicode: àáâãäåæçèéêë")
                .producers("Producer with Quotes: \"Double\" and 'Single'")
                .winner(true)
                .build();

        final var savedMovie = movieRepository.save(movieWithSpecialChars);

        assertThat(savedMovie.getTitle()).contains("!@#$%^&*()");
        assertThat(savedMovie.getStudios()).contains("àáâãäåæçèéêë");
        assertThat(savedMovie.getProducers()).contains("\"Double\" and 'Single'");
    }

    @Test
    void shouldHandleEdgeCaseYearValues() {
        final var oldMovie = Movie.builder()
                .year(1900)
                .title("Very Old Movie")
                .producers("Old Producer")
                .winner(false)
                .build();
        final var futureMovie = Movie.builder()
                .year(2100)
                .title("Future Movie")
                .producers("Future Producer")
                .winner(false)
                .build();

        final var savedOldMovie = movieRepository.save(oldMovie);
        final var savedFutureMovie = movieRepository.save(futureMovie);

        assertThat(savedOldMovie.getYear()).isEqualTo(1900);
        assertThat(savedFutureMovie.getYear()).isEqualTo(2100);
    }

    @Test
    void shouldHandleBooleanWinnerField() {
        final var winner = Movie.builder()
                .year(1980)
                .title("Winner")
                .producers("Producer")
                .winner(true)
                .build();
        final var nonWinner = Movie.builder()
                .year(1980)
                .title("Non Winner")
                .producers("Producer")
                .winner(false)
                .build();

        final var savedWinner = movieRepository.save(winner);
        final var savedNonWinner = movieRepository.save(nonWinner);

        assertThat(savedWinner.getWinner()).isTrue();
        assertThat(savedNonWinner.getWinner()).isFalse();
    }

    @Test
    void shouldPerformEfficientQueriesWithIndexes() {
        for (int year = 1980; year <= 1990; year++) {
            for (int i = 0; i < 10; i++) {
                final var movie = Movie.builder()
                        .year(year)
                        .title("Movie " + year + "-" + i)
                        .producers("Producer " + i)
                        .winner(i % 3 == 0)
                        .build();
                movieRepository.save(movie);
            }
        }

        final var startTime = System.currentTimeMillis();
        final var winningMovies = movieRepository.findAllWinningMoviesOrderedByYear();
        final var endTime = System.currentTimeMillis();
        final var queryTime = endTime - startTime;

        assertThat(winningMovies).hasSizeGreaterThan(30);
        assertThat(queryTime).isLessThan(1000);
    }

    @Test
    void shouldHandleLargeDatasets() {
        final var movies = new ArrayList<Movie>();
        for (int i = 0; i < 1000; i++) {
            movies.add(Movie.builder()
                    .year(1980 + (i % 40))
                    .title("Bulk Movie " + i)
                    .producers("Bulk Producer " + i)
                    .winner(i % 5 == 0)
                    .build());
        }

        final var startTime = System.currentTimeMillis();
        movieRepository.saveAll(movies);
        final var endTime = System.currentTimeMillis();
        final var saveTime = endTime - startTime;

        assertThat(movieRepository.count()).isEqualTo(1000);
        assertThat(saveTime).isLessThan(5000);
    }
}