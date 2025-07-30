package com.emikaelsilveira.goldenraspberry.repository;

import com.emikaelsilveira.goldenraspberry.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("SELECT m FROM Movie m WHERE m.winner = true ORDER BY m.year ASC")
    List<Movie> findAllWinningMoviesOrderedByYear();
}
