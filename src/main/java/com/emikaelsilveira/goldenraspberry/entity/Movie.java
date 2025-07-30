package com.emikaelsilveira.goldenraspberry.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "movies", indexes = {
        @Index(name = "idx_movie_year", columnList = "movie_year"),
        @Index(name = "idx_movie_winner", columnList = "winner"),
        @Index(name = "idx_movie_year_winner", columnList = "movie_year, winner")
})
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_year", nullable = false)
    @NotNull(message = "Year cannot be null")
    private Integer year;

    @Column(name = "title", nullable = false, length = 500)
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @Column(name = "studios", length = 1000)
    private String studios;

    @Column(name = "producers", nullable = false, length = 1000)
    @NotBlank(message = "Producers cannot be blank")
    private String producers;

    @Column(name = "winner", nullable = false)
    private Boolean winner;
}
