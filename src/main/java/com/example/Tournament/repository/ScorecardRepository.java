package com.example.Tournament.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import com.example.Tournament.entity.Scorecard;

import java.util.List;
import java.util.Optional;

public interface ScorecardRepository extends JpaRepository<Scorecard, Integer> {

    // 🔥 OLD (keep for compatibility)
    Optional<Scorecard> findByMatch_MatchIdAndPlayer_PlayerId(int matchId, int playerId);

    // 🔥 NEW (MOST IMPORTANT)
    Optional<Scorecard> findByMatch_MatchIdAndPlayer_PlayerIdAndInnings(
            int matchId, int playerId, int innings
    );

    // 🔥 MATCH + INNINGS FILTER
    List<Scorecard> findByMatch_MatchIdAndInnings(int matchId, int innings);

    // 🔥 ALL (existing)
    List<Scorecard> findByMatch_MatchId(int matchId);

    // 🔥 Tournament stats lookup helper
    List<Scorecard> findByMatch_MatchIdIn(List<Integer> matchIds);

    List<Scorecard> findByPlayer_PlayerId(int playerId);

    // 🔥 DELETE
    @Modifying
    @Transactional
    @Query("DELETE FROM Scorecard s WHERE s.player.playerId = :playerId")
    void deleteByPlayerId(int playerId);

    void deleteByMatch_MatchId(int matchId);

    // 🔥 NEW DELETE BY INNINGS
    void deleteByMatch_MatchIdAndInnings(int matchId, int innings);
}