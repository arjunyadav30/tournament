package com.example.Tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.example.Tournament.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Integer> {

    // 🔥 Status ke hisab se matches fetch karega
    List<Match> findByStatus(String status);

    // 🔥 Tournament specific matches
    List<Match> findByTournament_TournamentId(int tournamentId);

}