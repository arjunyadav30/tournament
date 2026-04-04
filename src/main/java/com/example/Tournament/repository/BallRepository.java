package com.example.Tournament.repository;

import com.example.Tournament.entity.Ball;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BallRepository extends JpaRepository<Ball, Integer> {
    
    // 🔥 Find all balls of a match
    List<Ball> findByMatch(com.example.Tournament.entity.Match match);
    
    // 🔥 Find balls by match ID
    List<Ball> findByMatchMatchId(int matchId);

    Optional<Ball> findTopByMatchMatchIdOrderByIdDesc(int matchId);

    void deleteByMatchMatchId(int matchId);
}