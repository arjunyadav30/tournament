package com.example.Tournament.repository;

import com.example.Tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    List<Tournament> findByStatus(String status);
}
