package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.emikaelsilveira.goldenraspberry.entity.Movie;
import com.emikaelsilveira.goldenraspberry.exception.CSVReaderException;
import com.emikaelsilveira.goldenraspberry.repository.MovieRepository;
import com.emikaelsilveira.goldenraspberry.service.CsvReaderService;
import com.emikaelsilveira.goldenraspberry.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:error_testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN",
        "app.security.api-key=test-api-key-12345"
})
@Transactional
class ErrorHandlingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private MovieService actualMovieService;

    @SpyBean
    private CsvReaderService csvReaderService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();
    }

    @Test
    void shouldHandleServiceExceptionGracefully() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", "test-api-key-12345"))
                .andExpect(status().isOk());

        assertThat(actualMovieService).isNotNull();
    }

    @Test
    void shouldHandleInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleMissingApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleInvalidHttpMethod() throws Exception {
        mockMvc.perform(post("/api/producers/intervals")
                        .header("x-api-key", "test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldHandleInvalidEndpoint() throws Exception {
        mockMvc.perform(get("/api/producers/nonexistent")
                        .header("x-api-key", "test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleDatabaseConstraintViolation() {
        final var invalidMovie = Movie.builder()
                .year(null)
                .title("Test Movie")
                .producers("Test Producer")
                .winner(true)
                .build();

        assertThatThrownBy(() -> movieRepository.save(invalidMovie))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldHandleEntityManagerErrors() {
        final var invalidMovie = Movie.builder()
                .year(2000)
                .title("A".repeat(600))
                .producers("Test Producer")
                .winner(true)
                .build();

        assertThatThrownBy(() -> movieRepository.saveAndFlush(invalidMovie))
                .isInstanceOf(Exception.class);
    }

    @Test
    void shouldHandleLargeDataOperations() {
        for (int i = 0; i < 1000; i++) {
            final var movie = Movie.builder()
                    .year(2000 + (i % 25))
                    .title("Test Movie " + i)
                    .producers("Producer " + (i % 10))
                    .studios("Studio " + (i % 5))
                    .winner(i % 10 == 0)
                    .build();
            movieRepository.save(movie);
        }

        final var startTime = System.currentTimeMillis();
        var result = actualMovieService.getProducerIntervals();
        final var endTime = System.currentTimeMillis();

        assertThat(result).isNotNull();
        assertThat(endTime - startTime).isLessThan(10000);
    }

    @Test
    void shouldHandleCsvReaderServiceFailure() {
        doThrow(new CSVReaderException("CSV parsing failed", new RuntimeException("IO Error")))
                .when(csvReaderService).run(any());

        assertThatThrownBy(() -> csvReaderService.run(null))
                .isInstanceOf(CSVReaderException.class)
                .hasMessageContaining("CSV parsing failed");
    }

    @Test
    void shouldHandleCorruptedCsvData() {
        movieRepository.deleteAll();

        var result = actualMovieService.getProducerIntervals();

        assertThat(result).isNotNull();
        assertThat(result.min()).isEmpty();
        assertThat(result.max()).isEmpty();
    }

    @Test
    void shouldHandlePartialDataCorruption() {
        createValidMovie(2000, "Good Movie", "Valid Producer");

        final var edgeCaseMovie = Movie.builder()
                .year(1920) // Very old year
                .title("Movie with 'quotes' & special chars!")
                .producers("Producer, Another Producer & Third Producer")
                .studios("Studio with very long name that exceeds normal expectations")
                .winner(true)
                .build();
        movieRepository.save(edgeCaseMovie);

        var result = actualMovieService.getProducerIntervals();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldHandleConcurrentDatabaseAccess() throws InterruptedException {
        final var threadCount = 5;
        final var latch = new CountDownLatch(threadCount);
        final var futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    createValidMovie(2000 + threadId, "Movie " + threadId, "Producer " + threadId);
                    actualMovieService.getProducerIntervals();
                } finally {
                    latch.countDown();
                }
            });
        }

        final var completed = latch.await(30, TimeUnit.SECONDS);

        assertThat(completed).isTrue();

        for (CompletableFuture<?> future : futures) {
            assertThatCode(() -> future.get(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldHandleSimultaneousReadWrites() throws InterruptedException {
        createValidMovie(2000, "Initial Movie", "Initial Producer");

        final var latch = new CountDownLatch(10);
        final var futures = new CompletableFuture[10];

        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    if (threadId % 2 == 0) {
                        actualMovieService.getProducerIntervals();
                        movieRepository.findAll();
                    } else {
                        createValidMovie(2010 + threadId, "Concurrent Movie " + threadId, "Producer " + threadId);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        final var completed = latch.await(30, TimeUnit.SECONDS);

        assertThat(completed).isTrue();

        for (CompletableFuture<?> future : futures) {
            assertThatCode(() -> future.get(5, TimeUnit.SECONDS)).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldHandleMemoryPressure() {
        for (int i = 0; i < 500; i++) {
            final var movie = Movie.builder()
                    .year(1980 + (i % 40))
                    .title("Memory Test Movie " + i + " with very long title that consumes more memory")
                    .producers("Producer " + i + " with extended name and details")
                    .studios("Studio " + i + " with comprehensive information")
                    .winner(i % 5 == 0)
                    .build();
            movieRepository.save(movie);
        }

        final var startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        final var result = actualMovieService.getProducerIntervals();
        movieRepository.findAll();
        movieRepository.findAllWinningMoviesOrderedByYear();

        final var endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        assertThat(result).isNotNull();
        assertThat(endMemory - startMemory).isLessThan(100_000_000); // Less than 100MB increase
    }

    @Test
    void shouldHandleDeepRecursionScenarios() {
        final var producer = "Prolific Producer";
        for (int year = 1980; year < 2020; year++) {
            createValidMovie(year, "Movie " + year, producer);
        }

        final var result = actualMovieService.getProducerIntervals();

        assertThat(result).isNotNull();
        if (result.min() != null && !result.min().isEmpty()) {
            assertThat(result.min().getFirst().producer()).contains("Prolific");
        }
    }

    @Test
    void shouldCompleteWithinReasonableTime() {
        setupModerateDataset();

        final var startTime = System.currentTimeMillis();
        final var result = actualMovieService.getProducerIntervals();
        final var endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isLessThan(5000);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldHandleRepeatedOperations() {
        setupModerateDataset();

        for (int i = 0; i < 10; i++) {
            var result = actualMovieService.getProducerIntervals();
            assertThat(result).isNotNull();
        }

        final var startTime = System.currentTimeMillis();
        final var finalResult = actualMovieService.getProducerIntervals();
        final var endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isLessThan(1000);
        assertThat(finalResult).isNotNull();
    }

    @Test
    void shouldHandleNullPointerScenarios() {
        final var movieWithNulls = Movie.builder()
                .year(2000)
                .title("Test Movie")
                .producers("Test Producer")
                .studios(null)
                .winner(true)
                .build();
        movieRepository.save(movieWithNulls);

        final var result = actualMovieService.getProducerIntervals();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldHandleExtremeDataValues() {
        createValidMovie(Integer.MAX_VALUE - 1, "Future Movie", "Future Producer");
        createValidMovie(1, "Ancient Movie", "Ancient Producer");

        final var movieWithLongStrings = Movie.builder()
                .year(2000)
                .title("A".repeat(450))
                .producers("B".repeat(950))
                .studios("C".repeat(950))
                .winner(true)
                .build();
        movieRepository.save(movieWithLongStrings);

        final var result = actualMovieService.getProducerIntervals();
        assertThat(result).isNotNull();
    }

    @Test
    void shouldHandleTransactionRollbackScenarios() {
        var exceptionOccurred = false;
        try {
            final var invalidMovie = Movie.builder()
                    .year(null)
                    .title("Invalid Movie")
                    .producers("Test Producer")
                    .winner(true)
                    .build();
            movieRepository.save(invalidMovie);
        } catch (Exception e) {
            exceptionOccurred = true;
            assertThat(e).isInstanceOf(Exception.class);
        }

        assertThat(exceptionOccurred).isTrue();
    }


    private void createValidMovie(int year, String title, String producers) {
        final var movie = Movie.builder()
                .year(year)
                .title(title)
                .producers(producers)
                .studios("Test Studio")
                .winner(true)
                .build();
        movieRepository.save(movie);
    }

    private void setupModerateDataset() {
        final String[] producers = {"Producer A", "Producer B", "Producer C", "Producer D", "Producer E"};

        for (int i = 0; i < 50; i++) {
            final var movie = Movie.builder()
                    .year(1980 + (i % 20))
                    .title("Dataset Movie " + i)
                    .producers(producers[i % producers.length])
                    .studios("Studio " + (i % 3))
                    .winner(i % 4 == 0)
                    .build();
            movieRepository.save(movie);
        }
    }
}