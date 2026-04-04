package com.example.Tournament.repository;

import com.example.Tournament.entity.Innings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InningsRepository extends JpaRepository<Innings, Integer> {
    
    // 🔥 Find innings by match
    List<Innings> findByMatch(com.example.Tournament.entity.Match match);
    
    // 🔥 Find innings by match ID
    List<Innings> findByMatchMatchId(int matchId);

    void deleteByMatchMatchId(int matchId);
}